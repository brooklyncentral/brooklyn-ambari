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

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;
import static com.google.common.base.Preconditions.checkNotNull;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.server.AmbariServer;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

import org.jclouds.compute.ComputeService;
import org.jclouds.compute.domain.Template;
import org.jclouds.compute.domain.TemplateBuilder;
import org.jclouds.net.domain.IpPermission;
import org.jclouds.net.domain.IpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import brooklyn.config.ConfigKey;
import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.BasicStartableImpl;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.entity.software.ssh.SshCommandSensor;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.location.Location;
import brooklyn.location.jclouds.BasicJcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsLocation;
import brooklyn.location.jclouds.JcloudsLocationConfig;
import brooklyn.location.jclouds.JcloudsLocationCustomizer;
import brooklyn.location.jclouds.JcloudsSshMachineLocation;
import brooklyn.location.jclouds.networking.JcloudsLocationSecurityGroupCustomizer;
import brooklyn.util.config.ConfigBag;

import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Ints;

/**
                "minRam", 8192,
                "osFamily", "ubuntu",
                "osVersionRegex", "12.*",
 */
public class AmbariClusterImpl extends BasicStartableImpl implements AmbariCluster {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BasicStartableImpl.class);
    public static final ImmutableList<String> DEFAULT_SERVICES = ImmutableList.<String>of("ZOOKEEPER");
    //TODO is there an issue with rebind here?  On rebind should be populated from somewhere else?

    @Override
    public void init() {
        super.init();

        SshCommandSensor<String> hostnameSensor = new SshCommandSensor<String>(ConfigBag.newInstance()
            .configure(SshCommandSensor.SENSOR_NAME, "fqdn")
            .configure(SshCommandSensor.SENSOR_COMMAND, "hostname -s"
        ));

        Object securityGroup = getConfig(SECURITY_GROUP);
        setAttribute(AMBARI_SERVER, addChild(createServerSpec(hostnameSensor, securityGroup)));
        
        setAttribute(AMBARI_AGENTS, addChild(EntitySpec.create(DynamicCluster.class)
            .configure(DynamicCluster.INITIAL_SIZE, getRequiredConfig(INITIAL_SIZE))
            .configure(DynamicCluster.MEMBER_SPEC, createAgentSpec(hostnameSensor, securityGroup))
            .displayName("All Nodes")
        ));
        
        addEnricher(Enrichers.builder()
            .propagating(Attributes.MAIN_URI)
            .from(getAttribute(AMBARI_SERVER))
            .build());
    }

    private EntitySpec<? extends AmbariServer> createServerSpec(SshCommandSensor<String> hostnameSensor, Object securityGroup) {
        EntitySpec<? extends AmbariServer> serverSpec = getConfig(SERVER_SPEC)
            .configure(SoftwareProcess.SUGGESTED_VERSION, getConfig(AmbariCluster.SUGGESTED_VERSION))
            .displayName("Ambari Server")
            .addInitializer(hostnameSensor);
        if(securityGroup != null){
            serverSpec.configure(SoftwareProcess.PROVISIONING_PROPERTIES.subKey("securityGroups"), securityGroup);
        }
        return serverSpec;
    }
    

    private EntitySpec<? extends AmbariAgent> createAgentSpec(SshCommandSensor<String> hostnameSensor, Object securityGroup) {
        EntitySpec<? extends AmbariAgent> agentSpec = getConfig(AGENT_SPEC)
            .configure(AmbariAgent.AMBARI_SERVER_FQDN, 
                attributeWhenReady(getAttribute(AMBARI_SERVER),AmbariServer.HOSTNAME))
            .configure(SoftwareProcess.SUGGESTED_VERSION, 
                getConfig(AmbariCluster.SUGGESTED_VERSION))
            //TODO shouldn't use default os
            .addInitializer(hostnameSensor);
        if(securityGroup != null){
            agentSpec.configure(SoftwareProcess.PROVISIONING_PROPERTIES.subKey("securityGroups"), securityGroup);
        }
        return agentSpec;
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);

        // TODO We could try to prevent setting SERVICE_UP until the services are all intalled
        subscribe(getAttribute(AMBARI_SERVER), AmbariServer.REGISTERED_HOSTS, new RegisteredHostEventListener(this));
    }

    static final class RegisteredHostEventListener implements SensorEventListener<List<String>> {
        private final AmbariCluster entity;
        
        public RegisteredHostEventListener(AmbariCluster entity) {
            this.entity = entity;
        }
        
        @Override
        public void onEvent(SensorEvent<List<String>> event) {
            List<String> hosts = event.getValue();
            Integer initialClusterSize = entity.getConfig(INITIAL_SIZE);
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

        LOG.debug("About to create cluster with services: " + services);
        if (services != null && services.size() > 0) {
            getAttribute(AMBARI_SERVER).installHDP("Cluster1", "mybp", hosts, services);
        } else {
            getAttribute(AMBARI_SERVER).installHDP("Cluster1", "mybp", hosts, DEFAULT_SERVICES);
        }
    }

    private <T> T getRequiredConfig(ConfigKey<T> key) {
        return checkNotNull(getConfig(key), "config %s", key);
    }
}
