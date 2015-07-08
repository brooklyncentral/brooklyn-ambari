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
package io.brooklyn.ambari.rest;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import brooklyn.util.collections.Jsonya;
import io.brooklyn.ambari.rest.RecommendationResponse.Blueprint;

public class DefaultAmbariBluePrint implements Mappable {

    private final List<HostGroup> hostGroups;
    private final Map<String, String> baseBlueprints;
    private final List<? extends Map<?,?>> configurations;

    public static DefaultAmbariBluePrint createBlueprintFromRecommendation(Blueprint blueprint, Map<String, String> baseBlueprints, List<? extends Map<?,?>> configurations) {
        return new DefaultAmbariBluePrint(blueprint, baseBlueprints, configurations);
    }

    public static DefaultAmbariBluePrint createBlueprintFromConfig(AmbariConfig config, Map<String, String> baseBlueprints, List<? extends Map<?,?>> configurations) {
        return new DefaultAmbariBluePrint(config,baseBlueprints, configurations);
    }

    private DefaultAmbariBluePrint(AmbariConfig config, Map<String, String> baseBlueprints, List<? extends Map<?,?>> configurations) {
        this.baseBlueprints = baseBlueprints;
        this.configurations = configurations;
        this.hostGroups = new LinkedList<HostGroup>();
        Set<Map.Entry<String, List<String>>> entries = config.getHostGroups().entrySet();
        for (Map.Entry<String, List<String>> entry : entries) {
            hostGroups.add(new HostGroup(entry.getKey(), entry.getValue()));
        }
    }

    private DefaultAmbariBluePrint(Blueprint blueprint, Map<String, String> baseBlueprints, List<? extends Map<?,?>> configurations) {
        this.baseBlueprints = baseBlueprints;
        this.configurations = configurations;
        this.hostGroups = new LinkedList<HostGroup>();
        for (RecommendationResponse.HostGroup hostGroup : blueprint.host_groups) {
            hostGroups.add(new HostGroup(hostGroup));
        }
    }

    public String toJson() {
        return Jsonya.newInstance().add(this.asMap()).root().toString();
    }

    public Map<?,?> asMap() {
        return ImmutableMap.of("host_groups", Mappables.toMaps(hostGroups), "configurations", configurations, "Blueprints", baseBlueprints);
    }

    private static class HostGroup implements Mappable {

        private final String name;
        private final List<Component> components = new LinkedList<Component>();

        public HostGroup(RecommendationResponse.HostGroup hostGroup) {
            name = hostGroup.name;
            for (Map<?,?> component : hostGroup.components) {
                if (!component.get("name").equals("ZKFC")) {
                    components.add(new Component(component));
                }
            }
        }

        public HostGroup(String hostGroupName, List<String> hostGroupComponents) {
            name = hostGroupName;
            for (String component : hostGroupComponents) {
                components.add(new Component(ImmutableMap.of("name", component)));
            }
        }

        @Override
        public Map<?,?> asMap() {
            return ImmutableMap.of("name", name, "components", Mappables.toMaps(components));
        }
    }

    private static class Component implements Mappable {

        private final Map<?, ?> componentParams;

        public Component(Map<?, ?> component) {
            componentParams = ImmutableMap.copyOf(component);
        }

        @Override
        public Map<?,?> asMap() {
            return componentParams;
        }
    }
}
