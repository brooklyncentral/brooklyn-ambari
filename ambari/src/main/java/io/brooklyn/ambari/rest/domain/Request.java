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

public class Request {

    @SerializedName("href")
    private String href;

    @SerializedName("Requests")
    private RequestInfo requestInfo;

    @Nullable
    public String getHref() {
        return href;
    }

    @Nullable
    public RequestInfo getRequestInfo() {
        return requestInfo;
    }

    public static class RequestInfo {

        @SerializedName("cluster_name")
        private String cluster;

        @SerializedName("request_context")
        private String context;

        @SerializedName("request_status")
        private String status;

        @SerializedName("id")
        private int id;

        @Nullable
        public String getCluster() {
            return cluster;
        }

        @Nullable
        public String getContext() {
            return context;
        }

        @Nullable
        public String getStatus() {
            return status;
        }

        public int getId() {
            return id;
        }
    }
}
