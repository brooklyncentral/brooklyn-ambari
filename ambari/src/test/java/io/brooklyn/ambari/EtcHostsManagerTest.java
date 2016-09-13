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

import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.entity.EntitySpec;
import org.apache.brooklyn.api.location.Location;
import org.apache.brooklyn.api.objs.BrooklynObject.TagSupport;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.test.BrooklynAppUnitTestSupport;
import org.apache.brooklyn.core.test.entity.TestApplication;
import org.apache.brooklyn.entity.stock.BasicEntity;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class EtcHostsManagerTest extends BrooklynAppUnitTestSupport {

    @Test
    public void testGatherIpHostnameMapping() {
        TestApplication app = TestApplication.Factory.newManagedInstanceForTests();
        List<Entity> entities = Lists.newArrayList(
                entityWithHostnameAndAddress(app, "a.example.com", "1.2.3.4"),
                entityWithHostnameAndAddress(app, "b.example.com", "1.2.4.3"),
                entityWithHostnameAndAddress(app, "c.example.com", "1.4.2.3"));
        Map<String, String> expected = ImmutableMap.of(
                "1.2.3.4", "a.example.com",
                "1.2.4.3", "b.example.com",
                "1.4.2.3", "c.example.com");
        assertEquals(EtcHostsManager.gatherIpHostnameMapping(entities, Attributes.ADDRESS), expected);
    }

    @Test
    public void testSetHostsOnMachinesDoesNothingIPIsNull() {
        TestApplication app = TestApplication.Factory.newManagedInstanceForTests();
        SshMachineLocation location = mock(SshMachineLocation.class);
        TagSupport tagSupport = mock(TestTagSupport.class);
        when(location.tags()).thenReturn(tagSupport);

        List<Entity> entities = Lists.newArrayList(
                entityWithCustomLocation(app, "a.example.com", null, location));

        EtcHostsManager.setHostsOnMachines(entities, Attributes.ADDRESS);

        verify(location, never()).execCommands(anyString(), anyList());
    }

    @Test
    public void testSetHostsOnMachinesExecuteCorrectCommands() {
        TestApplication app = TestApplication.Factory.newManagedInstanceForTests();
        SshMachineLocation location1 = mock(SshMachineLocation.class);
        SshMachineLocation location2 = mock(SshMachineLocation.class);
        TagSupport tagSupport = mock(TestTagSupport.class);
        when(location1.tags()).thenReturn(tagSupport);
        when(location2.tags()).thenReturn(tagSupport);

        List<Entity> entities = Lists.newArrayList(
                entityWithCustomLocation(app, "a.example.com", "1.2.3.4", location1),
                entityWithCustomLocation(app, "b.example.com", "1.2.4.3", location2));

        EtcHostsManager.setHostsOnMachines(entities, Attributes.ADDRESS);

        ArgumentCaptor<List> argument1 = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List> argument2 = ArgumentCaptor.forClass(List.class);

        verify(location1).execCommands(anyString(), argument1.capture());
        verify(location2).execCommands(anyString(), argument2.capture());

        assertTrue(argument1.getValue().toString().contains("a.example.com"));
        assertTrue(argument1.getValue().toString().contains("1.2.3.4"));
        assertTrue(argument2.getValue().toString().contains("b.example.com"));
        assertTrue(argument2.getValue().toString().contains("1.2.4.3"));
    }

    private Entity entityWithHostnameAndAddress(TestApplication app, String hostname, String address) {
        BasicEntity entity = app.createAndManageChild(EntitySpec.create(BasicEntity.class));
        entity.sensors().set(Attributes.HOSTNAME, hostname);
        entity.sensors().set(Attributes.ADDRESS, address);
        return entity;
    }

    private Entity entityWithCustomLocation(TestApplication app, String hostname, String address, Location location) {
        BasicEntity entity = app.createAndManageChild(EntitySpec.create(BasicEntity.class).location(location));
        entity.sensors().set(Attributes.HOSTNAME, hostname);
        entity.sensors().set(Attributes.ADDRESS, address);
        return entity;
    }

    private class TestTagSupport implements TagSupport {
        @Override
        public Set<Object> getTags() {
            return null;
        }

        @Override
        public boolean containsTag(Object tag) {
            return false;
        }

        @Override
        public boolean addTag(Object tag) {
            return false;
        }

        @Override
        public boolean addTags(Iterable<?> tags) {
            return false;
        }

        @Override
        public boolean removeTag(Object tag) {
            return false;
        }
    }
}
