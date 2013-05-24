package cfrouter.client.impl;

/**
 *
 */

import cfrouter.client.Route;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import static java.util.Arrays.asList;

public class PojoMapper {

    private static final Logger logger = LoggerFactory.getLogger(PojoMapper.class);


    private static ObjectMapper m = new ObjectMapper();
    private static JsonFactory jf = new JsonFactory();

    public static <T> T fromJson(String jsonAsString, Class<T> pojoClass)
            throws IOException {
        return m.readValue(jsonAsString, pojoClass);
    }

    public static <T> List<T> listFromJson(String jsonAsString, Class<T> pojoClass)
            throws IOException {
        return m.readValue(jsonAsString, m.getTypeFactory().constructCollectionType(List.class, pojoClass));
    }

    public static List<Route> routeListFromJson(String jsonAsString)
            throws IOException {
        try {
            return m.readValue(jsonAsString, new TypeReference<List<Route>>(){});
        } catch (NoSuchMethodError e) {
            //Using an old jackson version (e.g. 1.6.2) which does not yet have include TypeReference
            Route[] routes = m.readValue(jsonAsString, Route[].class);
            return asList(routes);
        }
    }

    public static String objectToJson(Object pojo) throws IOException {
        StringWriter sw = new StringWriter();
        JsonGenerator jg = jf.createJsonGenerator(sw);
        m.writeValue(jg, pojo);
        return sw.toString();
    }

    public static String routeToJson(Route route) {
        try {
            return objectToJson(route);
        } catch (IOException e) {
            logger.warn("unexpectedly failed to produce Json from tested Route object", e);
            return e.toString();
        }
    }
}