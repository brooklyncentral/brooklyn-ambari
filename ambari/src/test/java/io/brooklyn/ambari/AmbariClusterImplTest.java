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

import static brooklyn.entity.basic.Entities.descendants;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.getFirst;
import static com.google.common.collect.Iterators.getNext;
import static org.testng.Assert.assertTrue;

import java.util.Collection;
import java.util.List;

import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;

import brooklyn.entity.BrooklynAppUnitTestSupport;
import brooklyn.entity.Entity;
import brooklyn.entity.proxying.EntitySpec;
import io.brooklyn.ambari.hostgroup.AmbariHostGroup;
import io.brooklyn.ambari.rest.domain.HostComponent;
import io.brooklyn.ambari.rest.domain.HostGroup;
import io.brooklyn.ambari.server.AmbariServerMock;
import io.brooklyn.ambari.service.ExtraService;
import io.brooklyn.ambari.service.Ranger;

public class AmbariClusterImplTest extends BrooklynAppUnitTestSupport {

    @Test
    public void testExtraServicesComponentMapping() {
        EntitySpec<? extends ExtraService> extraServiceEntitySpec =
                createExtraServiceEntitySpecWithComponents("Monkey", "Ape|DataNode");
        app.createAndManageChild(createClusterSpecWithExtraService(extraServiceEntitySpec)).deployCluster();

        assertHostGroupContainsComponent("NameNode", "Monkey");
        assertHostGroupContainsComponent("DataNode", "Ape");
    }

    private EntitySpec<? extends ExtraService> createExtraServiceEntitySpecWithComponents(String... components) {
        EntitySpec<? extends ExtraService> extraServiceEntitySpec =
                EntitySpec.create(Ranger.class)
                        .configure(ExtraService.BIND_TO.getName(), "NameNode")
                        .configure(ExtraService.COMPONENT_NAMES.getName(), ImmutableList.copyOf(components));
        return extraServiceEntitySpec;
    }

    private EntitySpec<AmbariCluster> createClusterSpecWithExtraService(EntitySpec<? extends ExtraService> extraServiceEntitySpec) {
        return EntitySpec.create(AmbariCluster.class)
                .configure(
                        AmbariCluster.EXTRA_HADOOP_SERVICES,
                        ImmutableList.<EntitySpec<? extends ExtraService>>of(extraServiceEntitySpec))
                .configure(
                        AmbariCluster.SERVER_SPEC,
                        EntitySpec.create(AmbariServerMock.class)
                )
                .child(getNode("NameNode"))
                .child(getNode("DataNode"));
    }

    private void assertHostGroupContainsComponent(String node, String componentName) {
        AmbariServerMock ambariServerMock = getAmbariServerMock();
        List<HostGroup> hostGroups = getHostGroups(ambariServerMock);
        HostGroup hostGroup = getFirst(filter(hostGroups, isNode(node)), null);
        HostComponent component = getFirst(filter(hostGroup.getComponents(), componentNamed(componentName)), null);
        assertTrue(component != null, componentName + " not mapped to " + node);
    }

    private AmbariServerMock getAmbariServerMock() {
        Collection<Entity> children = app.getChildren();
        return getFirst(descendants(getNext(children.iterator(), null), AmbariServerMock.class), null);
    }

    private List<HostGroup> getHostGroups(AmbariServerMock ambariServerMock) {
        return ambariServerMock.getRecommendationWrapper().getRecommendation().getBlueprint().getHostGroups();
    }

    private Predicate<HostComponent> componentNamed(final String componentName) {
        return new Predicate<HostComponent>() {
            @Override
            public boolean apply(HostComponent hostComponent) {
                return componentName.equals(hostComponent.getName());
            }
        };
    }

    private Predicate<HostGroup> isNode(final String node) {
        return new Predicate<HostGroup>() {
            @Override
            public boolean apply(HostGroup hostGroup) {
                return node.equals(hostGroup.getName());
            }
        };
    }

    private EntitySpec<AmbariHostGroup> getNode(String nameNode) {
        return EntitySpec
                .create(AmbariHostGroup.class)
                .displayName(nameNode)
                .configure(
                        AmbariHostGroup.HADOOP_COMPONENTS,
                        ImmutableList.of("hdfs"));
    }

}