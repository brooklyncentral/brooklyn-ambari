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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.core.effector.EffectorTasks;
import org.apache.brooklyn.core.effector.ssh.SshEffectorTasks;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.location.SimulatedLocation;
import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.core.test.entity.TestEntity;
import org.apache.brooklyn.util.core.ResourceUtils;
import org.apache.brooklyn.util.core.text.TemplateProcessor;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import io.brooklyn.ambari.AmbariCluster;

public class CustomServiceTest {

//    private List<SimulatedLocation> locs;
//    private TestApplication app;
//    private CustomService entity;
//    
//    @BeforeMethod(alwaysRun=true)
//    public void setUp() {
//        locs = ImmutableList.of(new SimulatedLocation());
//        app = TestApplication.Factory.newManagedInstanceForTests();
//        entity = app.createAndManageChild(EntitySpec.create(CustomService.class));
//    }
//
//    @AfterMethod(alwaysRun=true)
//    public void tearDown() throws Exception {
//        if (app != null) Entities.destroyAll(app.getManagementContext());
//    }
//    
//    @Test
//    public void testServiceConfigurationXmlContent() throws Exception {
//        String yamlConfig = "httpfs-site:\n" +
//                      "  httpfs.proxyuser.hue: '*'\n" +
//                      "  httpfs.proxyuser.hue.groups: '*'\n" +
//                      "test-site:\n" +
//                      "  test.proxyuser.hue: '*'\n" +
//                      "  test.proxyuser.hue.groups: '*'\n";
//        Yaml yaml = new Yaml();
//        Map map = (Map) yaml.load(yamlConfig);
//        
//        entity.config().set(CustomService.SERVICE_CONF, map);
//        Map<String, ?> serviceConf = entity.config().get(CustomService.SERVICE_CONF);
//        
//        StringBuffer parsedConfig = new StringBuffer();
//        for (Entry<String, ?> entry: serviceConf.entrySet()) {
//            parsedConfig.append(prepareServiceConfigurationXmlContent(entity, entry.getKey()));
//        }
//        
//
//    }
//
//    private String prepareServiceConfigurationXmlContent(Entity entity, String configEntry) throws Exception {
//        Configuration cfg = new Configuration();
//        Template template = cfg.getTemplate( ResourceUtils.create().getResourceAsString(entity.config().get(CustomService.SERVICE_CONFIG_TEMPLATE_URL)));
//        StringWriter out = new StringWriter();
//        template.process(template, out);
//        String str = out.getBuffer().toString();
////        return TemplateProcessor.processTemplateContents(
////                ResourceUtils.create().getResourceAsString(entity.config().get(CustomService.SERVICE_CONFIG_TEMPLATE_URL)),
////                ImmutableMap.of("configEntry", configEntry));
//        return str;
//    }
}
