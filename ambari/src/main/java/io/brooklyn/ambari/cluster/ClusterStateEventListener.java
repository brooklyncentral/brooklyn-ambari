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
package io.brooklyn.ambari.cluster;

import org.apache.brooklyn.api.entity.EntityLocal;
import org.apache.brooklyn.api.sensor.SensorEvent;
import org.apache.brooklyn.api.sensor.SensorEventListener;
import org.apache.brooklyn.core.entity.lifecycle.ServiceStateLogic;
import org.apache.commons.lang3.StringUtils;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.service.ExtraServiceException;

public final class ClusterStateEventListener implements SensorEventListener<String> {

    private final AmbariCluster entity;

    public ClusterStateEventListener(AmbariCluster entity) {
        this.entity = entity;
    }

    @Override
    public void onEvent(SensorEvent<String> sensorEvent) {
        Boolean installed = entity.getAttribute(AmbariCluster.CLUSTER_SERVICES_INSTALLED);
        if (StringUtils.isNotBlank(sensorEvent.getValue()) && sensorEvent.getValue().equals("COMPLETED") && !Boolean.TRUE.equals(installed)) {
            try {
                entity.postDeployCluster();
            } catch (ExtraServiceException ex) {
                ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) entity, "ambari.extra.service", ex.getMessage());
                throw ex;
            }
        }
    }

}
