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

import static java.lang.String.format;
import static org.apache.brooklyn.util.ssh.BashCommands.installPackage;
import static org.apache.brooklyn.util.ssh.BashCommands.sudo;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.EntityLocal;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import org.apache.brooklyn.entity.software.base.SoftwareProcess;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.ssh.BashCommands;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.AmbariInstallCommands;
import io.brooklyn.ambari.hostgroup.AmbariHostGroup;
import io.brooklyn.ambari.server.AmbariServer;

public class AmbariAgentSshDriver extends JavaSoftwareProcessSshDriver implements AmbariAgentDriver {
    public static final Logger log = LoggerFactory.getLogger(AmbariAgentSshDriver.class);
    private final AmbariInstallCommands defaultAmbariInstallHelper =
            new AmbariInstallCommands(
                    entity.getConfig(SoftwareProcess.SUGGESTED_VERSION),
                    entity.getConfig(AmbariCluster.REPO_BASE_URL));

    public AmbariAgentSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    protected String getLogFileLocation() {
        return "/var/log/ambari-agent/ambari-agent.log";
    }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", false), CHECK_RUNNING).body.append(sudo("ambari-agent status")).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(STOPPING).body.append(sudo("ambari-agent stop")).execute();
    }

    @Override
    public AmbariAgentImpl getEntity() {
        return AmbariAgentImpl.class.cast(super.getEntity());
    }

    @Override
    public void install() {
        String parentFQDN =
        entity.getParent() instanceof AmbariServer
                ? ((AmbariServer) entity.getParent()).getFqdn()
                : "";

        Entity parentHostGroup =
                Iterables.getFirst(
                        Iterables.filter(Entities.ancestors(entity), AmbariHostGroup.class),
                        entity);

        String fqdn =
                parentFQDN.isEmpty()
                ? String.format("%s-%s.%s", parentHostGroup.getDisplayName().toLowerCase(), entity.getId().toLowerCase(), entity.getConfig(AmbariCluster.DOMAIN_NAME))
                : parentFQDN;

        getEntity().setFqdn(fqdn);
        ImmutableList<String> commands =
                ImmutableList.<String>builder()
                        .add(defaultAmbariInstallHelper.installAmbariRequirements(getMachine()))
                        .addAll(BashCommands.setHostname(fqdn))
                        .add(installPackage("ambari-agent"))
                        .build();

        newScript(INSTALLING).body
                .append(commands)
                .failOnNonZeroResultCode()
                .execute();
    }

    @Override
    public void customize() {
        String tmpConfigFileLoc = "/tmp/ambari-agent.ini";
        String destinationConfigFile = "/etc/ambari-agent/conf/ambari-agent.ini";

        copyTemplate(getTemplateConfigurationUrl(), tmpConfigFileLoc);

        newScript(CUSTOMIZING)
                .body.append(sudo(format("mv %s %s", tmpConfigFileLoc, destinationConfigFile)))
                .failOnNonZeroResultCode()
                .execute();

    }

    @Override
    public void launch() {
        newScript(LAUNCHING).body.append(sudo("ambari-agent start")).failOnNonZeroResultCode().execute();
    }

    String getTemplateConfigurationUrl() {
        return entity.getConfig(AmbariAgent.TEMPLATE_CONFIGURATION_URL);
    }

}
