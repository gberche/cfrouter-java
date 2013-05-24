package cfrouter.client;

import cfrouter.client.impl.PojoMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class RouteTest {

    @Test
    public void it_formats_as_json_output_and_parses_back_as_pojo() throws IOException {
        String[] uris = {"my_first_url.vcap.me", "my_second_url.vcap.me"};
        Route r = new Route("127.0.0.1", 4567, uris);

        String json = r.toJson();
        assertEquals("{\"host\":\"127.0.0.1\",\"port\":4567,\"uris\":[\"my_first_url.vcap.me\",\"my_second_url.vcap.me\"]}", json);
        Route unserializedRoute = PojoMapper.fromJson(json, Route.class);
        assertEquals(r, unserializedRoute);
    }

    @Test
    public void it_parses_gorouter_json_specs_without_failing() throws IOException {
        String gorouterCfSpecs = "{\n" +
                "  \"host\": \"127.0.0.1\",\n" +
                "  \"port\": 4567,\n" +
                "  \"uris\": [\n" +
                "    \"my_first_url.vcap.me\",\n" +
                "    \"my_second_url.vcap.me\"\n" +
                "  ],\n" +
                "  \"tags\": {\n" +
                "    \"another_key\": \"another_value\",\n" +
                "    \"some_key\": \"some_value\"\n" +
                "  }\n" +
                "}";
        Route unserializedRoute = PojoMapper.fromJson(gorouterCfSpecs, Route.class);

    }

    @Test
    public void it_parses_a_list_of_routes() throws IOException {
        Route r1 = new Route("host1", 9004, new String[]{"uri1"});
        Route r2 = new Route("host2", 9220, new String[]{"uri2", "uri3"});
        List<Route> expectedRoutes = new ArrayList<>();
        expectedRoutes.add(r1);
        expectedRoutes.add(r2);

        //given
        String json = TestResourceLoader.loadLocalResource("list_of_routes.json");
        //when
        List<Route> routesFetchedFromGenerics = PojoMapper.listFromJson(json, Route.class);
        //then
        assertThat(routesFetchedFromGenerics).isEqualTo(expectedRoutes);

        List<Route> routesFetchedStatically = PojoMapper.routeListFromJson(json);
        assertThat(routesFetchedStatically).isEqualTo(expectedRoutes);
    }

}
