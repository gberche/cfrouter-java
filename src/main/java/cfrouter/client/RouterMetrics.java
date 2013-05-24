package cfrouter.client;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import java.util.Map;

/**
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true) //ignore tags not yet modeled
public class RouterMetrics {

    /**
     * "latency": {
     "50": 0.0182665,
     "75": 0.05908225,
     "90": 0.3971016,
     "95": 1.2799558499999972,
     "99": 21.00002549,
     "samples": 1,
     "value": 5e-07
     },
     */
    Map<String, Float> latency;


    private int requests; //"requests": 520,
    private float requests_per_sec; // "requests_per_sec": 0.003032504913207889,
    private int responses_2xx; //        "responses_2xx": 405,
    private int responses_3xx; //        "responses_3xx": 37,
    private int responses_4xx; //        "responses_4xx": 0,
    private int responses_5xx; //        "responses_5xx": 0,
    private int responses_xxx; //        "responses_xxx": 78,

    private String start; //"start": "2013-04-16 12:47:37 +0200",

    private String uptime; //"uptime": "1d:5h:27m:0s",
    private int urls; //        "urls": 4,

    public RouterMetrics() {
    }

    public Map<String, Float> getLatency() {
        return latency;
    }

    public void setLatency(Map<String, Float> latency) {
        this.latency = latency;
    }

    public int getRequests() {
        return requests;
    }

    public void setRequests(int requests) {
        this.requests = requests;
    }

    public float getRequests_per_sec() {
        return requests_per_sec;
    }

    public void setRequests_per_sec(float requests_per_sec) {
        this.requests_per_sec = requests_per_sec;
    }

    public int getResponses_2xx() {
        return responses_2xx;
    }

    public void setResponses_2xx(int responses_2xx) {
        this.responses_2xx = responses_2xx;
    }

    public int getResponses_3xx() {
        return responses_3xx;
    }

    public void setResponses_3xx(int responses_3xx) {
        this.responses_3xx = responses_3xx;
    }

    public int getResponses_4xx() {
        return responses_4xx;
    }

    public void setResponses_4xx(int responses_4xx) {
        this.responses_4xx = responses_4xx;
    }

    public int getResponses_5xx() {
        return responses_5xx;
    }

    public void setResponses_5xx(int responses_5xx) {
        this.responses_5xx = responses_5xx;
    }

    public int getResponses_xxx() {
        return responses_xxx;
    }

    public void setResponses_xxx(int responses_xxx) {
        this.responses_xxx = responses_xxx;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getUptime() {
        return uptime;
    }

    public void setUptime(String uptime) {
        this.uptime = uptime;
    }

    public int getUrls() {
        return urls;
    }

    public void setUrls(int urls) {
        this.urls = urls;
    }
}
