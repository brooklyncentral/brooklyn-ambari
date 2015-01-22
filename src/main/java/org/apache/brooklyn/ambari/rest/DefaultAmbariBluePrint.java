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
package org.apache.brooklyn.ambari.rest;

import brooklyn.util.collections.Jsonya;
import brooklyn.util.collections.MutableMap;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.brooklyn.ambari.rest.RecommendationResponse.Resource.Recommendations.Blueprint;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by duncangrant on 21/01/15.
 */
public class DefaultAmbariBluePrint implements AsMap {

    private final List<HostGroup> host_groups = new LinkedList<HostGroup>();
    private ImmutableMap<String, String> baseBlueprints = ImmutableMap.of("stack_name", "HDP", "stack_version", "2.2");
    private ImmutableMap<Object, Object> configurations = ImmutableMap.of();

    private DefaultAmbariBluePrint(Blueprint blueprint) {
        for (Blueprint.HostGroup hostGroup : blueprint.host_groups) {
            host_groups.add(new HostGroup(hostGroup));
        }
    }

    public static DefaultAmbariBluePrint createBlueprintFromRecommendation(Blueprint blueprint) {
        return new DefaultAmbariBluePrint(blueprint);
    }

    public String toJson() {
        return Jsonya.newInstance().add(this.asMap()).root().toString();
    }

    public Map asMap() {
        return ImmutableMap.of("host_groups", toMaps(host_groups), "configurations", configurations, "Blueprints", baseBlueprints);
    }

    private static List<Map> toMaps(List<? extends AsMap> host_groups) {
        LinkedList<Map> maps = new LinkedList<Map>();
        for (AsMap host_group : host_groups) {
            maps.add(host_group.asMap());
        }
        return ImmutableList.<Map>copyOf(maps);
    }

    private static class HostGroup implements AsMap {

        private final String name;

        private final List<Component> components = new LinkedList<Component>();

        public HostGroup(Blueprint.HostGroup hostGroup) {
            name = hostGroup.name;
            for (Map component : hostGroup.components) {
                components.add(new Component(component));
            }
        }

        @Override
        public Map asMap() {
            return ImmutableMap.of("name", name, "components", toMaps(components));
        }
    }

    private static class Component implements AsMap {

        private final Map<String, String> componentParams;

        public Component(Map component) {
            componentParams = ImmutableMap.<String, String>copyOf(component);
        }

        @Override
        public Map asMap() {
            return ImmutableMap.copyOf(componentParams);
        }
    }

    public static final Function<Object, Object> TRANSLATOR = new Function<Object, Object>() {
        @Nullable
        @Override
        public Object apply(Object object) {
            DefaultAmbariBluePrint defaultAmbariBluePrint = (DefaultAmbariBluePrint) object;
            return defaultAmbariBluePrint.asMap();
        }
    };
}
