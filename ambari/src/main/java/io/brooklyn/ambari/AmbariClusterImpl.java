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
package io.brooklyn.ambari;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;

import org.slf4j.LoggerFactory;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;

import brooklyn.config.ConfigKey;
import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.BasicStartableImpl;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.location.Location;
import io.brooklyn.ambari.agent.AmbariAgentImpl;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.hostgroup.AmbariHostGroup;
import io.brooklyn.ambari.rest.AmbariConfig;
import io.brooklyn.ambari.server.AmbariServer;

/**
 * The minimum requirements for an ambari hadoop cluster.
 * These can be set in the provisioning properties of yaml or by
 * using machines of this spec in a byon cluster.
 * <p/>
 * "minRam", 8192,
 * "osFamily", "ubuntu",
 * "osVersionRegex", "12.*",
 */
public class AmbariClusterImpl extends BasicStartableImpl implements AmbariCluster {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BasicStartableImpl.class);
    public static final ImmutableList<String> DEFAULT_SERVICES = ImmutableList.<String>of("ZOOKEEPER");
    //TODO is there an issue with rebind here?  On rebind should be populated from somewhere else?

    @Override
    public void init() {
        super.init();

        setAttribute(AMBARI_SERVER, addChild(createServerSpec(getConfig(SECURITY_GROUP))));

        List<String> services = getConfig(HADOOP_SERVICES);
        if (isServicesBasedDeployment(services)) {
            createSingleClusterOfAgents();
        } else {
            calculateTotalAgentsInHostgroups();
        }

        addEnricher(Enrichers.builder()
                .propagating(Attributes.MAIN_URI)
                .from(getAttribute(AMBARI_SERVER))
                .build());
    }

    private EntitySpec<? extends AmbariServer> createServerSpec(Object securityGroup) {
        EntitySpec<? extends AmbariServer> serverSpec = getConfig(SERVER_SPEC)
                .configure(SoftwareProcess.SUGGESTED_VERSION, getConfig(AmbariCluster.SUGGESTED_VERSION))
                .displayName("Ambari Server");
        if (securityGroup != null) {
            serverSpec.configure(SoftwareProcess.PROVISIONING_PROPERTIES.subKey("securityGroups"), securityGroup);
        }
        return serverSpec;
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);

        // TODO We could try to prevent setting SERVICE_UP until the services are all intalled
        subscribe(getAttribute(AMBARI_SERVER), AmbariServer.REGISTERED_HOSTS, new RegisteredHostEventListener(this));
        Iterable<Entity> components = allAmbariNodes();
        EtcHostsManager.setHostsOnMachines(components, getConfig(ETC_HOST_ADDRESS));
    }

    private Iterable<Entity> allAmbariNodes() {
        return Entities.descendants(this, Predicates.or(Predicates.instanceOf(AmbariServer.class), Predicates.instanceOf(AmbariAgent.class)));
    }


    static final class RegisteredHostEventListener implements SensorEventListener<List<String>> {

        private final AmbariCluster entity;

        public RegisteredHostEventListener(AmbariCluster entity) {
            this.entity = entity;
        }

        @Override
        public void onEvent(SensorEvent<List<String>> event) {
            List<String> hosts = event.getValue();
            Integer initialClusterSize = entity.getAttribute(EXPECTED_AGENTS);
            Boolean initialised = entity.getAttribute(CLUSTER_SERVICES_INITIALISE_CALLED);
            if (hosts != null && hosts.size() == initialClusterSize && !Boolean.TRUE.equals(initialised)) {
                entity.installServices();
            }
        }

    }

    @Override
    public void installServices() {
        setAttribute(CLUSTER_SERVICES_INITIALISE_CALLED, true);
        getAttribute(AMBARI_SERVER).waitForServiceUp();
        List<String> services = getConfig(HADOOP_SERVICES);
        List<String> hosts = getAttribute(AMBARI_SERVER).getAttribute(AmbariServer.REGISTERED_HOSTS);

        if (isServicesBasedDeployment(services)) {
            LOG.debug("About to create cluster with services: " + services);
            getAttribute(AMBARI_SERVER).installHDP("Cluster1", "mybp", hosts, services, getConfig(AMBARI_CONFIGURATIONS));
        } else {
            AmbariConfig config = ambariConfigFromHostgroups();
            if (config.hasHostGroups()) {
                getAttribute(AMBARI_SERVER).installHDPFromConfig("Cluster1", "mybp", config);
            } else {
                getAttribute(AMBARI_SERVER).installHDP("Cluster1", "mybp", hosts, DEFAULT_SERVICES);
            }
        }
    }

    private boolean isServicesBasedDeployment(List<String> services) {
        // As opposed to components based deployment
        return services != null && !services.isEmpty();
    }

    private AmbariConfig ambariConfigFromHostgroups() {
        AmbariConfig config = new AmbariConfig(getConfig(AMBARI_CONFIGURATIONS));

        for (AmbariHostGroup hostGroup : getHostGroups()) {
            config.add(
                    hostGroup.getDisplayName(),
                    hostGroup.getHostFQDNs(),
                    hostGroup.getConfig(AmbariHostGroup.HADOOP_COMPONENTS)
            );
        }
        return config;
    }

    private void calculateTotalAgentsInHostgroups() {
        int agentsToExpect = 0;
        for (AmbariHostGroup hostGroup : getHostGroups()) {
                agentsToExpect += hostGroup.getConfig(AmbariHostGroup.INITIAL_SIZE);
        }
        setAttribute(EXPECTED_AGENTS, agentsToExpect);
    }

    private Iterable<AmbariHostGroup> getHostGroups() {
        return Entities.descendants(this, AmbariHostGroup.class);
    }

    private void createSingleClusterOfAgents() {
        int initialSize = getRequiredConfig(INITIAL_SIZE);
        setAttribute(EXPECTED_AGENTS, initialSize);
        setAttribute(AMBARI_AGENTS, addChild(EntitySpec.create(DynamicCluster.class)
                        .configure(DynamicCluster.INITIAL_SIZE, initialSize)
                        .configure(DynamicCluster.MEMBER_SPEC, AmbariAgentImpl.createAgentSpec(this))
                        .displayName("All Nodes")
        ));
    }

    private <T> T getRequiredConfig(ConfigKey<T> key) {
        return checkNotNull(getConfig(key), "config %s", key);
    }
}
