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

import static brooklyn.util.ssh.BashCommands.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.BrooklynTaskTags;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.basic.ServiceStateLogic;
import brooklyn.entity.effector.EffectorTasks;
import brooklyn.entity.software.SshEffectorTasks;
import brooklyn.management.Task;
import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.server.AmbariServer;

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
    public void preClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException {
        try {
            LOG.info("{} performing Ranger requirements on Ambari server", this);
            Task<List<?>> rangerServerRequirementsTasks = parallelListenerTask(ambariCluster.getAmbariServers(), new AmbariServerRequirementsFunction());
            Entities.submit(this, rangerServerRequirementsTasks).get();

            LOG.info("{} performing Ranger requirements on Ambari nodes with {} components installed", this, REQUIRES_JDBC_DRIVER);
            Task<List<?>> rangerAgentRequirementsTasks = parallelListenerTask(ambariCluster.getAmbariAgents(), new AmbariAgentRequirementsFunction(), REQUIRES_JDBC_DRIVER);
            Entities.submit(this, rangerAgentRequirementsTasks).get();

            LOG.info("{} performing Ranger requirements on the Ranger host", this);
            Task<List<?>> mysqlRequirementTasks = parallelListenerTask(ambariCluster.getAmbariAgents(), new MysqlRequirementsFunction(), REQUIRES_MYSQL_CLIENT);
            Entities.submit(this, mysqlRequirementTasks).get();
        } catch (ExecutionException|InterruptedException ex) {
            // If something failed, we propagate the exception.
            throw new ExtraServiceException(ex.getMessage());
        }
    }

    @Override
    public void postClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException {

    }

    abstract class BaseFunction<T extends Entity> implements Function<T, Void> {

        protected Void chechResult(Task<Integer> task, T node) {
            Entities.submit(node, task);
            task.blockUntilEnded();

            Integer result = task.getUnchecked();
            if (result != 0) {
                final String errorKey = "ranger.mysql";
                final String errorDescription = "Error initialising Ranger requirements";

                BrooklynTaskTags.WrappedStream stream = BrooklynTaskTags.stream(task, "stderr");
                final String errorMessage = String.format("%s: %s", errorDescription, stream != null ? stream.streamContents.get() : "Unexpected error");

                ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) node, errorKey, errorMessage);
                throw new RuntimeException(String.format("[Node %s] %s", node.getDisplayName(), errorMessage));
            }

            return null;
        }
    }

    class AmbariServerRequirementsFunction extends BaseFunction<AmbariServer> {
        @Nullable
        @Override
        public Void apply(AmbariServer ambariServer) {
            Task<Integer> sshTask = SshEffectorTasks
                    .ssh(
                            alternatives(installExecutable("mysql-connector-java"), installExecutable("libmysql-java")),
                            sudo("ambari-server setup --jdbc-db=mysql --jdbc-driver=/usr/share/java/mysql-connector-java.jar"))
                    .summary("Initialise Ranger requirements on " + ambariServer.getId())
                    .machine(EffectorTasks.getSshMachine(ambariServer))
                    .newTask()
                    .asTask();

            return chechResult(sshTask, ambariServer);
        }
    }

    class AmbariAgentRequirementsFunction extends BaseFunction<AmbariAgent> {
        @Nullable
        @Override
        public Void apply(AmbariAgent ambariAgent) {
            Task<Integer> sshTask = SshEffectorTasks
                    .ssh(alternatives(installExecutable("mysql-connector-java"), installExecutable("libmysql-java")))
                    .summary("Initialise Ranger requirements on " + ambariAgent.getId())
                    .machine(EffectorTasks.getSshMachine(ambariAgent))
                    .newTask()
                    .asTask();

            return chechResult(sshTask, ambariAgent);
        }
    }

    class MysqlRequirementsFunction extends BaseFunction<AmbariAgent> {
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

            return chechResult(sshTask, ambariAgent);

//            Integer result = sshTask.getUnchecked();
//            if (result != 0) {
//                ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) ambariAgent, "ranger.mysql", sshTask.getStatusSummary());
//                throw new ExtraServiceException("Error initialising Ranger requirement: " + result);
//            }
//
//            return null;
        }
    }
}
