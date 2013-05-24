package cfrouter.client;

import org.apache.http.impl.client.DefaultHttpClient;

/**
* Supports injecting additional config steps to HttpClient without duplicating whole test
*/
interface HttpClientConfig {
    void applyConfig(DefaultHttpClient httpclient);
}
