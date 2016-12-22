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

import static java.lang.String.format;
import static org.apache.brooklyn.util.ssh.BashCommands.installPackage;
import static org.apache.brooklyn.util.ssh.BashCommands.sudo;
import static org.apache.brooklyn.util.ssh.BashCommands.unzip;

import java.util.List;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.EntityLocal;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import org.apache.brooklyn.entity.software.base.SoftwareProcess;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.ssh.BashCommands;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.AmbariInstallCommands;
import io.brooklyn.ambari.service.CustomService;

public class AmbariServerSshDriver extends JavaSoftwareProcessSshDriver implements AmbariServerDriver {

    public static final String RESOURCE_STACK_LOCATION = "/var/lib/ambari-server/resources/stacks/%s/%s/services/";
    private final AmbariInstallCommands ambariInstallHelper =
            new AmbariInstallCommands(
                    entity.getConfig(SoftwareProcess.SUGGESTED_VERSION),
                    entity.getConfig(AmbariCluster.REPO_BASE_URL));

    public AmbariServerSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public AmbariServerImpl getEntity() {
        return AmbariServerImpl.class.cast(super.getEntity());
    }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", false), CHECK_RUNNING)
                .body.append(sudo("ambari-server status"))
                .execute() == 0;
    }

    @Override
    protected String getLogFileLocation() {
        return "/var/log/ambari-server/ambari-server.log";
    }

    @Override
    public void stop() {
        newScript(STOPPING).body.append(sudo("ambari-server stop")).execute();
    }

    @Override
    public void install() {
        String fqdn = String.format("%s-%s.%s", entity.getConfig(AmbariCluster.SERVER_HOST_GROUP).toLowerCase(), entity.getId().toLowerCase(), entity.getConfig(AmbariCluster.DOMAIN_NAME));
        getEntity().setFqdn(fqdn);
        ImmutableList<String> commands =
                ImmutableList.<String>builder()
                        .add(ambariInstallHelper.installAmbariRequirements(getMachine()))
                        .addAll(BashCommands.setHostname(fqdn))
                        .add(installPackage("ambari-server"))
                        .build();

        newScript(INSTALLING).body
                .append(commands)
                .failOnNonZeroResultCode()
                .execute();
    }

    @Override
    public void customize() {
        List<String> extraStackDefinitions = getExtraStackDefinitionUrls();
        ImmutableList.Builder<String> builder = ImmutableList.<String>builder();

        if (!extraStackDefinitions.isEmpty()) {
            for (String extraStackDefinition : extraStackDefinitions) {
                String tmpLocation = copyToTmp(extraStackDefinition);
                builder.add(getUnpackCommand(tmpLocation));
            }
        }

        AmbariCluster ambariCluster = getParentAmbariCluster();
        for (Entity customService: Entities.descendants(ambariCluster, Predicates.instanceOf(CustomService.class), false)) {
            if (Entities.isManaged(customService)) {
                ((CustomService)customService).customizeService();
            }
        }

        builder.add(sudo("ambari-server setup -s"));

        newScript(CUSTOMIZING)
                .body.append(builder.build())
                .failOnNonZeroResultCode()
                .execute();
    }

    @Override
    public void launch() {
        newScript(LAUNCHING)
                .body.append(sudo("ambari-server start"))
                .failOnNonZeroResultCode()
                .execute();
    }

    private String copyToTmp(String extraStackDefinition) {
        String filename = extraStackDefinition.substring(extraStackDefinition.lastIndexOf('/') + 1);
        String destination = "/tmp/" + filename;
        getMachine().copyTo(resource.getResourceFromUrl(extraStackDefinition), destination);
        return destination;
    }

    private String getUnpackCommand(String tmpLocation) {
        if (tmpLocation.endsWith("tar")) {
            return sudo(format("tar xvf %s -C %s", tmpLocation, stackResourceLocation()));
        } else if (tmpLocation.endsWith("tar.gz")) {
            return sudo(format("tar zxvf %s -C %s", tmpLocation, stackResourceLocation()));
        } else if (tmpLocation.endsWith("zip")) {
            return BashCommands.chain(
                    BashCommands.INSTALL_UNZIP,
                    sudo(unzip(tmpLocation, stackResourceLocation())));
        }

        throw new IllegalStateException("Stack locations must be of type tar, tar.gz, zip");
    }

    private List<String> getExtraStackDefinitionUrls() {
        AmbariCluster parent = getParentAmbariCluster();
        return parent.getExtraStackDefinitionsUrls();
    }

    private String stackResourceLocation() {
        AmbariCluster parent = getParentAmbariCluster();
        String stackName = parent.getConfig(AmbariCluster.HADOOP_STACK_NAME);
        String stackVersion = parent.getConfig(AmbariCluster.HADOOP_STACK_VERSION);
        return format(RESOURCE_STACK_LOCATION, stackName, stackVersion);
    }

    private AmbariCluster getParentAmbariCluster() {
        Iterable<AmbariCluster> ancestors = Iterables.filter(Entities.ancestors(entity), AmbariCluster.class);
        return Iterables.getFirst(ancestors, null);
    }

}
