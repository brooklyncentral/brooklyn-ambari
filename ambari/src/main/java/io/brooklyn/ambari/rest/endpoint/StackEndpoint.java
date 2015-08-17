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

import io.brooklyn.ambari.rest.domain.RecommendationWrappers;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.Path;

import java.util.Map;

public interface StackEndpoint {

    @POST("/api/v1/stacks/{stack}/versions/{version}/recommendations")
    RecommendationWrappers getRecommendations(@Path("stack") String stack, @Path("version") String version, @Body Map body);

    @PUT("/api/v1/stacks/{stack}/versions/{version}/operating_systems/{os}/repositories/{repository}")
    Response updateStackRepository(@Path("stack") String stack, @Path("version") String version, @Path("os") String os, @Path("repository") String repository, @Body Map body);
}
