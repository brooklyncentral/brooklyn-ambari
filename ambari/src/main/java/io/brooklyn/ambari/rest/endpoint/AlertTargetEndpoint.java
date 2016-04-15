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

import io.brooklyn.ambari.rest.domain.AlertTarget;
import io.brooklyn.ambari.rest.domain.AlertTargets;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.PUT;
import retrofit.http.DELETE;
import retrofit.http.Path;

import java.util.Map;

public interface AlertTargetEndpoint {

    @POST("/api/v1/alert_targets")
    AlertTarget createAlertNotification(@Body Map<?,?> alertNotificationRequest);

    @GET("/api/v1/alert_targets")
    AlertTargets listAlertNotifications();

    @PUT("/api/v1/alert_targets/{id}")
    AlertTarget editAlertNotification(@Path("id") Integer id, @Body Map<?,?> alertNotificationRequest);

    @DELETE("/api/v1/alert_targets/{id}")
    AlertTarget deleteAlertNotification(@Path("id") Integer id);
}
