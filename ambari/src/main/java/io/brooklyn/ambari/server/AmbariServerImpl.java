/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.brooklyn.ambari.server;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.JsonPath;

import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.location.access.BrooklynAccessUtils;
import brooklyn.util.guava.Functionals;
import brooklyn.util.http.HttpTool;
import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.rest.AmbariApiException;
import io.brooklyn.ambari.rest.AmbariRequestInterceptor;
import io.brooklyn.ambari.rest.domain.RecommendationWrapper;
import io.brooklyn.ambari.rest.domain.RecommendationWrappers;
import io.brooklyn.ambari.rest.domain.Request;
import io.brooklyn.ambari.rest.endpoint.BlueprintEndpoint;
import io.brooklyn.ambari.rest.endpoint.ClusterEndpoint;
import io.brooklyn.ambari.rest.endpoint.ConfigurationEnpoint;
import io.brooklyn.ambari.rest.endpoint.HostEndpoint;
import io.brooklyn.ambari.rest.endpoint.ServiceEndpoint;
import io.brooklyn.ambari.rest.endpoint.StackEndpoint;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

public class AmbariServerImpl extends SoftwareProcessImpl implements AmbariServer {

    public static final Logger LOG = LoggerFactory.getLogger(AmbariServerImpl.class);
    private volatile HttpFeed serviceUpHttpFeed;
    private volatile HttpFeed hostsHttpFeed;
    private volatile HttpFeed clusterHttpFeed;

    private String ambariUri;
    private RestAdapter restAdapter;

    //TODO clearly needs changed
    private UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials("admin", "admin");
    public static final Map<String, String> BASE_BLUEPRINTS = ImmutableMap.of("stack_name", "HDP", "stack_version", "2.2");
    public static final List<? extends Map<?, ?>> CONFIGURATIONS = ImmutableList.of(ImmutableMap.of("nagios-env", ImmutableMap.of("nagios_contact", "admin@localhost")));

    @Override
    public Class<AmbariServerDriver> getDriverInterface() {
        return AmbariServerDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        connectServiceUpIsRunning();

        HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, getAttribute(HTTP_PORT));

        ambariUri = String.format("http://%s:%d", hp.getHostText(), hp.getPort());

        setAttribute(Attributes.MAIN_URI, URI.create(ambariUri));

        restAdapter = new RestAdapter.Builder()
                .setEndpoint(ambariUri)
                .setRequestInterceptor(new AmbariRequestInterceptor(usernamePasswordCredentials))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        serviceUpHttpFeed = HttpFeed.builder()
                .entity(this)
                .period(500, TimeUnit.MILLISECONDS)
                .baseUri(ambariUri)
                .poll(new HttpPollConfig<Boolean>(URL_REACHABLE)
                        .onSuccess(HttpValueFunctions.responseCodeEquals(200))
                        .onFailureOrException(Functions.constant(false)))
                .build();

        addEnricher(Enrichers.builder().updatingMap(Attributes.SERVICE_NOT_UP_INDICATORS)
                .from(URL_REACHABLE)
                .computing(Functionals.ifNotEquals(true).value("URL not reachable"))
                .build());

