package io.brooklyn.ambari.rest;

import retrofit.http.POST;
import retrofit.http.Path;

public interface AmbariApi {
    
    @POST("/api/v1/blueprints/{blueprintname}")
    void createBlueprint(@Path("blueprintname") String blueprintname);
    
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
    void createCluster(@Path("clustername") String clustername);

}
