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

package io.brooklyn.ambari.rest.domain;

import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.brooklyn.util.collections.MutableList;
import org.apache.brooklyn.util.collections.MutableMap;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

public class HostGroup {

    @SerializedName("name")
    private String name;

    @SerializedName("configuration")
    private List<Configuration> configurations;

    @SerializedName("components")
    private List<HostComponent> components;

    @SerializedName("hosts")
    private List<Map<String, String>> hosts;

    public HostGroup() {
        this.configurations = MutableList.of();
        this.components = MutableList.of();
        this.hosts = MutableList.of();
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nonnull
    public List<Configuration> getConfigurations() {
        return configurations;
    }

    @Nonnull
    public List<HostComponent> getComponents() {
        return components;
    }

    @Nonnull
    public List<Map<String, String>> getHosts() {
        return hosts;
    }

    public static class Builder {

        private String name;
        private final List<String> components;
        private final List<String> hosts;

        public Builder() {
            this.components = MutableList.of();
            this.hosts = MutableList.of();
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder addComponent(String component) {
            Preconditions.checkNotNull(component);
            this.components.add(component);
            return this;
        }

        public Builder addComponents(List<String> component) {
            Preconditions.checkNotNull(component);
            this.components.addAll(component);
            return this;
        }

        public Builder addHost(String host) {
            Preconditions.checkNotNull(host);
            this.hosts.add(host);
            return this;
        }

        public Builder addHosts(List<String> hosts) {
            Preconditions.checkNotNull(hosts);
            this.hosts.addAll(hosts);
            return this;
        }

        public HostGroup build() {
            Preconditions.checkNotNull(this.name);

            HostGroup hostGroup = new HostGroup();
            hostGroup.name = this.name;
            for (String component : components) {
                hostGroup.components.add(new HostComponent.Builder().setName(component).build());
            }
            for (String host : hosts) {
                hostGroup.hosts.add(MutableMap.of("fqdn", host));
            }

            return hostGroup;
        }
    }
}
