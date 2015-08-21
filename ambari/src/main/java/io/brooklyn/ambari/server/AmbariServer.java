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

import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;

import brooklyn.catalog.Catalog;
import brooklyn.entity.annotation.Effector;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.PortAttributeSensorAndConfigKey;
import brooklyn.event.basic.Sensors;
import io.brooklyn.ambari.AmbariNode;
import io.brooklyn.ambari.rest.domain.RecommendationWrapper;
import io.brooklyn.ambari.rest.domain.RecommendationWrappers;
import io.brooklyn.ambari.rest.domain.Request;

@Catalog(name = "Ambari Server", description = "Ambari Server: part of an ambari cluster used to install and monitor a hadoop cluster.")
@ImplementedBy(AmbariServerImpl.class)
public interface AmbariServer extends AmbariNode {

    // TODO this value is read-only; changing its config value is not reflected in the deployed artifacts!
    PortAttributeSensorAndConfigKey HTTP_PORT =
            new PortAttributeSensorAndConfigKey("ambari.server.httpPort", "HTTP Port", "8080");

    AttributeSensor<List<String>> REGISTERED_HOSTS = Sensors.newSensor(
            new TypeToken<List<String>>() {
            },
            "ambari.server.registeredHosts",
            "List of registered agent names");

    AttributeSensor<Boolean> URL_REACHABLE = Sensors.newBooleanSensor("ambari.server.urlReachable");

    AttributeSensor<String> CLUSTER_STATE = Sensors.newStringSensor("ambari.server.clusterState");

    /**
     * @throws IllegalStateException if times out.
     */
    public void waitForServiceUp();

    /**
     * Retrieves the Ambari recommendations for the given hosts / services from the REST API.
     *
     * @param stackName    the stack name to use.
     * @param stackVersion the stack version to use.
     * @param hosts        a list of registered hosts to the Ambari server.
     * @param services     a list of services to install.
     * @return the recommendations formulated by Ambari.
     */
    public RecommendationWrappers getRecommendations(String stackName, String stackVersion, List<String> hosts, List<String> services);

    /**
     * Creates a new blueprint and deploys it, based on the Ambari recommendations.
     *
     * @param clusterName           the cluster name to use.
     * @param blueprintName         the blueprint name to use.
     * @param recommendationWrapper the Ambari recommendation to create the blueprint and deploy it.
     * @param config                the additional configuration for the Hadoop services.
     * @return a request corresponding to the Ambari's operation.
     */
    public Request deployCluster(String clusterName, String blueprintName, RecommendationWrapper recommendationWrapper, Map config);

    @Effector(description = "Adds a host to a cluster")
    public void addHostToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                 @EffectorParam(name = "Host FQDN") String hostName);

    @Effector(description = "Add a service to a cluster")
    public void addServiceToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                    @EffectorParam(name = "Service name") String service);

    @Effector(description = "Create component")
    public void createComponentToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                         @EffectorParam(name = "Service name") String service,
                                         @EffectorParam(name = "Component name") String component);

    @Effector(description = "Create a new configuration for a specific component")
    public void createComponentConfiguration(@EffectorParam(name = "Cluster name") String cluster,
                                             @EffectorParam(name = "Configuration key") String key,
                                             @EffectorParam(name = "Configuration") Map<Object, Object> config);

    @Effector(description = "Create host component")
    public void createHostComponent(@EffectorParam(name = "Cluster name") String cluster,
                                    @EffectorParam(name = "Host FQDN") String hostName,
                                    @EffectorParam(name = "Component name") String component);

    @Effector(description = "Install a service on a cluster")
    public Request installService(@EffectorParam(name = "Cluster name") String cluster,
                                  @EffectorParam(name = "Service name") String service);

    @Effector(description = "Start a service on a cluster")
    public Request startService(@EffectorParam(name = "Cluster name") String cluster,
                                @EffectorParam(name = "Service name") String service);

    @Effector(description = "Create, configure and install cluster based on Ambari recommendation from the given hosts and services")
    public void createCluster(@EffectorParam(name = "Cluster Name") String clusterName,
                              @EffectorParam(name = "Blueprint Name") String blueprintName,
                              @EffectorParam(name = "Stack Name") String stackName,
                              @EffectorParam(name = "Stack version") String stackVersion,
                              @EffectorParam(name = "Hosts", description = "List of FQDNs to add to cluster") List<String> hosts,
                              @EffectorParam(name = "Services", description = "List of services to install on cluster") List<String> services,
                              @EffectorParam(name = "Configurations", description = "Map of configurations to apply to blueprint") Map<String, Map> config);

    @Effector(description = "Update the stack url")
    public void updateStackRepository(@EffectorParam(name = "Stack Name") String stackName,
                                      @EffectorParam(name = "Stack Version") String stackVersion,
                                      @EffectorParam(name = "Operating System") String os,
                                      @EffectorParam(name = "Repository Name") String repoName,
                                      @EffectorParam(name = "Repository URL") String url);
}


