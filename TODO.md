- Contribute cfrouter-java to the community
   - change commit emails to avoid spam
   - push on github
   - send email to mailing list
   - setup maven release process
   - consider building from sonatype to be published on maven central or checkout from github on our jenkins + local build

- clean up the exceptions thrown in the interface. Some are parasites.

- add support for router clusters of size >1: return list of metrics, list of routes, ...

- add support for renewing routes that expire: fetch the expiration time and register a timer to invoke RouterStartHandler periodically


- fix erratic medium/integration tests that break on slow gorouter route registration

- rename uri into "virtualhost" in Route object and upwards

- add support for nats authentication. Not clear natsj supports it.

- test the CONNECT method when supported in router
   - using mockwebserver (http://code.google.com/p/mockwebserver/) start and HTTPS server
   - configure httpclient to use CF router as proxy
   - assert the server received the right request and client is returned response
   - debug at the wire level the sending of CONNECT by httpClient. Proxy chaining not yet supported in httpClient http://httpcomponents.10934.n7.nabble.com/Implementing-Proxy-Chaining-in-Apache-HTTP-td11963.html

- test KEEP ALIVE cnx properly routes consecutive requests properly

- clean up parasite trace "nats.NatsException: Received a body for an unknown subscription."

- add meaningful traces

- split between small test (only nats) and medium test (HTTP interactions)

- add asserts in Cf tests: unit tests with nats mock ?

- expose metrics as JMX

- optimize the client for large number of routes:
   - add test for large nb of routes (say 10k)
   - e.g. avoid linear scans for unregister
   - pool the HttpClient and implement spring destroy phase for Router object

- consider using jersey client rather than raw UrlConnection and jackson ?
