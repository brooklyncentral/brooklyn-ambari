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
import com.google.gson.annotations.SerializedName;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

public class ServiceComponents {

    @SerializedName("href")
    private String href;

    @SerializedName("ServiceInfo")
    private Service.ServiceInfo service;

    @SerializedName("components")
    private List<ServiceComponent> components;

    public ServiceComponents() {
        this.components = MutableList.of();

    }

    @Nullable
    public String getHref() {
        return href;
    }

    @Nullable
    public Service.ServiceInfo getService() {
        return service;
    }

    @Nonnull
    public List<ServiceComponent> getComponents() {
        return components;
    }
}
