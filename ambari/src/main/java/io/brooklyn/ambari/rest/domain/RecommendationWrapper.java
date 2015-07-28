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
import javax.annotation.Nullable;
import java.util.List;

public class RecommendationWrapper {

    @SerializedName("href")
    private String href;

    @SerializedName("hosts")
    private List<String> hosts;

    @SerializedName("services")
    private List<String> services;

    @SerializedName("Versions")
    private Stack stack;

    @SerializedName("recommendations")
    private Recommendation recommendation;

    public RecommendationWrapper() {
        this.hosts = MutableList.of();
        this.services = MutableList.of();
    }

    @Nullable
    public String getHref() {
        return href;
    }

    @Nonnull
    public List<String> getHosts() {
        return hosts;
    }

    @Nonnull
    public List<String> getServices() {
        return services;
    }

    @Nullable
    public Stack getStack() {
        return stack;
    }

    @Nullable
    public Recommendation getRecommendation() {
        return recommendation;
    }

    public static class Builder {

        private Stack stack;
        private Recommendation recommendation;

        public Builder setStack(Stack stack) {
            this.stack = stack;
            return this;
        }

        public Builder setRecommendation(Recommendation recommendation) {
            this.recommendation = recommendation;
            return this;
        }

        public RecommendationWrapper build() {
            Preconditions.checkNotNull(this.stack);
            Preconditions.checkNotNull(this.recommendation);

            RecommendationWrapper recommendationWrapper = new RecommendationWrapper();
            recommendationWrapper.stack = this.stack;
            recommendationWrapper.recommendation = this.recommendation;

            return recommendationWrapper;
        }
    }
}
