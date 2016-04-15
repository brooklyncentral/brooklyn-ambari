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
import com.google.common.collect.ImmutableMap;
import com.google.gson.annotations.SerializedName;

import java.util.Map;

public class AlertTarget {

    @SerializedName("id")
    private String id;

    @SerializedName("name")
    private String name;

    @SerializedName("description")
    private String description;

    @SerializedName("global")
    private String global;

    @SerializedName("notification_type")
    private String notificationType;

    @SerializedName("alert_states")
    private String alertStates;

    @SerializedName("properties")
    private Map<String, ?> properties;

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public static class Builder {

        private String name;

        private String description;

        private String global;

        private String notificationType;

        private String alertStates;

        private Map<String, ?> properties;

        private String ambariDispatchRecipients;

        private String mailSmtpHost;

        private String mailSmtpPort;

        private String mailSmtpFrom;

        private String mailSmtpAuth;

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setDescription(String description) {
            this.description = description;
            return this;
        }

        public Builder setGlobal(String global) {
            this.global = global;
            return this;
        }

        public Builder setNotificationType(String notificationType) {
            this.notificationType = notificationType;
            return this;
        }

        public Builder setAlertSstates(String alertStates) {
            this.alertStates = alertStates;
            return this;
        }

        public Builder setProperties(Map<String, ?> properties) {
            this.properties = properties;
            return this;
        }

        public Builder setAmbariDispatchRecipients(String ambariDispatchRecipients) {
            this.ambariDispatchRecipients = ambariDispatchRecipients;
            return this;
        }

        public Builder setMailSmtpHost(String mailSmtpHost) {
            this.mailSmtpHost = mailSmtpHost;
            return this;
        }

        public Builder setMailSmtpPort(String mailSmtpPort) {
            this.mailSmtpPort = mailSmtpPort;
            return this;
        }

        public Builder setMailSmtpFrom(String mailSmtpFrom) {
            this.mailSmtpFrom = mailSmtpFrom;
            return this;
        }

        public Builder setMailSmtpAuth(String mailSmtpAuth) {
            this.mailSmtpAuth = mailSmtpAuth;
            return this;
        }

        public AlertTarget build() {
            Preconditions.checkNotNull(this.name);
            Preconditions.checkNotNull(this.ambariDispatchRecipients);

            AlertTarget alertTarget = new AlertTarget();
            alertTarget.name = this.name;
            alertTarget.description = this.description;
            alertTarget.global = this.global;
            alertTarget.notificationType = this.notificationType;
            alertTarget.alertStates = this.alertStates;
            alertTarget.properties = ImmutableMap.<String,Object>builder()
                    .put("ambari.dispatch.recipients", this.ambariDispatchRecipients)
                    .put("mail.smtp.host", this.mailSmtpHost)
                    .put("mail.smtp.port", this.mailSmtpPort)
                    .put("mail.smtp.from", this.mailSmtpFrom)
                    .put("mail.smtp.auth", this.mailSmtpAuth)
                    .build();

            return alertTarget;
        }
    }
}
