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

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class HostComponent {

    @SerializedName("href")
    private String href;

    @SerializedName("name")
    private String name;

    @SerializedName("HostRoles")
    private HostRoles hostRoles;

    @Nullable
    public String getHref() {
        return href;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public HostRoles getHostRoles() {
        return hostRoles;
    }

    public static class HostRoles {

        @SerializedName("cluster_name")
        private String cluster;

        @SerializedName("host_name")
        private String host;

        @SerializedName("component_name")
        private String component;

        @SerializedName("state")
        private String state;

        @Nullable
        public String getCluster() {
            return cluster;
        }

        @Nullable
        public String getHost() {
            return host;
        }

        @Nullable
        public String getComponent() {
            return component;
        }

        @Nullable
        public String getState() {
            return state;
        }
    }

    public static class Builder {

        private String name;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public HostComponent build() {
            Preconditions.checkNotNull(this.name);

            HostComponent hostComponent = new HostComponent();
            hostComponent.name = this.name;

            return hostComponent;
        }
    }
}
