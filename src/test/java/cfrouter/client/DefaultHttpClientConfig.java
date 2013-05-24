package cfrouter.client;

import org.apache.http.impl.client.DefaultHttpClient;

/**
* Default config, perform no other additional steps
*/
class DefaultHttpClientConfig implements HttpClientConfig {
    @Override
    public void applyConfig(DefaultHttpClient httpclient) {
        //NoOp
    }
}
