package cfrouter.client;

import cfrouter.client.impl.RecordingMessageHandler;
import cfrouter.client.impl.RouterImpl;
import nats.client.Message;
import nats.client.Nats;
import nats.client.NatsConnector;
import nats.client.Subscription;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.params.ConnRoutePNames;
import org.apache.http.conn.routing.HttpRoute;
import org.apache.http.conn.routing.HttpRoutePlanner;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.HttpContext;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.fest.assertions.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 *
 */
public class RouterTest {

    private static final Logger logger = LoggerFactory.getLogger(RouterTest.class);

    private static final String ROUTER_HOST = getEnvOrSystemProp("ROUTER_DNS_WILDCARD");
    private final int ROUTER_PORT = new Integer(getEnvOrSystemProp("ROUTER_PORT"));
    private static final String NATS_URL = getEnvOrSystemProp("NATS_URL");


    Router router;

    private List <Route> routes;
    private BlockingDeque<Message> registerMsgs = new LinkedBlockingDeque<Message>();
    private BlockingDeque<Message> unregisterMsgs = new LinkedBlockingDeque<Message>();

    private BlockingDeque<Message> announceReplyMsgs = new LinkedBlockingDeque<Message>();
    private Subscription registerSubscription;
    private Subscription unregisterSubscription;
    private Nats nats;

    public void registerToNatsMsgs() {
        registerSubscription = registerRecordingHandler("router.register", registerMsgs);
        unregisterSubscription = registerRecordingHandler("router.unregister", unregisterMsgs);

    }

    private Subscription registerRecordingHandler(String subject, BlockingDeque<Message> registerMsgs1) {
        return nats.subscribe(subject, new RecordingMessageHandler(subject, registerMsgs1));
    }

    @Before
    public void setUp() {
        this.nats = new NatsConnector().addHost(NATS_URL).connect();
        router = new RouterImpl(this.nats);
        registerToNatsMsgs();

        Route r1 = new Route("host1", 80, new String[]{System.currentTimeMillis() + "-ut-random."+ROUTER_HOST});
        Route r2 = new Route("host2", 80, r1.getUris());
        Route r3 = new Route("host2", 8080, new String[]{"xaas-engine-fut." +ROUTER_HOST});
        routes = asList(r1, r2, r3);
    }

    @After
    public void tearDown() {
        try {
            router.removeRoutes(routes); //Clear previously registered routes if any
        } catch (Exception e) {
            e.printStackTrace();
        }

        //Clean up nats to avoid leaking subscription on natsd
        registerSubscription.close();
        unregisterSubscription.close();

        //clear queues among test exec to keep each test method independent
        registerMsgs.clear();
        unregisterMsgs.clear();
        announceReplyMsgs.clear();

        nats.close();
    }

    @Test
    public void it_listens_to_router_starts_and_publishes_all_active_routes() throws InterruptedException {
        RouterStartHandler handler = mock(RouterStartHandler.class);
        when(handler.fetchAllActiveRoutes()).thenReturn(routes);

        //given
        router.registerRouterStartMsgHandler(handler);

        //when
        nats.publish("router.start");

        //then
        waitForRegisterMsgNatsSending(routes);
        verify(handler).fetchAllActiveRoutes(); //verify the handler was invoked
    }

    @Test
    public void it_registers_new_routes() throws InterruptedException, IOException {

        //when
        router.addRoutes(routes);

        //then
        for (Route r : routes) {
            Message message = registerMsgs.pollFirst(2, TimeUnit.SECONDS);     //expect to receive a register msg
            assertNotNull("expected register msg to be received", message);
            assertEquals("register msg", r.toJson(), message.getBody());
        }
        List<Route> activeRoutes = router.getActiveRoutes();
        assertTrue("expected to find updated routes ", activeRoutes.containsAll(routes));
    }

    @Test
    public void it_lists_active_routes() throws IOException, InterruptedException {
        List<Route> activeRoutes = router.getActiveRoutes();

        Assertions.assertThat(activeRoutes).isNotEmpty();
    }

