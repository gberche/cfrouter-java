package cfrouter.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;


/** copied from https://github.com/TheLadders/embedded-test-jetty */
public final class PortAvailability
{
  private static final Logger LOGGER = LoggerFactory.getLogger(PortAvailability.class);


  /**
   * Checks to see if a specific port is available.
   * 
   * @param port
   *          the port to check for availability
   */
  /*
   * Slightly adapted from Apache MINA (took out min/max port check)
   */
  public static boolean available(int port)
  {
    ServerSocket ss = null;
    DatagramSocket ds = null;
    try
    {
      ss = new ServerSocket(port);
      ds = new DatagramSocket(port);
      ss.setReuseAddress(true);
      ds.setReuseAddress(true);
      return true;
    }
    catch (IOException e)
    {
      LOGGER.debug("Error checking port availability", e);
    }
    finally
    {
      close(ss);
      close(ds);
    }

    return false;
  }


  private static void close(DatagramSocket ds)
  {
    if (ds != null)
    {
      ds.close();
    }
  }


  private static void close(ServerSocket ss)
  {
    if (ss != null)
    {
      try
      {
        ss.close();
      }
      catch (IOException e)
      {
        LOGGER.warn("Error closing socket", e);
      }
    }
  }
}
