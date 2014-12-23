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
package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.BasicStartableImpl;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.group.DynamicCluster;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.event.basic.MapConfigKey;
import brooklyn.location.Location;
import brooklyn.location.cloud.CloudLocationConfig;
import brooklyn.location.jclouds.JcloudsLocationConfig;
import brooklyn.util.time.Duration;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jclouds.compute.domain.OsFamily;

import java.util.Collection;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

public class AmbariClusterImpl extends BasicStartableImpl implements AmbariCluster {


    @Override
    public void init() {
        super.init();

        setDisplayName("Ambari Cluster");
        Integer initialSize = getConfig(INITIAL_SIZE);
        //TODO need to do something better with security groups here
        Object securityGroup = getConfig(SECURITY_GROUP);
        ImmutableMap<String, Object> serverProvisioningProperties = ImmutableMap.<String, Object>of(
                "inboundPorts", ImmutableList.of(8080, 22),
                "securityGroups", securityGroup);

        AmbariServer ambariServer = addChild(EntitySpec.create(AmbariServer.class)
                        .configure(SoftwareProcess.PROVISIONING_PROPERTIES, serverProvisioningProperties)
                        .displayName("Ambari Server")
        );


        ImmutableMap<String, Object> agentProvisioningProperties = ImmutableMap.<String, Object>of(
                "minRam", 4096,
                "osFamily", "ubuntu",
                "osVersionRegex", "12.*",
                "securityGroups", securityGroup);

        EntitySpec<AmbariAgent> agentEntitySpec = EntitySpec.create(AmbariAgent.class)
                .configure(AmbariAgent.AMBARI_SERVER_FQDN, attributeWhenReady(ambariServer, AmbariServer.HOSTNAME))
                        //TODO shouldn't use default os
                .configure(SoftwareProcess.PROVISIONING_PROPERTIES, agentProvisioningProperties);

        DynamicCluster ambariAgentCluster = addChild(EntitySpec.create(DynamicCluster.class)
                        //TODO should probably change option to hadoop cluster size
                        .configure(DynamicCluster.INITIAL_SIZE, initialSize - 1)
                        .configure(DynamicCluster.MEMBER_SPEC, agentEntitySpec)
                        .displayName("All Nodes")
        );
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);


    }
}
