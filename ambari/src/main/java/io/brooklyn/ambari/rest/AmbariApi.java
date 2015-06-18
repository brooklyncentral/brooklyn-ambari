package io.brooklyn.ambari.rest;

import java.util.Collection;

import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.Blueprint;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.BlueprintClusterBinding;
import org.codehaus.jackson.annotate.JsonProperty;

import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.Path;

public interface AmbariApi {
    
    @POST("/api/v1/blueprints/{blueprintname}")
    void createBlueprint(@Path("blueprintname") String blueprintname, @Body Blueprint blueprint);
    
    @POST("/api/v1/clusters/{cluster}/hosts/{hostName}/host_components/{component}")
    void createHostComponent(@Path("cluster") String cluster, @Path("hostName") String hostName, @Path("component") String component);
    
    @POST("/api/v1/clusters/{cluster}/services/{service}/components/{component}")
    void createComponent(@Path("cluster") String cluster, @Path("service") String service, @Path("component") String component);
    
    @POST("/api/v1/clusters/{cluster}/services/{service}")
    void addServiceToCluster(@Path("cluster") String cluster, @Path("service") String service);
    
    @POST("/api/v1/clusters/{cluster}/hosts/{host}")
    void addHostToCluster(@Path("cluster") String cluster, @Path("host") String host);
    
    @POST("/api/v1/clusters/{cluster}")
    void createClusterAPI(@Path("cluster") String cluster);
    
    @POST("/api/v1/clusters/{clustername}")
    void createCluster(@Path("clustername") String clustername, @Body BlueprintClusterBinding bluePrintClusterBinding);
    
    @POST("/api/v1/stacks/{stack}/versions/{version}/recommendations")
    RecommendationResponse getRecommendations(@Path("stack") String stack, @Path("version") String version, @Body RecommendationRequest body);
    
    public static class RecommendationRequest {
        
        @JsonProperty
        private Collection<String> hosts;

        @JsonProperty
        private Collection<String> services;
        
        @JsonProperty
        private String recommend = "host_groups";
        
        public RecommendationRequest(Collection<String> hosts, Collection<String> services) {
            this.hosts = hosts;
            this.services = services;
        }
    }

}


