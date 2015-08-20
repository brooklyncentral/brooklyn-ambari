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

package io.brooklyn.ambari.agent;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.util.config.ConfigBag;
import io.brooklyn.ambari.AmbariCluster;

import javax.annotation.Nullable;
import java.util.List;

import static brooklyn.event.basic.DependentConfiguration.attributeWhenReady;

public class AmbariAgentImpl extends SoftwareProcessImpl implements AmbariAgent {
    @Override
    public Class getDriverInterface() {
        return AmbariAgentDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();

        //TODO - Need to wire isrunning to service up (I think)
        setAttribute(SERVICE_UP, true);
    }

    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();
    }

    public String getAmbariServerFQDN() {
        return getConfig(AMBARI_SERVER_FQDN);
    }

    @Override
    public void setFqdn(String fqdn) {
        setAttribute(FQDN, fqdn);
    }

    @Override
    public String getFqdn() {
        return getAttribute(FQDN);
    }

    @Override
    public void setComponents(List<String> components) {
        setAttribute(COMPONENTS, components);
    }

    @Nullable
    @Override
    public List<String> getComponents() {
        return getAttribute(COMPONENTS);
    }

    public static EntitySpec<? extends AmbariAgent> createAgentSpec(Entity ambariCluster, ConfigBag configBag) {
        EntitySpec<? extends AmbariAgent> agentSpec = EntitySpec.create(ambariCluster.getConfig(AmbariCluster.AGENT_SPEC))
                .configure(AMBARI_SERVER_FQDN,
                        attributeWhenReady(ambariCluster.getAttribute(AmbariCluster.AMBARI_SERVER), FQDN))
                .configure(SoftwareProcess.SUGGESTED_VERSION,
                        ambariCluster.getConfig(AmbariCluster.SUGGESTED_VERSION));
        if (configBag != null) {
            agentSpec.configure(configBag.getAllConfig());
        }
        Object securityGroup = ambariCluster.getConfig(AmbariCluster.SECURITY_GROUP);
        if (securityGroup != null) {
            agentSpec.configure(SoftwareProcess.PROVISIONING_PROPERTIES.subKey("securityGroups"), securityGroup);
        }
        return agentSpec;
    }
}