    @Test
    public void it_may_override_existing_routes() throws InterruptedException, IOException {
        ArrayList<Route> updatedRoutes = new ArrayList<Route>();
        try {
            //given
            router.addRoutes(routes);

            waitForRegisterMsgNatsSending(routes);

            for (Route route : routes) {
                Route updatedRoute = new Route(route);
                updatedRoute.setPort(updatedRoute.getPort() + 1);
                updatedRoutes.add(updatedRoute);
            }

            //when
            router.replaceRoutes(updatedRoutes);
            waitForRegisterMsgNatsSending(updatedRoutes);

            //then
            List<Route> activeRoutes = router.getActiveRoutes();
            assertTrue("expected to find updated routes ", activeRoutes.containsAll(updatedRoutes));
            for (Route originalRoute : routes) {
                assertFalse("expected to not find original routes: " + originalRoute, activeRoutes.contains(originalRoute));
            }
        } finally {
            router.removeRoutes(updatedRoutes);
        }

    }

    @Test
    public void it_unregister_routes() throws InterruptedException, IOException {
        //given
        it_registers_new_routes();

        //when
        router.removeRoutes(routes);

        //then
        assertUnregisterMsgSent(routes);
        List<Route> activeRoutes = router.getActiveRoutes();
        for (Route route : routes) {
            assertFalse("expected to have routes unregistered", activeRoutes.contains(route));
        }
    }

    /**
     * Assume the junit JVM and the cfrouter can reach each other through TCP
     * @throws Exception
     */
    @Test
    public void router_routes_httprequests_according_to_registered_routes() throws Exception {
        //start up a local HTTP server on two port A and B, serving respectively contentA and contentB
        final AtomicInteger receivedResourceACalls = new AtomicInteger(0);
        int portServerA = getNextAvailablePort(9000);
        Server serverA = startJettyServer(portServerA, new CountingServletHandler("Resource A", receivedResourceACalls));

        final AtomicInteger receivedResourceBCalls = new AtomicInteger(0);
        int portServerB = getNextAvailablePort(portServerA + 1);
        Server serverB = startJettyServer(portServerB, new CountingServletHandler("Resource B", receivedResourceBCalls));

        String localHostIp = InetAddress.getLocalHost().getHostAddress();
        logger.info("Started on ports: " + portServerA + " and " + portServerB + " from ip:" + localHostIp);

        //ensure jetty properly responding prior to asserting router responses
        waitUntilUrlIsResponding("http://" + localHostIp + ":" + portServerA);
        waitUntilUrlIsResponding("http://" + localHostIp + ":" + portServerB);

        routes = new ArrayList<Route>();
        try {
            //register R1 to point to host:A and R2 (route2...) to host:B
            String virtualHostA = "route1."+ROUTER_HOST;
            Route r1 = new Route(localHostIp, portServerA, new String[]{virtualHostA});
            String virtualHostB = "route2."+ROUTER_HOST;
            Route r2 = new Route(localHostIp, portServerB, new String[]{virtualHostB});
            routes.add(r1);
            routes.add(r2);


            router.addRoutes(routes);


            //send a http request to route1 and expect contentA to be returned,
            //send a http request to route2 and expect contentB to be returned,

            resetCounters(receivedResourceACalls, receivedResourceBCalls);
            String contentA = fetchRoutedContentAsString(virtualHostA);

            assertEquals(1, receivedResourceACalls.get());
            assertEquals(0, receivedResourceBCalls.get());

            resetCounters(receivedResourceACalls, receivedResourceBCalls);
            String contentB = fetchRoutedContentAsString(virtualHostB);

            assertEquals(0, receivedResourceACalls.get());
            assertEquals(1, receivedResourceBCalls.get());

        } finally {
            router.removeRoutes(routes);
            unregisterMsgs.pollFirst(2, TimeUnit.SECONDS);//wait for the unregister message to propagate
            serverA.stop();
            serverB.stop();
        }
    }

    @Test
    public void router_provides_host_header_of_routed_uri_to_origin_server_when_directly_contacted() throws Exception {
        assert_router_provides_host_header_of_routed_uri_to_origin_server(new DefaultHttpClientConfig());
    }

