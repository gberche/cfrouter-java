package cfrouter.client;

import cfrouter.client.impl.PojoMapper;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Describes a route to be programmed by the HTTP Router
 */
@JsonIgnoreProperties(ignoreUnknown = true) //ignore tags not yet modeled
public class Route {

    private String host;
    private int port;
    private String [] uris;
    //tags ommited for now
    //private Id ommited for now

    /**
     *
     * @param host the host that will be routed HTTP request tos
     * @param port the port that will be routed HTTP request tos
     * @param uris the uris (really DNS hostnames or virtual host) that will be routed to the host/port e.g. "app1.myrouter.mydomain.com"
     */
    public Route(String host, int port, String[] uris) {
        this.host = host;
        this.port = port;
        this.uris = uris;
    }

    public Route() {
    }

    public Route(Route route) {
        this.host = route.getHost();
        this.port = route.getPort();
        int length = route.getUris().length;
        this.uris =  new String [length];
        System.arraycopy(route.getUris(), 0, this.uris, 0, length);

    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String[] getUris() {
        return uris;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setUris(String[] uris) {
        this.uris = uris;
    }

    public String toJson() {
        return PojoMapper.routeToJson(this);
    }

    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
