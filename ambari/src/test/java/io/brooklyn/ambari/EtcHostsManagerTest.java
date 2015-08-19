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

import brooklyn.entity.BrooklynAppUnitTestSupport;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.BasicEntity;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.location.Location;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.test.entity.TestApplication;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

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
        ((EntityLocal) entity).setAttribute(Attributes.HOSTNAME, hostname);
        ((EntityLocal) entity).setAttribute(Attributes.ADDRESS, address);
        return entity;
    }

    private Entity entityWithCustomLocation(TestApplication app, String hostname, String address, Location location) {
        BasicEntity entity = app.createAndManageChild(EntitySpec.create(BasicEntity.class).location(location));
        ((EntityLocal) entity).setAttribute(Attributes.HOSTNAME, hostname);
        ((EntityLocal) entity).setAttribute(Attributes.ADDRESS, address);
        return entity;
    }
}
