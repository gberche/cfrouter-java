# CloudFoundry gorouter Java Client

<!--
[![Build Status](https://secure.travis-ci.org/gberche/gorouter-javaclient.png?branch=master)](http://travis-ci.org/gberche/gorouter-javaclient)
-->


A Java client for the [CloudFoudry gorouter](https://github.com/cloudfoundry/gorouter) which allows to set/get/reset
the routes, look up the metrics. It also includes a test suite that validates some of the gorouter behavior.

Aug 2013 update: this repo has moved to https://github.com/Orange-OpenSource/cfrouter-java

## Getting started

To use the basic client in your project, add the following to your Maven pom.xml:
```xml
<dependency>
    <groupId>com.github.gberche</groupId>
    <artifactId>gorouter-javaclient</artifactId>
    <version>0.1-SNAPSHOT</version>
</dependency>
```

## Running the test suite

Prerequisites:
*   a DNS wild card entry pointing to the gorouter IP address (e.g A *.myrouter.mydomain.com 192.168.0.1)
*   a Natsd daemon running and reacheable from the Junit JVM (10.114.6.104:4222 in the example below)
*   A gorouter instance configured (currently without authentication), running and reacheable from the junit jvm
    by default on port > 9000

Export environment variables or define them as java system properties:

```sh
mvn -DNATS_URL=nats://10.114.6.104:4222 -DROUTER_DNS_WILDCARD=myrouter.mydomain.com -DROUTER_PORT=8081 test
```

## Using the client

Instanciate the router

```java
  Nats nats = new NatsConnector().addHost(NATS_URL).connect();
  Router router = new RouterImpl(nats);
```

Register the route
```java
       //Single host, multiple end-points
        Route r1 = new Route("hostA", 80, new String[]{"myappendpoint1.myrouter.mydomain.com", "myappendpoint2.myrouter.mydomain.com"});

        //Single endpoint, multiple host/ports cluster
        Route r2 = new Route("host1.cluster", 80, new String[]{"myLoadBalancedApp.myrouter.mydomain.com"});
        Route r3 = new Route("host2.cluster", 80, new String[]{"myLoadBalancedApp.myrouter.mydomain.com"});

        router.addRoutes(asList(r1, r2, r3));
```

Register a callback to provide routes to new router instances or in the future renew the routes when they expire.
```java
        RouterStartHandler handler = new RouterStartHandler() {
            @Override
            public List<Route> fetchAllActiveRoutes() {
                //Fetch this of active routes to renew, e.g. from a db
                List<Route> routes = ....
                return routes;
            }
        }
        router.registerRouterStartMsgHandler(handler);

```

Fetch list of active routes and metrics from the router (support for cluster of router instances planned):

```java
        List<Route> activeRoutes = router.getActiveRoutes();

        RouterMetrics metrics = router.getRouterMetrics();
```

In case you choose to increase the router expiration time (2 mins by default), you may need to unregister some routes

```java
        List<Route> activeRoutes = router.getActiveRoutes();
        //select some routes to unregister
        router.removeRoutes(activeRoutes);
```


## License

(The Apache Software License 2.0) - http://www.apache.org/licenses/
Copyright (c) 2013 Guillaume Berche

## Bugs, improvements, suggestions

Any feedback is welcome. Please report bugs and suggest/contribute improvements through github issues and pull requests.

See identified limitations and [planned improvements](/TODO.md)



