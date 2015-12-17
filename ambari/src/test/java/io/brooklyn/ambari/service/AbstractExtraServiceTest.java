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

import java.util.List;
import java.util.Map;

import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.entity.ImplementedBy;
import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableList;

import io.brooklyn.ambari.AmbariCluster;

public class AbstractExtraServiceTest extends BrooklynAppUnitTestSupport {

    @ImplementedBy(DummyExtraServiceImpl.class)
    public interface DummyExtraService extends ExtraService {

    }

    public static class DummyExtraServiceImpl extends AbstractExtraService implements DummyExtraService {

        public DummyExtraServiceImpl() {
        }

        @Override
        public Map<String, Map> getAmbariConfig(AmbariCluster ambariCluster) {
            return null;
        }

        @Override
        public void preClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException {

        }

        @Override
        public void postClusterDeploy(AmbariCluster ambariCluster) throws ExtraServiceException {

        }
    }

    @Test
    public void getComponentMappingsThrowExIfNoHost() {
        try {
            app.createAndManageChild(createDummyExtraServiceSpec(null, null, ImmutableList.of("MY_COMPONENT"))).getComponentMappings();
            fail();
        } catch (Throwable throwable) {
            Throwable rootCause = ExceptionUtils.getRootCause(throwable);

            assertEquals(NullPointerException.class, rootCause.getClass());
            assertEquals("Default host is required", rootCause.getMessage());
        }
    }

    @Test
    public void getComponentMappingsGetDefaultHostIfNoHost() {
        final String defaultHost = "my-default-host";
        final List<ExtraService.ComponentMapping> componentMappings = app.createAndManageChild(createDummyExtraServiceSpec(defaultHost, null, ImmutableList.of("MY_COMPONENT"))).getComponentMappings();

        for (ExtraService.ComponentMapping componentMapping : componentMappings) {
            assertEquals(defaultHost, componentMapping.getHost());
        }
    }

    @Test
    public void getComponentMappingsGetHostIfExists() {
        final String defaultHost = "my-default-host";
        final String host = "my-host";
        final List<ExtraService.ComponentMapping> componentMappings = app.createAndManageChild(createDummyExtraServiceSpec(defaultHost, null, ImmutableList.of("MY_COMPONENT|" + host))).getComponentMappings();

        for (ExtraService.ComponentMapping componentMapping : componentMappings) {
            assertEquals(host, componentMapping.getHost());
        }
    }

    private EntitySpec<DummyExtraService> createDummyExtraServiceSpec(String bindTo, String serviceName, List<String> componentsName) {
        return EntitySpec.create(DummyExtraService.class)
                .configure(DummyExtraService.BIND_TO, bindTo)
                .configure(DummyExtraService.SERVICE_NAME, serviceName)
                .configure(DummyExtraService.COMPONENT_NAMES, componentsName);
    }
}