    @Test
    public void router_provides_host_header_of_routed_uri_to_origin_server_when_reached_as_proxy() throws Exception {
        final String routerProxyHost = "proxy." + ROUTER_HOST;

        //The default way to assign the proxy
        HttpClientConfig defaultProxyConfig = new HttpClientConfig() {
            @Override
            public void applyConfig(DefaultHttpClient httpclient) {
                HttpHost proxy = new HttpHost(routerProxyHost, ROUTER_PORT);
                httpclient.getParams().setParameter(ConnRoutePNames.DEFAULT_PROXY, proxy);
            }
        };
        assert_router_provides_host_header_of_routed_uri_to_origin_server(defaultProxyConfig);
    }

   @Test
   @Ignore("http client does not yet support proxy chains that would enable clean sneaking into the http traffic")
    public void router_provides_host_header_of_routed_uri_to_origin_server_when_reached_as_proxy_spied_in_between() throws Exception {
        final String routerProxyHost = "proxy." + ROUTER_HOST;

        //Multiple ways to configure HttpClient to use a proxy: http://hc.apache.org/httpcomponents-client-ga/tutorial/html/connmgmt.html#d5e571

        //The explicit one, could have been useful to have a "spy proxy" in the middle.
        // However, multi-hop proxy chains are not yet supported in http-client, see http://httpcomponents.10934.n7.nabble.com/Implementing-Proxy-Chaining-in-Apache-HTTP-td11963.html
        // Using wireshark instead if I need to troubleshoot
        HttpClientConfig explicitHttpClientRouteConfig = new HttpClientConfig(){
            @Override
            public void applyConfig(DefaultHttpClient httpclient) {
                httpclient.setRoutePlanner(new HttpRoutePlanner() {

                    @Override
                    public HttpRoute determineRoute(HttpHost target, HttpRequest request, HttpContext context) throws HttpException {
                        HttpHost[] proxies = {
                                new HttpHost("localhost", 9005), //a spy proxy such as charles debugging proxy
                                new HttpHost(routerProxyHost, ROUTER_PORT),
                        };
                        boolean forceConnectMethodEvenForRegularHttp = true;
                        return new HttpRoute(target, null, proxies, forceConnectMethodEvenForRegularHttp, null, null);
                    }

                });
            }
        };
        assert_router_provides_host_header_of_routed_uri_to_origin_server(explicitHttpClientRouteConfig);
    }


    private void assert_router_provides_host_header_of_routed_uri_to_origin_server(HttpClientConfig httpClientConfig) throws Exception {
        int port = getNextAvailablePort(9000);
        Server server = startJettyServer(port, new EchosHostServletHandler());

        String localHostIp = InetAddress.getLocalHost().getHostAddress();

        //ensure jetty properly responding prior to asserting router responses
        waitUntilUrlIsResponding("http://" + localHostIp + ":" + port);

        routes = new ArrayList<Route>();
        try {
            String virtualHost = "route1."+ROUTER_HOST;
            Route r1 = new Route(localHostIp, port, new String[]{virtualHost});
            routes.add(r1);

            //when
            router.replaceRoutes(routes);

            //then
            String content = fetchRoutedContentAsString(virtualHost, httpClientConfig);
            assertEquals("Host=" + virtualHost + ":8081", content.trim());
        } finally {
            router.removeRoutes(routes);
            unregisterMsgs.pollFirst(2, TimeUnit.SECONDS);//wait for the unregister message to propagate
            server.stop();
        }
    }


    private String fetchRoutedContentAsString(String virtualHost) throws IOException {
        return fetchRoutedContentAsString(virtualHost, new DefaultHttpClientConfig());
    }

    private String fetchRoutedContentAsString(String virtualHost, HttpClientConfig httpClientConfig) throws IOException {
        String contentA;

        DefaultHttpClient httpclient = new DefaultHttpClient();
        httpClientConfig.applyConfig(httpclient);
        try {
            HttpGet httpget = new HttpGet("http://" + virtualHost + ":8081/path");
            contentA = httpclient.execute(httpget, new BasicResponseHandler());
        } finally {
            httpclient.getConnectionManager().shutdown();
        }
        return contentA;
    }

    private void resetCounters(AtomicInteger receivedResourceACalls, AtomicInteger receivedResourceBCalls) {
        receivedResourceACalls.set(0);
        receivedResourceBCalls.set(0);
    }

