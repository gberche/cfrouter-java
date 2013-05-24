package cfrouter.client;

import cfrouter.client.impl.PojoMapper;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 *
 */
public class RouterMetricsTest {

    @Test
    public void parse_from_json() throws IOException {
        String json = TestResourceLoader.loadLocalResource("varzResponse.json");
        RouterMetrics metrics = PojoMapper.fromJson(json, RouterMetrics.class);
        assertEquals(520, metrics.getRequests());
    }

}
