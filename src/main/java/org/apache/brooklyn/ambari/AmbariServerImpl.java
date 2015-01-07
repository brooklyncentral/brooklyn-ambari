package org.apache.brooklyn.ambari;

import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.location.access.BrooklynAccessUtils;
import brooklyn.util.guava.Functionals;
import brooklyn.util.http.HttpTool;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.auth.UsernamePasswordCredentials;

import javax.annotation.Nullable;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class AmbariServerImpl extends SoftwareProcessImpl implements AmbariServer {

    private volatile HttpFeed serviceUpHttpFeed;
    private volatile HttpFeed hostsHttpFeed;
    //TODO clearly needs changed
    private UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials("admin", "admin");
    private AmbariApiHelper ambariApiHelper = new DefaultAmbariApiHelper();

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

        serviceUpHttpFeed = HttpFeed.builder()
                .entity(this)
                .period(500, TimeUnit.MILLISECONDS)
                .baseUri(ambariUri)
                .poll(new HttpPollConfig<Boolean>(SERVICE_UP)
                        .onSuccess(HttpValueFunctions.responseCodeEquals(200))
                        .onFailureOrException(Functions.constant(false)))
                .build();

        hostsHttpFeed = HttpFeed.builder()
                .entity(this)
                .period(1000, TimeUnit.MILLISECONDS)
                .baseUri(ambariUri + "api/v1/hosts")
                .credentials("admin", "admin")
                .header(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials))
                .poll(new HttpPollConfig<List<String>>(REGISTERED_HOSTS)
                                .onSuccess(Functionals.chain(HttpValueFunctions.jsonContents(), getHosts()))
                                .onFailureOrException(Functions.<List<String>>constant(ImmutableList.<String>of()))
                ).build();

    }

    Function<JsonElement, List<String>> getHosts() {
        Function<JsonElement, List<String>> path = new Function<JsonElement, List<String>>() {
            @Nullable
            @Override
            public List<String> apply(@Nullable JsonElement jsonElement) {
                String jsonString = jsonElement.toString();
                return JsonPath.read(jsonString, "$.items[*].Hosts.host_name");
            }
        };
        return path;
    }

    @Override
    public void disconnectSensors() {
        super.disconnectSensors();

        if (serviceUpHttpFeed != null) serviceUpHttpFeed.stop();
    }

    @Override
    public void createCluster(String cluster) {
        waitForServiceUp();
    }

    @Override
    public void addHostToCluster(@EffectorParam(name = "Cluster name") String cluster, @EffectorParam(name = "Host FQDN") String hostName) {
        waitForServiceUp();
        ambariApiHelper.addHostToCluster(cluster, hostName, usernamePasswordCredentials, getAttribute(Attributes.MAIN_URI));
    }

    @Override
    public void addServiceToCluster(@EffectorParam(name = "Cluster name") String cluster, @EffectorParam(name = "Service") String service) {
        waitForServiceUp();
        ambariApiHelper.addServiceToCluster(cluster, service, usernamePasswordCredentials, getAttribute(Attributes.MAIN_URI));
    }

    @Override
    public void addComponentToCluster(@EffectorParam(name = "Cluster name") String cluster, @EffectorParam(name = "Service name") String service, @EffectorParam(name = "Component name") String component) {
        waitForServiceUp();
        ambariApiHelper.createComponent(cluster, service, component, usernamePasswordCredentials, getAttribute(Attributes.MAIN_URI));
    }

    @Override
    public void createHostComponent(@EffectorParam(name = "Cluster name") String cluster, @EffectorParam(name = "Host FQDN") String hostName, @EffectorParam(name = "Component name") String component) {
        waitForServiceUp();
        ambariApiHelper.createHostComponent(cluster, hostName, component, usernamePasswordCredentials, getAttribute(Attributes.MAIN_URI));
    }

    @Override
    public void createCluster(String cluster, Enumeration<String> hosts, Iterable<String> services) {

    }
}
