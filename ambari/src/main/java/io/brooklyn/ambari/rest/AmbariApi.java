package io.brooklyn.ambari.rest;

import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.Blueprint;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.BlueprintClusterBinding;

import io.brooklyn.ambari.domain.RecommendationRequest;
import io.brooklyn.ambari.domain.ResourceWrappedResponse;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;
import retrofit.http.Path;

public interface AmbariApi {
    
    @POST("/api/v1/blueprints/{blueprintname}")
    void createBlueprint(@Path("blueprintname") String blueprintname, @Body Blueprint blueprint);
    
    @POST("/api/v1/clusters/{cluster}/hosts/{hostName}/host_components/{component}")
    void createHostComponent(
            @Path("cluster") String cluster,
            @Path("hostName") String hostName,
            @Path("component") String component);
    
    @POST("/api/v1/clusters/{cluster}/services/{service}/components/{component}")
    void createComponent(
            @Path("cluster") String cluster,
            @Path("service") String service,
            @Path("component") String component);
    
    @POST("/api/v1/clusters/{cluster}/services/{service}")
    void addServiceToCluster(@Path("cluster") String cluster, @Path("service") String service);
    
    @POST("/api/v1/clusters/{cluster}/hosts/{host}")
    void addHostToCluster(@Path("cluster") String cluster, @Path("host") String host);
    
    @POST("/api/v1/clusters/{cluster}")
    void createClusterAPI(@Path("cluster") String cluster);
    
    @POST("/api/v1/clusters/{clustername}")
    void createCluster(@Path("clustername") String clustername, @Body BlueprintClusterBinding bluePrintClusterBinding);

    @Headers({
            "Content-Type: application/x-www-form-urlencoded",
            "Accept: text/plain"
    })
    @POST("/api/v1/stacks/{stack}/versions/{version}/recommendations")
    ResourceWrappedResponse<RecommendationResponse> getRecommendations(@Path("stack") String stack, @Path("version") String version, @Body RecommendationRequest body);

}


