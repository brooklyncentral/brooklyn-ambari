package io.brooklyn.ambari.rest;

import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.Blueprint;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.BlueprintClusterBinding;

import io.brooklyn.ambari.domain.RecommendationRequest;
import io.brooklyn.ambari.domain.ResourceWrappedResponse;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;

public interface AmbariApi {

    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: text/plain"
    })
    @POST("/api/v1/blueprints/{name}")
    Response createBlueprint(@Path("name") String name, @Body Blueprint blueprint);

    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: text/plain"
    })
    @POST("/api/v1/clusters/{cluster}/hosts/{hostName}/host_components/{component}")
    Response createHostComponent(
            @Path("cluster") String cluster,
            @Path("hostName") String hostName,
            @Path("component") String component);

    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: text/plain"
    })
    @POST("/api/v1/clusters/{cluster}/services/{service}/components/{component}")
    Response createComponent(
            @Path("cluster") String cluster,
            @Path("service") String service,
            @Path("component") String component);

    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: text/plain"
    })
    @POST("/api/v1/clusters/{cluster}/services/{service}")
    Response addServiceToCluster(@Path("cluster") String cluster, @Path("service") String service);

    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: text/plain"
    })
    @POST("/api/v1/clusters/{cluster}/hosts/{host}")
    Response addHostToCluster(@Path("cluster") String cluster, @Path("host") String host);

    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: text/plain"
    })
    @POST("/api/v1/clusters/{name}")
    Response createCluster(@Path("name") String name, @Body BlueprintClusterBinding bluePrintClusterBinding);

    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: text/plain"
    })
    @POST("/api/v1/stacks/{stack}/versions/{version}/recommendations")
    ResourceWrappedResponse<RecommendationResponse> getRecommendations(@Path("stack") String stack, @Path("version") String version, @Body RecommendationRequest body);

}


