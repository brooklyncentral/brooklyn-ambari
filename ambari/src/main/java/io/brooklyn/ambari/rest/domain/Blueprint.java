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
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.brooklyn.util.collections.MutableList;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

public class Blueprint {

    @SerializedName("href")
    private String href;

    @SerializedName("configurations")
    private List<Configuration> configurations;

    @SerializedName("host_groups")
    private List<HostGroup> hostGroups;

    @SerializedName("Blueprints")
    private BlueprintInfo blueprintInfo;

    public Blueprint() {
        this.configurations = MutableList.of();
        this.hostGroups = MutableList.of();
    }

    @Nullable
    public String getHref() {
        return href;
    }

    @Nonnull
    public List<Configuration> getConfigurations() {
        return configurations;
    }

    @Nonnull
    public List<HostGroup> getHostGroups() {
        return hostGroups;
    }

    @Nullable
    public BlueprintInfo getBlueprintInfo() {
        return blueprintInfo;
    }

    public static class BlueprintInfo {

        @SerializedName("blueprint_name")
        private String name;

        @SerializedName("stack_name")
        private String stackName;

        @SerializedName("stack_version")
        private String stackVersion;

        @Nullable
        public String getName() {
            return name;
        }

        @Nullable
        public String getStackName() {
            return stackName;
        }

        @Nullable
        public String getStackVersion() {
            return stackVersion;
        }
    }

    public static class Builder {

        private final List<HostGroup> hostGroups;

        public Builder() {
            this.hostGroups = MutableList.of();
        }

        public Builder addHostGroup(HostGroup hostGroup) {
            Preconditions.checkNotNull(hostGroup);
            this.hostGroups.add(hostGroup);
            return this;
        }

        public Blueprint build() {
            Blueprint blueprint = new Blueprint();
            blueprint.hostGroups = this.hostGroups;

            return blueprint;
        }
    }
}
