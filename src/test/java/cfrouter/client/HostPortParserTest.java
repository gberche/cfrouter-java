package cfrouter.client;

import com.google.common.net.HostAndPort;
import org.junit.Assert;
import org.junit.Test;

/**
 * Explore ways to extract host port
 */
public class HostPortParserTest {

    @Test
    public void manually_extract_host_port() {
        String hostPort = "10.149.230.134:8080";
        int colonIndex = hostPort.indexOf(':');
        String host = hostPort.substring(0, colonIndex);
        int port = Integer.parseInt(hostPort.substring(colonIndex+1));
        Assert.assertEquals("10.149.230.134", host);
        Assert.assertEquals(8080, port);
    }

    @Test
    public void guava_extracts_host_port() {
        HostAndPort hp = HostAndPort.fromString("10.149.230.134:8080").withDefaultPort(80);
        Assert.assertEquals("10.149.230.134", hp.getHostText());
        Assert.assertEquals(8080, hp.getPort());
    }
}
