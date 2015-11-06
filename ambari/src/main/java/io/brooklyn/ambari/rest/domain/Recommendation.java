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

import javax.annotation.Nullable;

import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;

public class Recommendation {

    @SerializedName("blueprint")
    private Blueprint blueprint;

    @SerializedName("blueprint_cluster_binding")
    private Bindings bindings;

    @Nullable
    public Blueprint getBlueprint() {
        return blueprint;
    }

    @Nullable
    public Bindings getBindings() {
        return bindings;
    }

    public static class Builder {

        private Blueprint blueprint;
        private Bindings bindings;

        public Builder setBlueprint(Blueprint blueprint) {
            this.blueprint = blueprint;
            return this;
        }

        public Builder setBindings(Bindings bindings) {
            this.bindings = bindings;
            return this;
        }

        public Recommendation build() {
            Preconditions.checkNotNull(this.blueprint);
            Preconditions.checkNotNull(this.bindings);

            Recommendation recommendation = new Recommendation();
            recommendation.blueprint = blueprint;
            recommendation.bindings = bindings;

            return recommendation;
        }
    }
}
