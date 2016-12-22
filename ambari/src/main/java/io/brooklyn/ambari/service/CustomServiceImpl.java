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

import static java.lang.String.format;
import static org.apache.brooklyn.util.ssh.BashCommands.sudo;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.brooklyn.core.effector.ssh.SshEffectorTasks;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.core.task.DynamicTasks;
import org.apache.brooklyn.util.core.text.TemplateProcessor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;

import io.brooklyn.ambari.AmbariCluster;

/**
 * Custom service implementation. 
 */
public class CustomServiceImpl extends AbstractExtraService implements CustomService {

    @Override
    public Map<String, Map> getAmbariConfig(AmbariCluster ambariCluster) {
        return ImmutableMap.of();
    }

    @Override
    public void preClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException {

    }

    @Override
    public void postClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException {

    }

    @Override
    public void customizeService() {
        DynamicTasks.queue(
                SshEffectorTasks.put("/tmp/" + getServiceScriptFileName())
                                .contents(TemplateProcessor.processTemplateContents(
                                        ResourceUtils.create().getResourceAsString(config().get(CustomService.SERVICE_SCRIPT_TEMPLATE_URL)),
                                        this,
                                        ImmutableMap.<String,String>of())),
                SshEffectorTasks.put("/tmp/" + getMetaInfoXmlFileName())
                                .contents(TemplateProcessor.processTemplateContents(
                                        ResourceUtils.create().getResourceAsString(config().get(CustomService.SERVICE_METAINFO_TEMPLATE_URL)),
                                        this,
                                        ImmutableMap.<String,String>of())),
                SshEffectorTasks.ssh(
                        sudo(format("mkdir -p %s", getServiceDirectory())),
                        sudo(format("mv /tmp/%s %s", getMetaInfoXmlFileName(), getServiceDirectory())),
                        sudo(format("mkdir -p %s", getServiceScriptDirectory())),
                        sudo(format("mv /tmp/%s %s/",getServiceScriptFileName(), getServiceScriptDirectory())),
                        sudo(format("mkdir -p %s", getServiceConfigDirectory()))));

        Map<String, ?> serviceConf = getConfig(CustomService.CUSTOM_SERVICE_CONF);

        for (Entry<String, ?> entry: serviceConf.entrySet()) {
            DynamicTasks.queue(
                    SshEffectorTasks.put("/tmp/" + getServicePropertyFileName(entry.getKey()))
                                    .contents(prepareServiceConfigurationXmlContent(entry.getKey(), (Map) entry.getValue())),
                    SshEffectorTasks.ssh(
                            sudo(format("mv %s %s/", "/tmp/" + getServicePropertyFileName(entry.getKey()), getServiceConfigDirectory()))));
        }
        DynamicTasks.waitForLast();
    }

    private String prepareServiceConfigurationXmlContent(String config, Map<String, String> configMap) {
        
        return  TemplateProcessor.processTemplateContents(
                ResourceUtils.create().getResourceAsString(config().get(CustomService.SERVICE_CONFIG_TEMPLATE_URL)),
                this,
                ImmutableMap.<String,String>of("configKey", config));
    }

    public String getServicePropertyFileName(String propertyName) {
        return propertyName + ".xml";
    }

    public String getServiceScriptFileName() {
        return getConfig(CustomService.CUSTOM_COMPONENT_NAME).toLowerCase() + "_client.py";
    }
    
    public String getMetaInfoXmlFileName() {
        return "metainfo.xml";
    }
    
    public String getServiceName() {
        return getConfig(CustomService.CUSTOM_SERVICE_NAME);
    }
    
    public String getComponentName() {
        return getConfig(CustomService.CUSTOM_COMPONENT_NAME);
    }

    public List<String> getPropertyKeys(String propertyKey) {
        return ImmutableList.copyOf(Iterables.filter(getConfig(CustomService.CUSTOM_SERVICE_CONF).get(propertyKey).keySet(), String.class));
    }
    public String getPropertyValue(String propertyKey,String key) {
        return getConfig(CustomService.CUSTOM_SERVICE_CONF).get(propertyKey).get(key).toString();
    }

    private String getServiceDirectory() {
        return format("/var/lib/ambari-server/resources/stacks/HDP/%s/services/%s/", getConfig(AmbariCluster.HADOOP_STACK_VERSION), getConfig(CustomService.CUSTOM_SERVICE_NAME));
    }

    private String getServiceScriptDirectory() {
        return format("%s/package/scripts", getServiceDirectory());
    }

    private String getServiceConfigDirectory() {
        return format("%s/configuration", getServiceDirectory());
    }
}

