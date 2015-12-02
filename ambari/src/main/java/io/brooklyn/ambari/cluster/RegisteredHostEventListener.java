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

import java.util.List;

import org.apache.brooklyn.api.effector.Effector;
import org.apache.brooklyn.api.entity.EntityLocal;
import org.apache.brooklyn.api.sensor.SensorEvent;
import org.apache.brooklyn.api.sensor.SensorEventListener;
import org.apache.brooklyn.core.effector.EffectorBody;
import org.apache.brooklyn.core.effector.Effectors;
import org.apache.brooklyn.core.effector.MethodEffector;
import org.apache.brooklyn.core.entity.lifecycle.ServiceStateLogic;
import org.apache.brooklyn.util.core.config.ConfigBag;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.AmbariClusterImpl;
import io.brooklyn.ambari.rest.AmbariApiException;
import io.brooklyn.ambari.service.ExtraServiceException;

public final class RegisteredHostEventListener implements SensorEventListener<List<String>> {

    private final AmbariClusterImpl entity;
    private Boolean pauseOnDeployment;

    public RegisteredHostEventListener(AmbariClusterImpl entity, Boolean pauseOnDeployment) {
        this.entity = entity;
        this.pauseOnDeployment = pauseOnDeployment;
    }

    @Override
    public void onEvent(SensorEvent<List<String>> event) {
        List<String> hosts = event.getValue();
        Integer initialClusterSize = entity.getAttribute(AmbariCluster.EXPECTED_AGENTS);
        Boolean initialised = entity.getAttribute(AmbariCluster.CLUSTER_SERVICES_INITIALISE_CALLED);
        if (hosts != null && hosts.size() == initialClusterSize && !Boolean.TRUE.equals(initialised)) {
            try {
                if (pauseOnDeployment) {
                    entity.getMutableEntityType().addEffector(createDeployClusterEffector());
                } else {
                    entity.deployCluster();
                }
            } catch (AmbariApiException ex) {
                ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) entity, "ambari.api", ex.getMessage());
                throw ex;
            } catch (ExtraServiceException ex) {
                ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) entity, "ambari.extra.service", ex.getMessage());
                throw ex;
            }
        }
    }

    private Effector<Void> createDeployClusterEffector() {
        return Effectors
                .effector(
                        new MethodEffector<Void>(
                                AmbariCluster.class,
                                "deployCluster"))
                .impl(
                        new EffectorBody<Void>() {
                            @Override
                            public Void call(ConfigBag configBag) {
                                entity.deployCluster();
                                return null;
                            }
                        })
                .build();
    }

}
