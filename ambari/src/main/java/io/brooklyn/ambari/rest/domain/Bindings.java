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

import brooklyn.util.collections.MutableList;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nonnull;
import java.util.List;

public class Bindings {

    @SerializedName("host_groups")
    private List<HostGroup> hostGroups;

    public Bindings() {
        this.hostGroups = MutableList.of();
    }

    @Nonnull
    public List<HostGroup> getHostGroups() {
        return hostGroups;
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

        public Bindings build() {
            Bindings bindings = new Bindings();
            bindings.hostGroups = this.hostGroups;

            return bindings;
        }
    }
}
