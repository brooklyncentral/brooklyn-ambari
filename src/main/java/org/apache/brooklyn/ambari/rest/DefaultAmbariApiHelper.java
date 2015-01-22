package org.apache.brooklyn.ambari.rest;

import brooklyn.util.collections.Jsonya;
import brooklyn.util.http.HttpTool;
import brooklyn.util.http.HttpToolResponse;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HttpHeaders;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.Set;

public class DefaultAmbariApiHelper implements AmbariApiHelper {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultAmbariApiHelper.class);

    @Override
    public void createClusterAPI(String cluster, UsernamePasswordCredentials usernamePasswordCredentials, URI baseUri) {
        String json = Jsonya.newInstance().at("Clusters").put("version", "HDP-2.2").root().toString();
        post(usernamePasswordCredentials, baseUri, json.getBytes(), "/api/v1/clusters/{cluster}", cluster);
    }

    @Override
    public void addHostToCluster(String cluster, String host, UsernamePasswordCredentials usernamePasswordCredentials, URI baseUri) {
        post(usernamePasswordCredentials, baseUri, new byte[0], "/api/v1/clusters/{cluster}/hosts/{host}", cluster, host);
    }

    @Override
    public void addServiceToCluster(String cluster, String service, UsernamePasswordCredentials usernamePasswordCredentials, URI baseUri) {
        post(usernamePasswordCredentials, baseUri, new byte[0], "/api/v1/clusters/{cluster}/services/{service}", cluster, service);
    }

    @Override
    public void createComponent(String cluster, String service, String component, UsernamePasswordCredentials usernamePasswordCredentials, URI baseUri) {
        post(usernamePasswordCredentials, baseUri, new byte[0], "/api/v1/clusters/{cluster}/services/{service}/components/{component}", cluster, service, component);
    }

    @Override
    public void createHostComponent(String cluster, String hostName, String component, UsernamePasswordCredentials usernamePasswordCredentials, URI baseUri) {
        post(usernamePasswordCredentials, baseUri, new byte[0], "/api/v1/clusters/{cluster}/hosts/{hostName}/host_components/{component}", cluster, hostName, component);
    }

    @Override
    public void createBlueprint(DefaultAmbariBluePrint blueprint, UsernamePasswordCredentials usernamePasswordCredentials, URI attribute) {

    }

    @Override
    public RecommendationResponse getRecommendations(Set<String> hosts, Iterable<String> services, UsernamePasswordCredentials usernamePasswordCredentials, URI baseUri) {
        String json = Jsonya.newInstance()
                .root().put("hosts", hosts)
                .root().put("services",services)
                .root().put("recommend","host_groups")
                .root().toString();
        HttpToolResponse httpToolResponse = post(usernamePasswordCredentials, baseUri, json.getBytes(), "/api/v1/stacks/HDP/versions/2.2/recommendations");

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String contentAsString = httpToolResponse.getContentAsString();
            RecommendationResponse recommendation = objectMapper.readValue(contentAsString, RecommendationResponse.class);
            return recommendation;
        } catch (IOException e) {
            e.printStackTrace();
            //TODO Handle this failure
        }
        return null;
    }

    private HttpToolResponse post(UsernamePasswordCredentials usernamePasswordCredentials, URI baseUri, byte[] body, String path, String... templateParams) {
        URI uri = UriBuilder.fromUri(baseUri).path(path).build(templateParams);
        //TODO trustAll should probably be fixed
        HttpClient httpClient = HttpTool.httpClientBuilder().credentials(usernamePasswordCredentials).trustAll().uri(uri).build();
        ImmutableMap<String, String> headers = ImmutableMap.of("x-requested-by", "bob", HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials));
        //TODO should handle failure
        HttpToolResponse httpToolResponse = HttpTool.httpPost(httpClient, uri, headers, body);
        return httpToolResponse;
    }

}