package cfrouter.client;

import cfrouter.client.impl.RouterImpl;
import nats.client.Nats;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.apache.commons.lang.builder.EqualsBuilder.reflectionEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 *
 */
public class RouterImplTest {

    private RouterImpl router;

    @Before
    public void setUp() {
        Nats nats = mock(Nats.class);
        router = new RouterImpl(nats);
    }

    @Test
    public void list_of_current_routes_parse_into_route_bean() throws IOException {
        String json = TestResourceLoader.loadLocalResource("routesResponse.json");

        List<Route> parsedRoutes = router.parseRouteDumpIntoDtos(json);

        List<Route> expectedRoutes = new ArrayList<Route>();
        expectedRoutes.add(new Route("host2", 80, new String[]{"route2.myrouter.mydomain.com"}));
        expectedRoutes.add(new Route("host1", 80, new String[]{"route1.myrouter.mydomain.com"}));
        expectedRoutes.add(new Route("10.114.6.195", 8080, new String[]{"route1.myrouter.mydomain.com"}));

        assertTrue(reflectionEquals(expectedRoutes, parsedRoutes));
    }


}
