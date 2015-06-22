package io.brooklyn.ambari;

import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;
import com.squareup.okhttp.mockwebserver.MockResponse;
import com.squareup.okhttp.mockwebserver.MockWebServer;

import brooklyn.util.ResourceUtils;
import io.brooklyn.ambari.domain.RecommendationRequest;
import io.brooklyn.ambari.domain.ResourceWrappedResponse;
import io.brooklyn.ambari.rest.AmbariApi;
import io.brooklyn.ambari.rest.AmbariApiHelper;

public class AmbariApiTest {

    @Test
    public void testGetRecommendations() throws Exception {
        String json = new ResourceUtils(this)
                .getResourceAsString("classpath://io/brooklyn/ambari/rest/domain/recommendation-response.json");
        MockWebServer server = new MockWebServer();
        server.enqueue(new MockResponse().setBody(json));
        server.start();
        RecommendationRequest rr = new RecommendationRequest(
                ImmutableList.of("ignored"),
                ImmutableList.of("ignored"));
        AmbariApi api = AmbariApiHelper.newApi(server.getUrl("/").toString(), "user", "password");
        ResourceWrappedResponse<RecommendationResponse> r = api.getRecommendations("HDP", "2.2", rr);

        assertTrue(r.getResources().size() > 0);
        RecommendationResponse rrr = r.getResources().get(0);
        assertNotNull(rrr.getHosts());
        assertNotNull(rrr.getVersion().getStackName());
        System.out.println(r);
    }

}
