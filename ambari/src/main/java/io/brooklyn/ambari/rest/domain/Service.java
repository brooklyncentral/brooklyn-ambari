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

import com.google.gson.annotations.SerializedName;

import javax.annotation.Nullable;

public class Service {

    @SerializedName("href")
    private String href;

    @SerializedName("ServiceInfo")
    private ServiceInfo serviceInfo;

    @Nullable
    public String getHref() {
        return href;
    }

    @Nullable
    public ServiceInfo getServiceInfo() {
        return serviceInfo;
    }

    public static class ServiceInfo {

        @SerializedName("cluster_name")
        private String cluster;

        @SerializedName("service_name")
        private String service;

        @Nullable
        public String getCluster() {
            return cluster;
        }

        @Nullable
        public String getService() {
            return service;
        }
    }
}
