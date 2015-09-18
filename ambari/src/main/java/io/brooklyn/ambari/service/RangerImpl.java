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

package io.brooklyn.ambari.service;

import static brooklyn.util.ssh.BashCommands.alternatives;
import static brooklyn.util.ssh.BashCommands.installExecutable;
import static brooklyn.util.ssh.BashCommands.sudo;

import brooklyn.entity.basic.Entities;
import brooklyn.entity.effector.EffectorTasks;
import brooklyn.entity.software.SshEffectorTasks;
import brooklyn.management.Task;
import brooklyn.util.ssh.BashCommands;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.server.AmbariServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

/**
 * Ranger hadoop service implementation. This will install and preform all the prerequisites needed for Ranger.
 */
public class RangerImpl extends AbstractExtraService implements Ranger {

    private static final Logger LOG = LoggerFactory.getLogger(RangerImpl.class);

    private static final String DB_HOST = "localhost";

    private static final List<String> REQUIRES_JDBC_DRIVER = Lists.newArrayList("NAMENODE", "HBASE_MASTER", "HBASE_REGIONSERVER");
    private static final List<String> REQUIRES_MYSQL_CLIENT = Lists.newArrayList("RANGER_ADMIN");

    @Override
    public Map<String, Map> getAmbariConfig() {
        return ImmutableMap.<String, Map>builder()
                .put("admin-properties", ImmutableMap.builder()
                        .put("db_host", DB_HOST)
                        .put("db_root_user", getConfig(DB_USER))
                        .put("db_root_password", getConfig(DB_PASSWORD))
                        .build())
                .build();
    }

    @Override
    public void preClusterDeploy(AmbariCluster ambariCluster) {
        LOG.info("{} performing Ranger requirements on Ambari server", this);
        Task<List<?>> rangerServerRequirementsTasks = parallelListenerTask(ambariCluster.getAmbariServers(), new AmbariServerRequirementsFunction());
        Entities.submit(this, rangerServerRequirementsTasks).getUnchecked();

        LOG.info("{} performing Ranger requirements on Ambari nodes with {} components installed", this, REQUIRES_JDBC_DRIVER);
        Task<List<?>> rangerAgentRequirementsTasks = parallelListenerTask(ambariCluster.getAmbariAgents(), new AmbariAgentRequirementsFunction(), REQUIRES_JDBC_DRIVER);
        Entities.submit(this, rangerAgentRequirementsTasks).getUnchecked();

        LOG.info("{} performing MySQL requirements on the Ranger host", this);
        Task<List<?>> mysqlRequirementTasks = parallelListenerTask(ambariCluster.getAmbariAgents(), new MysqlRequirementsFunction(), REQUIRES_MYSQL_CLIENT);
        Entities.submit(this, mysqlRequirementTasks).getUnchecked();
    }

    @Override
    public void postClusterDeploy(AmbariCluster ambariCluster) {

    }

    private class AmbariServerRequirementsFunction implements Function<AmbariServer, Void> {
        @Nullable
        @Override
        public Void apply(AmbariServer ambariServer) {
            Task<Integer> sshTask = SshEffectorTasks
                    .ssh(
                            installExecutable("mysql-connector-java"),
                            sudo("ambari-server setup --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar"))
                    .summary("Initialise Ranger requirements on " + ambariServer.getId())
                    .machine(EffectorTasks.getSshMachine(ambariServer))
                    .newTask()
                    .asTask();
            Entities.submit(ambariServer, sshTask);
            sshTask.blockUntilEnded();
            Integer result = sshTask.getUnchecked();
            if (result != 0) {
                throw new RuntimeException("Non-zero result code indicating failure when initialising Ranger requirements: " + result);
            }

            return null;
        }
    }

    private class AmbariAgentRequirementsFunction implements Function<AmbariAgent, Void> {
        @Nullable
        @Override
        public Void apply(AmbariAgent ambariAgent) {
            Task<Integer> sshTask = SshEffectorTasks
                    .ssh(installExecutable("mysql-connector-java"))
                    .summary("Initialise Ranger requirements on " + ambariAgent.getId())
                    .machine(EffectorTasks.getSshMachine(ambariAgent))
                    .newTask()
                    .asTask();
            Entities.submit(ambariAgent, sshTask);
            sshTask.blockUntilEnded();
            Integer result = sshTask.getUnchecked();
            if (result != 0) {
                throw new RuntimeException("Non-zero result code indicating failure when initialising Ranger requirements: " + result);
            }

            return null;
        }
    }

    private class MysqlRequirementsFunction implements Function<AmbariAgent, Void> {
        @Nullable
        @Override
        public Void apply(AmbariAgent ambariAgent) {
            Task<Integer> sshTask = SshEffectorTasks
                    .ssh(
                            installExecutable("mysql-server"),
                            alternatives(installExecutable("mysql"), installExecutable("mysql-client")),
                            alternatives(sudo("service mysqld restart"), sudo("service mysql restart")),
                            String.format("mysql -u root -e 'create user `%s`@`%s` identified by \"%s\";'", getConfig(DB_USER), DB_HOST, getConfig(DB_PASSWORD)),
                            String.format("mysql -u root -e 'grant all privileges on *.* to `%s`@`%s` identified by \"%s\" with grant option; flush privileges;'", getConfig(DB_USER), DB_HOST, getConfig(DB_PASSWORD)))
                    .summary("Initialise MySQL requirements on " + ambariAgent.getId())
                    .machine(EffectorTasks.getSshMachine(ambariAgent))
                    .newTask()
                    .asTask();
            Entities.submit(ambariAgent, sshTask);
            sshTask.blockUntilEnded();
            Integer result = sshTask.getUnchecked();
            if (result != 0) {
                throw new RuntimeException("Non-zero result code indicating failure when initialising MySQL requirement for Ranger: " + result);
            }

            return null;
        }
    }
}
