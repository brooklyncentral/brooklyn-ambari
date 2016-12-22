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

import java.util.Map;

import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.core.config.MapConfigKey;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

/**
 * Custom service
 */
@ImplementedBy(CustomServiceImpl.class)
public interface CustomService extends ExtraService {
    @SetFromFlag("customServiceName")
    ConfigKey<String> CUSTOM_SERVICE_NAME = ConfigKeys.newStringConfigKey("custom.service.name", "Name of the custom service");
    
    @SetFromFlag("customServiceConfig")
    ConfigKey<Map<String, Map>> CUSTOM_SERVICE_CONF = new MapConfigKey<Map>(Map.class, "custom.service.config", "Service Configuration as Map of maps");

    @SetFromFlag("serviceConfigTemplateUrl")
    ConfigKey<String> SERVICE_CONFIG_TEMPLATE_URL = ConfigKeys.newStringConfigKey(
            "service.config.templateUrl", "Template file (in freemarker format) for the custom service's service-config.xml file", 
            "classpath://io/brooklyn/ambari/service/custom/configuration/service-config.xml");

    @SetFromFlag("serviceConfigTemplateUrl")
    ConfigKey<String> SERVICE_SCRIPT_TEMPLATE_URL = ConfigKeys.newStringConfigKey(
            "service.script.templateUrl", "Template file (in freemarker format) for the custom service's python script file", 
            "classpath://io/brooklyn/ambari/service/custom/package/scripts/service_client.py");

    @SetFromFlag("serviceConfigTemplateUrl")
    ConfigKey<String> SERVICE_METAINFO_TEMPLATE_URL = ConfigKeys.newStringConfigKey(
            "service.metainfo.templateUrl", "Template file (in freemarker format) for the custom service's metainfo.xml file", 
            "classpath://io/brooklyn/ambari/service/custom/metainfo.xml");

    @SetFromFlag("customCcomponentName")
    ConfigKey<String> CUSTOM_COMPONENT_NAME = ConfigKeys.newStringConfigKey("custom.component.name", "Name of the componentName");

    void customizeService();
}