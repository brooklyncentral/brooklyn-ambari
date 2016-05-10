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

import java.util.List;
import java.util.Map;

public class AlertTargets {

    @SerializedName("href")
    private String href;

    @SerializedName("items")
    private List<Map> items;

    public List<Map> getItems() {
        return items;
    }

    public static class Builder {

        private String href;

        private List<Map> items;

        public Builder setHref(String href) {
            this.href = href;
            return this;
        }

        public Builder setItems(List<Map> items) {
            this.items = items;
            return this;
        }

        public AlertTargets build() {
            Preconditions.checkNotNull(this.href);
            Preconditions.checkNotNull(this.items);

            AlertTargets alertTargets = new AlertTargets();
            alertTargets.href = this.href;
            alertTargets.items = this.items;

            return alertTargets;
        }
    }
}
