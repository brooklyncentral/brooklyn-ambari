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
package org.apache.brooklyn.ambari.testdoubles;

import brooklyn.config.ConfigKey;
import brooklyn.entity.*;
import brooklyn.entity.annotation.EffectorParam;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.event.AttributeSensor;
import brooklyn.location.Location;
import brooklyn.management.Task;
import brooklyn.policy.Enricher;
import brooklyn.policy.EnricherSpec;
import brooklyn.policy.Policy;
import brooklyn.policy.PolicySpec;
import brooklyn.util.guava.Maybe;
import org.apache.brooklyn.ambari.server.AmbariServer;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by duncangrant on 08/01/15.
 */
public class AmbariServerStub implements AmbariServer {
    @Override
    public void waitForServiceUp() {

    }

    @Override
    public void createCluster(@EffectorParam(name = "Cluster name") String cluster) {
        throw new RuntimeException("Should not have tried to add cluster");
    }

    @Override
    public void addHostToCluster(@EffectorParam(name = "Cluster name") String cluster, @EffectorParam(name = "Host FQDN") String hostName) {
        throw new RuntimeException("Should not have tried to add host to cluster");
    }

    @Override
    public void addServiceToCluster(@EffectorParam(name = "Cluster name") String cluster, @EffectorParam(name = "Service") String service) {
        throw new RuntimeException("Should not have tried to add host to cluster");
    }

    @Override
    public void addComponentToCluster(@EffectorParam(name = "Cluster name") String cluster, @EffectorParam(name = "Service name") String service, @EffectorParam(name = "Component name") String component) {
        throw new RuntimeException("Should not have tried to create component");
    }

    @Override
    public void createHostComponent(@EffectorParam(name = "Cluster name") String cluster, @EffectorParam(name = "Host FQDN") String hostName, @EffectorParam(name = "Component name") String component) {
        throw new RuntimeException("Should not have tried to create a host component");
    }

    @Override
    public void installHDP(String clusterName, String blueprintName, List<String> hosts, List<String> services) {
        throw new RuntimeException("Should not have tried to create cluster");
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public String getDisplayName() {
        return null;
    }

    @Override
    public String getCatalogItemId() {
        return null;
    }

    @Override
    public TagSupport tags() {
        return null;
    }

    @Override
    public TagSupport getTagSupport() {
        return null;
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return null;
    }

    @Override
    public EntityType getEntityType() {
        return null;
    }

    @Override
    public Application getApplication() {
        return null;
    }

    @Override
    public String getApplicationId() {
        return null;
    }

    @Override
    public Entity getParent() {
        return null;
    }

    @Override
    public Collection<Entity> getChildren() {
        return null;
    }

    @Override
    public Entity setParent(Entity parent) {
        return null;
    }

    @Override
    public void clearParent() {

    }

    @Override
    public <T extends Entity> T addChild(T child) {
        return null;
    }

    @Override
    public <T extends Entity> T addChild(EntitySpec<T> spec) {
        return null;
    }

    @Override
    public boolean removeChild(Entity child) {
        return false;
    }

    @Override
    public Collection<Policy> getPolicies() {
        return null;
    }

    @Override
    public Collection<Enricher> getEnrichers() {
        return null;
    }

    @Override
    public Collection<Group> getGroups() {
        return null;
    }

    @Override
    public void addGroup(Group group) {

    }

    @Override
    public void removeGroup(Group group) {

    }

    @Override
    public Collection<Location> getLocations() {
        return null;
    }

    @Override
    public <T> T getAttribute(AttributeSensor<T> sensor) {
        return null;
    }

    @Override
    public <T> T getConfig(ConfigKey<T> key) {
        return null;
    }

    @Override
    public <T> T getConfig(ConfigKey.HasConfigKey<T> key) {
        return null;
    }

    @Override
    public Maybe<Object> getConfigRaw(ConfigKey<?> key, boolean includeInherited) {
        return null;
    }

    @Override
    public Maybe<Object> getConfigRaw(ConfigKey.HasConfigKey<?> key, boolean includeInherited) {
        return null;
    }

    @Override
    public <T> Task<T> invoke(Effector<T> eff, Map<String, ?> parameters) {
        return null;
    }

    @Override
    public void addPolicy(Policy policy) {

    }

    @Override
    public <T extends Policy> T addPolicy(PolicySpec<T> enricher) {
        return null;
    }

    @Override
    public boolean removePolicy(Policy policy) {
        return false;
    }

    @Override
    public void addEnricher(Enricher enricher) {

    }

    @Override
    public <T extends Enricher> T addEnricher(EnricherSpec<T> enricher) {
        return null;
    }

    @Override
    public boolean removeEnricher(Enricher enricher) {
        return false;
    }

    @Override
    public <T extends Feed> T addFeed(T feed) {
        return null;
    }

    @Override
    public Set<Object> getTags() {
        return null;
    }

    @Override
    public boolean addTag(Object tag) {
        return false;
    }

    @Override
    public boolean removeTag(Object tag) {
        return false;
    }

    @Override
    public boolean containsTag(Object tag) {
        return false;
    }

    @Override
    public void start(@EffectorParam(
            name = "locations") Collection<? extends Location> locations) {

    }

    @Override
    public void stop() {

    }

    @Override
    public void restart() {

    }
}
