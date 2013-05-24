package cfrouter.client.impl;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true) //ignore tags not yet modeled
public class RouterDiscoveryResponse {

    private String host; //to use for REST endpooint
    private String[] credentials;

    public RouterDiscoveryResponse() {
    }

    public String[] getCredentials() {
        return credentials;
    }

    public void setCredentials(String[] credentials) {
        this.credentials = credentials;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }
}
