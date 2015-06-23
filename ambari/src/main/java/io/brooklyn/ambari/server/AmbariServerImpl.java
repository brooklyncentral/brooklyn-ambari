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
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse;
import org.apache.ambari.server.api.services.stackadvisor.recommendations.RecommendationResponse.Recommendation;
import org.apache.http.auth.UsernamePasswordCredentials;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.JsonPath;

import brooklyn.enricher.Enrichers;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.location.access.BrooklynAccessUtils;
import brooklyn.util.guava.Functionals;
import brooklyn.util.http.HttpTool;
import io.brooklyn.ambari.domain.RecommendationRequest;
import io.brooklyn.ambari.domain.ResourceWrappedResponse;
import io.brooklyn.ambari.rest.AmbariApi;
import io.brooklyn.ambari.rest.AmbariApiHelper;

public class AmbariServerImpl extends SoftwareProcessImpl implements AmbariServer {

    private volatile HttpFeed serviceUpHttpFeed;
    private volatile HttpFeed hostsHttpFeed;
    //TODO clearly needs changed
    private UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials("admin", "admin");

    @Override
    public Class<?> getDriverInterface() {
        return AmbariServerDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        connectServiceUpIsRunning();

        HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, getAttribute(HTTP_PORT));
        String ambariUri = String.format("http://%s:%d/", hp.getHostText(), hp.getPort());
        setAttribute(Attributes.MAIN_URI, URI.create(ambariUri));

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
                .baseUri(ambariUri + "api/v1/hosts")
                .credentials("admin", "admin")
                .header(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials))
                .poll(new HttpPollConfig<List<String>>(REGISTERED_HOSTS)
                        // todo: sure httpvaluefunctions has a method that can replace getHosts()
                        .onSuccess(Functionals.chain(HttpValueFunctions.jsonContents(), getHosts()))
                        .onFailureOrException(Functions.<List<String>>constant(ImmutableList.<String>of())))
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

    @Override
    public void disconnectSensors() {
        if (serviceUpHttpFeed != null) serviceUpHttpFeed.stop();
        if (hostsHttpFeed != null) hostsHttpFeed.stop();
        disconnectServiceUpIsRunning();
        super.disconnectSensors();
    }

    @Override
    public void addHostToCluster(String cluster, String hostName) {
        waitForServiceUp();
        getApiService().addHostToCluster(cluster, hostName);
    }

    @Override
    public void addServiceToCluster(String cluster, String service) {
        waitForServiceUp();
        getApiService().addServiceToCluster(cluster, service);
    }

    @Override
    public void addComponentToCluster(String cluster, String service, String component) {
        waitForServiceUp();
        getApiService().createComponent(cluster, service, component);
    }

    @Override
    public void createHostComponent(String cluster, String hostName, String component) {
        waitForServiceUp();
        getApiService().createHostComponent(cluster, hostName, component);
    }

    @Override
    public void installHDP(String clusterName, String blueprintName, List<String> hosts, List<String> services) {
        waitForServiceUp();
        AmbariApi api = getApiService();
        ResourceWrappedResponse<RecommendationResponse> recommendations = api.getRecommendations("HDP", "2.2", new RecommendationRequest(hosts, services));
        Recommendation recommendation = recommendations.getResources().get(0).getRecommendations();
        api.createBlueprint(blueprintName, recommendation.getBlueprint());
        api.createCluster(clusterName, recommendation.getBlueprintClusterBinding());
    }

    private AmbariApi getApiService() {
        String uri = getAttribute(Attributes.MAIN_URI).toString();
        String username = usernamePasswordCredentials.getUserName();
        String password = usernamePasswordCredentials.getPassword();
        return AmbariApiHelper.newApi(uri, username, password);
    }

}
