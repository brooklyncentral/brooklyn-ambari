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

import java.util.List;
import javax.annotation.Nullable;

import org.apache.brooklyn.api.catalog.Catalog;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.api.sensor.AttributeSensor;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.sensor.Sensors;
import org.apache.brooklyn.util.core.flags.SetFromFlag;
import org.apache.brooklyn.util.javalang.JavaClassNames;

import com.google.common.reflect.TypeToken;

import io.brooklyn.ambari.AmbariNode;

@Catalog(name="Ambari Agent", description="Ambari Agent: part of an ambari cluster that runs on each node that will form part of the Hadoop cluster")
@ImplementedBy(AmbariAgentImpl.class)
public interface AmbariAgent extends AmbariNode {

    @SetFromFlag("configFileUrl")
    ConfigKey<String> TEMPLATE_CONFIGURATION_URL = ConfigKeys.newConfigKey(
            "ambari.templateConfigurationUrl", "Template file (in freemarker format) for the ambari-agent.ini file",
            JavaClassNames.resolveClasspathUrl(AmbariAgent.class, "ambari-agent.ini"));

    @SetFromFlag("ambariServerFQDN")
    ConfigKey<String> AMBARI_SERVER_FQDN = ConfigKeys.newStringConfigKey(
            "ambari.server.fqdn", "Fully Qualified Domain Name of ambari server that agent should register to");


    AttributeSensor<List<String>> COMPONENTS = Sensors.newSensor(
            new TypeToken<List<String>>() {},
            "hadoop.components",
            "List of installed components");

    /**
     * Set the list of {@link AmbariAgent#COMPONENTS} that will be / are installed on this node.
     *
     * @param components the list of component to set.
     */
    void setComponents(@Nullable List<String> components);

    /**
     * Returns the list of {@link AmbariAgent#COMPONENTS} that will be / are installed on this node.
     *
     * @return a list of string that represents the components to be installed.
     */
    @Nullable
    List<String> getComponents();
}