    private void waitUntilUrlIsResponding(String url) {

        int retry = 0;
        do {
            retry++;
            try {
                Object contentA = URI.create(url).toURL().getContent();
                break;
            } catch (IOException e) {
                //throws file not found when receiving 404
                logger.info("local server not yet ready for url: " + url + " Caught:" + e);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e1) {
                    //no op
                }
            }
        } while (retry < 10);
    }

    @Test
    public void client_exposes_router_metrics_as_beans() throws IOException, InterruptedException {
        RouterMetrics metrics = router.getRouterMetrics();
        assertNotNull(metrics);
        assertTrue(metrics.getRequests() > 0);
    }

    @Ignore("run this manually if you need to clear routes of the router under tests")
    @Test
    public void it_can_clear_all_routes() throws IOException, InterruptedException {
        List<Route> activeRoutes = router.getActiveRoutes();
        router.removeRoutes(activeRoutes);
        List<Route> routeAfterClear = router.getActiveRoutes();
        assertTrue(routeAfterClear.isEmpty());
    }



    @Test
    public void client_exposes_currently_active_routes() throws IOException, InterruptedException {
        router.addRoutes(routes);

        //Wait for the request to be received
        waitForRegisterMsgNatsSending(routes);

        List<Route> activeRoutes = router.getActiveRoutes();
        assertTrue("expected to find all registered routes. Expected: \n" + routes + "\nGot: \n" + activeRoutes, activeRoutes.containsAll(routes));
    }

    private void waitForRegisterMsgNatsSending(List<Route> routes1) throws InterruptedException {
        for (Route ignored : routes1) {
            Message message = registerMsgs.pollFirst(2, TimeUnit.SECONDS);     //expect to receive a register msg
            assertNotNull("expected register msg to be received", message);
        }
    }

    private void assertUnregisterMsgSent(List<Route> routeList) throws InterruptedException {
        for (Route r : routeList) {
            Message message = unregisterMsgs.pollFirst(2, TimeUnit.SECONDS);     //expect to receive a register msg
            assertNotNull("expected unregister msg to be received", message);
            assertEquals("unregister msg", r.toJson(), message.getBody());
        }
    }



    private Server startJettyServer(int port, Handler servletHandler) throws Exception {
        Server server = new Server(port);
        server.setHandler(servletHandler);
        server.start();
        return server;
    }

    private int getNextAvailablePort(int initial) {
        int current = initial;
        while (! PortAvailability.available(current)) {
            current ++;
            if (current - initial > 100) {
                throw new RuntimeException("did not find an available port from " + initial + " up to:" + current);
            }
        }
        return current;
    }

    private static class BaseServletHandler extends AbstractHandler {

        @Override
        public void handle(String s, Request baseRequest, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
            response.setContentType("text/html;charset=utf-8");
            response.setStatus(HttpServletResponse.SC_OK);
            baseRequest.setHandled(true);
        }
    }


    private static class CountingServletHandler extends BaseServletHandler {
        private final String resourceName;
        private final AtomicInteger receivedResourceCalls;

        public CountingServletHandler(String resourceName, AtomicInteger receivedResourceCalls) {
            this.resourceName = resourceName;
            this.receivedResourceCalls = receivedResourceCalls;
        }

        @Override
        public void handle(String s, Request baseRequest, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
            super.handle(s, baseRequest, baseRequest, response);
            response.getWriter().println("<h1>" + resourceName + "</h1>");
            receivedResourceCalls.incrementAndGet();
        }
    }

    private static class EchosHostServletHandler extends BaseServletHandler {

        public EchosHostServletHandler() {
            super();
        }

        @Override
        public void handle(String s, Request baseRequest, HttpServletRequest httpServletRequest, HttpServletResponse response) throws IOException, ServletException {
            super.handle(s, baseRequest, httpServletRequest, response);
            response.getWriter().println("Host=" + httpServletRequest.getHeader("Host"));
        }
    }

    private static String getEnvOrSystemProp(String keyName) {
        String value;
        value = System.getenv(keyName);
        if (value == null) {
            value = System.getProperty(keyName);
        }
        if (value == null) {
            throw new IllegalArgumentException("Missing " + keyName + " as a JVM property or environment variable, can't start test without it ");
        }
        return value;
    }

}
