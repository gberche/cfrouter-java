package cfrouter.client;

import java.util.List;

/**
* Hook to be requested to provide active routes when a new router starts
*/
public interface RouterStartHandler {

    /**
     * Invokes periodically (by default in the router every 2 mins)
     * @return the list of routes that are still active and should be renewed
     */
    List<Route> fetchAllActiveRoutes();
}
