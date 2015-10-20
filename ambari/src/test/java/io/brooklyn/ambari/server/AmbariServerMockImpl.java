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
package io.brooklyn.ambari.server;

import static org.mockito.Mockito.mock;

import java.util.Map;

import io.brooklyn.ambari.rest.AmbariApiException;
import io.brooklyn.ambari.rest.domain.RecommendationWrapper;
import io.brooklyn.ambari.rest.domain.Request;

public class AmbariServerMockImpl extends AmbariServerImpl implements AmbariServerMock {

    public String getClusterName() {
        return clusterName;
    }

    public String getBlueprintName() {
        return blueprintName;
    }

    public RecommendationWrapper getRecommendationWrapper() {
        return recommendationWrapper;
    }

    public Map getConfig() {
        return config;
    }

    private String clusterName;
    private String blueprintName;
    private RecommendationWrapper recommendationWrapper;
    private Map config;

    @Override
    public void waitForServiceUp() {
    }

    @Override
    public Request deployCluster(String clusterName, String blueprintName, RecommendationWrapper recommendationWrapper, Map config) throws AmbariApiException {
        this.clusterName = clusterName;
        this.blueprintName = blueprintName;
        this.recommendationWrapper = recommendationWrapper;
        this.config = config;
        return mock(Request.class);
    }
}
