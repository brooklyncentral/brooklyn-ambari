package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.location.access.BrooklynAccessUtils;
import brooklyn.util.collections.Jsonya;
import brooklyn.util.http.HttpTool;
import brooklyn.util.http.HttpToolResponse;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.HttpClient;

import javax.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.concurrent.TimeUnit;

public class AmbariServerImpl extends SoftwareProcessImpl implements AmbariServer {

    private volatile HttpFeed httpFeed;

    @Override
    public Class getDriverInterface() {
        return AmbariServerDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();

        //TODO need configured param not 8080
        HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, 8080);

        String ambariUri = String.format("http://%s:%d/", hp.getHostText(), hp.getPort());
        setAttribute(Attributes.MAIN_URI, URI.create(ambariUri));

        httpFeed = HttpFeed.builder()
                .entity(this)
                .period(500, TimeUnit.MILLISECONDS)
                .baseUri(ambariUri)
                .poll(new HttpPollConfig<Boolean>(SERVICE_UP)
                        .onSuccess(HttpValueFunctions.responseCodeEquals(200))
                        .onFailureOrException(Functions.constant(false)))
                .build();
        /**
         * see {@link brooklyn.entity.nosql.riak.RiakNodeImpl}
         * https://github.com/cloudsoft/bt-hpc/blob/master/brooklyn-vello/src/main/java/io/cloudsoft/vello/impl/VelloFlowClientImpl.java
         */
//        httpFeed = HttpFeed.builder().credentials()
    }

    @Override
    public void disconnectSensors() {
        super.disconnectSensors();

        if (httpFeed != null) httpFeed.stop();
    }

    @Override
    public void createCluster(String cluster) {
        waitForServiceUp();
        //TODO clearly needs changed
        UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials("admin","admin");
        //TODO trustAll should probably be fixed
        URI attribute = getAttribute(Attributes.MAIN_URI);
        URI uri = UriBuilder.fromUri(attribute).path("/api/v1/clusters/{cluster}").build(cluster);
        HttpClient httpClient = HttpTool.httpClientBuilder().credentials(usernamePasswordCredentials).trustAll().uri(uri).build();
        ImmutableMap<String, String> headers = ImmutableMap.of("x-requested-by", "bob", HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials));
        String json = Jsonya.newInstance().at("Clusters").put("version","HDP-2.2").root().toString();
        //TODO should handle failure
        HttpToolResponse httpToolResponse = HttpTool.httpPost(httpClient, uri, headers, json.getBytes());
    }

    //TODO register agent
}