        hostsHttpFeed = HttpFeed.builder()
                .entity(this)
                .period(1000, TimeUnit.MILLISECONDS)
                .baseUri(String.format("%s/api/v1/hosts", ambariUri))
                .credentials("admin", "admin")
                .header(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials))
                .poll(new HttpPollConfig<List<String>>(REGISTERED_HOSTS)
                        .onSuccess(Functionals.chain(HttpValueFunctions.jsonContents(), getHosts()))
                        .onFailureOrException(Functions.<List<String>>constant(ImmutableList.<String>of())))
                .build();

        clusterHttpFeed = HttpFeed.builder()
                .entity(this)
                .period(1000, TimeUnit.MILLISECONDS)
                .baseUri(String.format("%s/api/v1/clusters/%s/requests/%d",
                        ambariUri,
                        "Cluster1",
                        1))
                .credentials("admin", "admin")
                .header(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials))
                .poll(new HttpPollConfig<String>(CLUSTER_STATE)
                        .onSuccess(Functionals.chain(HttpValueFunctions.jsonContents(), getClusterState()))
                        .onFailureOrException(Functions.<String>constant(null)))
                .build();
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

    Function<JsonElement, String> getClusterState() {
        Function<JsonElement, String> path = new Function<JsonElement, String>() {
            @Nullable
            @Override
            public String apply(@Nullable JsonElement jsonElement) {
                String jsonString = jsonElement.toString();
                return JsonPath.read(jsonString, "$.Requests.request_status");
            }
        };
        return path;
    }

    @Override
    public void disconnectSensors() {
        super.disconnectSensors();
        disconnectServiceUpIsRunning();

        if (serviceUpHttpFeed != null) serviceUpHttpFeed.stop();
        if (hostsHttpFeed != null) hostsHttpFeed.stop();
        if (clusterHttpFeed != null) clusterHttpFeed.stop();
    }

    @Override
    public RecommendationWrappers getRecommendations(String stackName, String stackVersion, List<String> hosts, List<String> services) {
        waitForServiceUp();

        return restAdapter.create(StackEndpoint.class).getRecommendations(stackName, stackVersion, ImmutableMap.builder()
                .put("hosts", hosts)
                .put("services", services)
                .put("recommend", "host_groups")
                .build());
    }

    @Override
    public Request deployCluster(String clusterName, String blueprintName, RecommendationWrapper recommendationWrapper, Map config) throws AmbariApiException {
        Preconditions.checkNotNull(recommendationWrapper);
        Preconditions.checkNotNull(recommendationWrapper.getStack());
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation());
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation().getBlueprint());
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation().getBindings());

        try {
            restAdapter.create(BlueprintEndpoint.class).createBlueprint(blueprintName, ImmutableMap.builder()
                    .put("host_groups", recommendationWrapper.getRecommendation().getBlueprint().getHostGroups())
                    .put("configurations", getConfigurations(config))
                    .put("Blueprints", recommendationWrapper.getStack())
                    .build());

            return restAdapter.create(ClusterEndpoint.class).createCluster(clusterName, ImmutableMap.builder()
                    .put("blueprint", blueprintName)
                    .put("default_password", "admin")
                    .put("host_groups", recommendationWrapper.getRecommendation().getBindings().getHostGroups())
                    .build());
        } catch (RetrofitError retrofitError) {
            throw new AmbariApiException(retrofitError);
        }
    }

    @Override
    public void addHostToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                 @EffectorParam(name = "Host FQDN") String hostName) {
        waitForServiceUp();
        restAdapter.create(HostEndpoint.class).addHost(cluster, hostName);

    }

    @Override
    public void addServiceToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                    @EffectorParam(name = "Service") String service) {
        waitForServiceUp();
        restAdapter.create(ServiceEndpoint.class).addService(cluster, service);
    }

    @Override
    public void createComponentToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                         @EffectorParam(name = "Service name") String service,
                                         @EffectorParam(name = "Component name") String component) {
        waitForServiceUp();
        restAdapter.create(ServiceEndpoint.class).createComponent(cluster, service, component);
    }

    @Override
    public void createHostComponent(@EffectorParam(name = "Cluster name") String cluster,
                                    @EffectorParam(name = "Host FQDN") String hostName,
                                    @EffectorParam(name = "Component name") String component) {
        waitForServiceUp();
        restAdapter.create(HostEndpoint.class).createHostComponent(cluster, hostName, component);
    }

    @Override
    public void createComponentConfiguration(@EffectorParam(name = "Cluster name") String cluster,
                                             @EffectorParam(name = "Component configuration key") String key,
                                             @EffectorParam(name = "Component configuration") Map<Object, Object> config) {
        waitForServiceUp();
        restAdapter.create(ConfigurationEnpoint.class).createConfiguration(cluster, ImmutableMap.builder()
                .put("Clusters", ImmutableMap.builder()
                        .put("desired_configs", ImmutableMap.builder()
                                .put("type", key)
                                .put("tag", String.format("version%d", System.currentTimeMillis()))
                                .put("properties", config)
                                .build())
                        .build())
                .build());
    }

    @Override
    public Request installService(@EffectorParam(name = "Cluster name") String cluster,
                                  @EffectorParam(name = "Service name") String service) {
        waitForServiceUp();
        return restAdapter.create(ServiceEndpoint.class).updateService(cluster, service, ImmutableMap.builder()
                .put("RequestInfo", ImmutableMap.builder()
                        .put("context", String.format("Install %s service", service))
                        .build())
                .put("ServiceInfo", ImmutableMap.builder()
                        .put("state", "INSTALLED")
                        .build())
                .build());
    }

    @Override
    public Request startService(@EffectorParam(name = "Cluster name") String cluster,
                                @EffectorParam(name = "Service name") String service) {
        waitForServiceUp();
        return restAdapter.create(ServiceEndpoint.class).updateService(cluster, service, ImmutableMap.builder()
                .put("RequestInfo", ImmutableMap.builder()
                        .put("context", String.format("Start %s service", service))
                        .build())
                .put("ServiceInfo", ImmutableMap.builder()
                        .put("state", "STARTED")
                        .build())
                .build());
    }

    @Override
    public void createCluster(@EffectorParam(name = "Cluster Name") String clusterName,
                              @EffectorParam(name = "Blueprint Name") String blueprintName,
                              @EffectorParam(name = "Stack Name") String stackName,
                              @EffectorParam(name = "Stack version") String stackVersion,
                              @EffectorParam(name = "Hosts", description = "List of FQDNs to add to cluster") List<String> hosts,
                              @EffectorParam(name = "Services", description = "List of services to install on cluster") List<String> services,
                              @EffectorParam(name = "Configurations", description = "Map of configurations to apply to blueprint") Map<String, Map> config) {
        final RecommendationWrappers recommendationWrappers = getRecommendations(stackName, stackVersion, hosts, services);

        deployCluster(clusterName, blueprintName, recommendationWrappers.getRecommendationWrappers().size() > 0 ? recommendationWrappers.getRecommendationWrappers().get(0) : null, config);
    }

    @Override
    public void updateStackRepository(@EffectorParam(name = "Stack Name") String stackName, @EffectorParam(name = "Stack Version") String stackVersion, @EffectorParam(name = "Operating System") String os, @EffectorParam(name = "Repository Name") String repoName, @EffectorParam(name = "Repository URL") String url) {
        waitForServiceUp();
        restAdapter.create(StackEndpoint.class)
                .updateStackRepository(stackName, stackVersion, os, repoName, ImmutableMap.builder()
                        .put("Repositories", ImmutableMap.builder()
                                .put("base_url", url)
                                .put("verify_base_url", true)
                                .build())
                        .build());
    }

    @Override
    public boolean agentOnServer() {
        Iterable<AmbariCluster> ambariClusters = Iterables.filter(Entities.ancestors(this), AmbariCluster.class);
        for(Entity parent: ambariClusters) {
                return !parent.getConfig(AmbariCluster.SERVER_COMPONENTS).isEmpty();
        }
        return false;
    }

    private List<? extends Map<?, ?>> getConfigurations(Map<String, Map> config) {
        ImmutableList.Builder<Map<?, ?>> builder = ImmutableList.<Map<?, ?>>builder();
        if (config != null) {
            for (Map.Entry<String, Map> stringMapEntry : config.entrySet()) {
                builder.add(
                        ImmutableMap.of(
                                stringMapEntry.getKey(),
                                ImmutableMap.<String, Map>of(
                                        "properties",
                                        stringMapEntry.getValue())));
            }
        }
        return builder.build();
    }

    @Override
    public void setFqdn(String fqdn) {
        setAttribute(FQDN, fqdn);
    }

    @Override
    public String getFqdn() {
        return getAttribute(FQDN);
    }
}
