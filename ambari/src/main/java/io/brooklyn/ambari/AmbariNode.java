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

import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.java.UsesJava;
import brooklyn.event.AttributeSensor;
import brooklyn.event.basic.Sensors;

/**
 * Represent a node managed by the {@link AmbariCluster}. Typically, this will be either a
 * {@link io.brooklyn.ambari.server.AmbariServer} or {@link io.brooklyn.ambari.agent.AmbariAgent}.
 */
public interface AmbariNode extends SoftwareProcess, UsesJava {

    AttributeSensor<String> FQDN = Sensors.newStringSensor(
            "entity.fqdn",
            "The fully qualified domain name of the entity.");

    /**
     * Sets the fully qualified domain name for this entity.
     *
     * @param fqdn the fully qualified domain name to set.
     */
    void setFqdn(String fqdn);

    /**
     * Returns the fully qualify domain name of this entity.
     *
     * @return a string representing the fully qualify domain name of this entity.
     */
    String getFqdn();
}
