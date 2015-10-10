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

import java.util.List;
import java.util.Map;

import com.google.common.reflect.TypeToken;

import brooklyn.config.ConfigKey;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.util.flags.SetFromFlag;
import io.brooklyn.ambari.AmbariCluster;

/**
 * Defines an "extra service" for the Hadoop cluster. An entity implementing this interface will be assured to be called
 * at two particular times within the Ambari lifecycle:
 * <ul>
 *     <ol>{@link ExtraService#preClusterDeploy(AmbariCluster)} will be call once all the Ambari agents and servers have
 *     been installed, just before deploying a new hadoop cluster.</ol>
 *     <ol>{@link ExtraService#postClusterDeploy(AmbariCluster)} once the hadoop cluster has been deployed.</ol>
 * </ul>
 */
public interface ExtraService extends Entity {

    @SetFromFlag("bindTo")
    ConfigKey<String> BIND_TO = ConfigKeys.newStringConfigKey("bindTo", "Name of component which will be use to determine the host to install RANGER", AmbariCluster.SERVER_HOST_GROUP);

    @SetFromFlag("serviceName")
    ConfigKey<String> SERVICE_NAME = ConfigKeys.newStringConfigKey("serviceName", "Name of the Hadoop service, identified by Ambari");

    @SetFromFlag("componentNames")
    ConfigKey<List<String>> COMPONENT_NAMES = ConfigKeys.newConfigKey(new TypeToken<List<String>>() {}, "componentNames", "List of component names for this Hadoop service, identified by Ambari");

    /**
     * Returns the necessary configuration the extra services implementation need to pass to Ambari.
     *
     * @return a map of configuration.
     */
    Map<String, Map> getAmbariConfig();

    /**
     * Called just before the hadoop cluster will be deployed. If an error occurred during this phase, the subclasses
     * should throw an {@link ExtraServiceException} for the error to be propagated properly to the tree.
     *
     * @param ambariCluster the current Ambari cluster entity.
     */
    void preClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException;

    /**
     * Called just after the hadoop cluster has been deployed. If an error occurred during this phase, the subclasses
     * should throw an {@link ExtraServiceException} for the error to be propagated properly to the tree.
     *
     * @param ambariCluster the current Ambari cluster entity.
     */
    void postClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException;
}
