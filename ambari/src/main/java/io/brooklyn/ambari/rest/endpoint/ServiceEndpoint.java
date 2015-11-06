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

package io.brooklyn.ambari.rest.endpoint;

import java.util.Map;

import io.brooklyn.ambari.rest.domain.Request;
import io.brooklyn.ambari.rest.domain.ServiceComponents;
import io.brooklyn.ambari.rest.domain.Services;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

public interface ServiceEndpoint {

    @GET("/api/v1/clusters/{cluster}")
    Services getServices(@Path("cluster") String cluster);

    @GET("/api/v1/clusters/{cluster}/services/{service}")
    ServiceComponents getServiceComponents(@Path("cluster") String cluster, @Path("service") String service);

    @POST("/api/v1/clusters/{cluster}/services/{service}")
    Response addService(@Path("cluster") String cluster, @Path("service") String service);

    @PUT("/api/v1/clusters/{cluster}/services/{service}")
    Request updateService(@Path("cluster") String cluster, @Path("service") String service, @Body Map body);

    @POST("/api/v1/clusters/{cluster}/services/{service}/components/{component}")
    Response createComponent(@Path("cluster") String cluster, @Path("service") String service, @Path("component") String component);
}
