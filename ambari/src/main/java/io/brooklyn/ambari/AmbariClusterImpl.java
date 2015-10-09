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

package io.brooklyn.ambari;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import brooklyn.config.ConfigKey;
import brooklyn.enricher.Enrichers;
import brooklyn.entity.Entity;
import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.BasicStartableImpl;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.basic.ServiceStateLogic;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.proxying.EntitySpec;
import brooklyn.event.SensorEvent;
import brooklyn.event.SensorEventListener;
import brooklyn.location.Location;
import brooklyn.management.Task;
import brooklyn.util.collections.MutableList;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.task.Tasks;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.agent.AmbariAgentImpl;
import io.brooklyn.ambari.hostgroup.AmbariHostGroup;
import io.brooklyn.ambari.rest.AmbariApiException;
import io.brooklyn.ambari.rest.domain.Bindings;
import io.brooklyn.ambari.rest.domain.Blueprint;
import io.brooklyn.ambari.rest.domain.HostComponent;
import io.brooklyn.ambari.rest.domain.HostGroup;
import io.brooklyn.ambari.rest.domain.Recommendation;
import io.brooklyn.ambari.rest.domain.RecommendationWrapper;
import io.brooklyn.ambari.rest.domain.RecommendationWrappers;
import io.brooklyn.ambari.rest.domain.Request;
import io.brooklyn.ambari.rest.domain.Stack;
import io.brooklyn.ambari.server.AmbariServer;
import io.brooklyn.ambari.service.ExtraService;
import io.brooklyn.ambari.service.ExtraServiceException;

/**
 * The minimum requirements for an ambari hadoop cluster.
 * These can be set in the provisioning properties of yaml or by
 * using machines of this spec in a byon cluster.
 * <p/>
 * "minRam", 8192,
 * "osFamily", "ubuntu",
 * "osVersionRegex", "12.*",
 */
public class AmbariClusterImpl extends BasicStartableImpl implements AmbariCluster {

    private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(BasicStartableImpl.class);
    public static final ImmutableList<String> DEFAULT_SERVICES = ImmutableList.<String>of("ZOOKEEPER");
    public static final ImmutableMap<String, Map> DEFAULT_CONFIG_MAP = ImmutableMap.<String, Map>of();
    //TODO is there an issue with rebind here?  On rebind should be populated from somewhere else?

    private boolean isHostGroupsDeployment;
    private List<String> services;
    private Map<String, Map> configuration;
    private Map<String, List<EntitySpec<? extends ExtraService>>> entitySpecsByNode;
    private Map<String, List<String>> componentsByNode;
    private Function<AmbariServer, String> mapAmbariServerToFQDN = new Function<AmbariServer, String>() {
        @Nullable
        @Override
        public String apply(@Nullable AmbariServer ambariServer) {
            return ambariServer.getFqdn();
        }
    };

    @Override
    public void init() {
        super.init();

        isHostGroupsDeployment = Iterables.size(getHostGroups()) > 0;

        addChild(createServerSpec(getConfig(SECURITY_GROUP)));
        if (!getConfig(SERVER_COMPONENTS).isEmpty()) {
            for (AmbariServer ambariServer : getAmbariServers()) {
                ambariServer.config().set(SoftwareProcess.CHILDREN_STARTABLE_MODE, SoftwareProcess.ChildStartableMode.BACKGROUND_LATE);
                EntitySpec<? extends AmbariAgent> agentSpec = AmbariAgentImpl.createAgentSpec(this, null);
                ambariServer.addChild(agentSpec);
            }
        }

        configuration = MutableMap.copyOf(getConfig(AMBARI_CONFIGURATIONS));
        services = MutableList.copyOf(getConfig(HADOOP_SERVICES));

        calculateTotalAgents();
        if (!isHostGroupsDeployment) {
            createClusterTopology();
            if (services.size() == 0) {
                services.addAll(DEFAULT_SERVICES);
            }
        }

        if (configuration.size() == 0) {
            configuration.putAll(DEFAULT_CONFIG_MAP);
        }

        addEnricher(Enrichers.builder()
                .propagating(Attributes.MAIN_URI)
                .from(getMasterAmbariServer())
                .build());

        entitySpecsByNode = new MutableMap<String, List<EntitySpec<? extends ExtraService>>>();
        componentsByNode = new MutableMap<String, List<String>>();
        addDeprecatedExtraServiceToExtraServices();
        for (EntitySpec<? extends ExtraService> entitySpec : getConfig(EXTRA_HADOOP_SERVICES)) {
            String bindTo = entitySpec.getFlags().containsKey(ExtraService.BIND_TO.getName())
                    ? String.valueOf(entitySpec.getFlags().get(ExtraService.BIND_TO.getName()))
                    : ExtraService.BIND_TO.getDefaultValue();
            String serviceName = entitySpec.getFlags().containsKey(ExtraService.SERVICE_NAME.getName())
                    ? String.valueOf(entitySpec.getFlags().get(ExtraService.SERVICE_NAME.getName()))
                    : ExtraService.SERVICE_NAME.getDefaultValue();
            List<String> componentNames = entitySpec.getFlags().containsKey(ExtraService.COMPONENT_NAMES.getName())
                    ? (List<String>) entitySpec.getFlags().get(ExtraService.COMPONENT_NAMES.getName())
                    : ExtraService.COMPONENT_NAMES.getDefaultValue();


            if (!entitySpecsByNode.containsKey(bindTo)) {
                entitySpecsByNode.put(bindTo, new MutableList<EntitySpec<? extends ExtraService>>());
            }
            entitySpecsByNode.get(bindTo).add(entitySpec);

            if (isHostGroupsDeployment) {
                Preconditions.checkNotNull(componentNames,
                        "Please specify the list of components names (%s) for \"%s\" as this is a host groups based deployment.",
                        ExtraService.COMPONENT_NAMES.getName(),
                        entitySpec.getType().getName());

                if (!componentsByNode.containsKey(bindTo)) {
                    componentsByNode.put(bindTo, new MutableList<String>());
                }
                if (componentNames.size() > 0) {
                    componentsByNode.get(bindTo).addAll(componentNames);
                }
            } else {
                Preconditions.checkNotNull(serviceName,
                        "Please specify the service name (%s) for \"%s\" as this is a services based deployment.",
                        ExtraService.SERVICE_NAME.getName(),
                        entitySpec.getType().getName());

                if (StringUtils.isNotBlank(serviceName)) {
                    services.add(serviceName);
                }
            }
        }
    }

    private void addDeprecatedExtraServiceToExtraServices() {
        EntitySpec<? extends ExtraService> entitySpec = getConfig(EXTRA_HADOOP_SERVICE);
        if (entitySpec != null) {
            MutableList<EntitySpec<? extends ExtraService>> specs = MutableList.copyOf(getConfig(EXTRA_HADOOP_SERVICES));
            specs.add(entitySpec);
            config().set(EXTRA_HADOOP_SERVICES, specs);
        }
    }

    @Override
    public void start(Collection<? extends Location> locations) {
        super.start(locations);

        subscribe(getMasterAmbariServer(), AmbariServer.REGISTERED_HOSTS, new RegisteredHostEventListener(this));
        subscribe(getMasterAmbariServer(), AmbariServer.CLUSTER_STATE, new ClusterStateEventListener(this));

        EtcHostsManager.setHostsOnMachines(getAmbariNodes(), getConfig(ETC_HOST_ADDRESS));
    }

    @Override
    public Iterable<AmbariNode> getAmbariNodes() {
        return Entities.descendants(this, AmbariNode.class);
    }

    @Override
    public Iterable<AmbariAgent> getAmbariAgents() {
        return Entities.descendants(this, AmbariAgent.class);
    }

    @Override
    public Iterable<AmbariServer> getAmbariServers() {
        return Entities.descendants(this, AmbariServer.class);
    }

    @Override
    public AmbariServer getMasterAmbariServer() {
        return Iterables.getFirst(Entities.descendants(this, AmbariServer.class), null);
    }

    @Override
    public List<String> getExtraStackDefinitionsUrls() {
        return getConfig(STACK_DEFINITION_URLS);
    }

    static final class RegisteredHostEventListener implements SensorEventListener<List<String>> {

        private final AmbariCluster entity;

        public RegisteredHostEventListener(AmbariCluster entity) {
            this.entity = entity;
        }

        @Override
        public void onEvent(SensorEvent<List<String>> event) {
            List<String> hosts = event.getValue();
            Integer initialClusterSize = entity.getAttribute(EXPECTED_AGENTS);
            Boolean initialised = entity.getAttribute(CLUSTER_SERVICES_INITIALISE_CALLED);
            if (hosts != null && hosts.size() == initialClusterSize && !Boolean.TRUE.equals(initialised)) {
                try {
                    entity.deployCluster();
                } catch (AmbariApiException ex) {
                    ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) entity, "ambari.api", ex.getMessage());
                    throw ex;
                } catch (ExtraServiceException ex) {
                    ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) entity, "ambari.extra.service", ex.getMessage());
                    throw ex;
                }
            }
        }
    }

    static final class ClusterStateEventListener implements SensorEventListener<String> {

        private final AmbariCluster entity;

        public ClusterStateEventListener(AmbariCluster entity) {
            this.entity = entity;
        }

        @Override
        public void onEvent(SensorEvent<String> sensorEvent) {
            Boolean installed = entity.getAttribute(CLUSTER_SERVICES_INSTALLED);
            if (StringUtils.isNotBlank(sensorEvent.getValue()) && sensorEvent.getValue().equals("COMPLETED") && !Boolean.TRUE.equals(installed)) {
                try {
                    entity.postDeployCluster();
                } catch (ExtraServiceException ex) {
                    ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) entity, "ambari.extra.service", ex.getMessage());
                    throw ex;
                }
            }
        }
    }

    @Override
    public void deployCluster() throws AmbariApiException, ExtraServiceException {
        // Set the flag to true so the deployment won't happen multiple times
        setAttribute(CLUSTER_SERVICES_INITIALISE_CALLED, true);

        // Wait for the Ambari server to be up
        getMasterAmbariServer().waitForServiceUp();

        final Map<String, List<String>> componentsByNodeName = new MutableMap<String, List<String>>();

        RecommendationWrapper recommendationWrapper = null;

        if (isHostGroupsDeployment) {
            LOG.info("{} getting the recommendation from AmbariHostGroup configuration", this);
            recommendationWrapper = getRecommendationWrapperFromAmbariHostGroups();
        } else {
            LOG.info("{} getting the recommendation from Ambari for the services: {}", this, services);
            recommendationWrapper = getRecommendationWrapperFromAmbariServer();
        }

        Preconditions.checkNotNull(recommendationWrapper);
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation());
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation().getBlueprint());
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation().getBindings());

        for (HostGroup hostGroup : recommendationWrapper.getRecommendation().getBlueprint().getHostGroups()) {
            if (!componentsByNodeName.containsKey(hostGroup.getName())) {
                componentsByNodeName.put(hostGroup.getName(), new MutableList<String>());
            }
            final List<HostComponent> hostComponents = MutableList.copyOf(hostGroup.getComponents());
            for (HostComponent component : hostComponents) {
                // We need to filter out the ZKFC component otherwise installation fails. This will remove ZKFC component
                // from the recommendations (as we use it afterward) and the list of components we created.
                if (StringUtils.equals(component.getName(), "ZKFC")) {
                    hostGroup.getComponents().remove(component);
                    continue;
                }
                componentsByNodeName.get(hostGroup.getName()).add(component.getName());
            }
        }

        for (HostGroup hostGroup : recommendationWrapper.getRecommendation().getBindings().getHostGroups()) {
            AmbariAgent ambariAgent = null;

            for (int i = 0; i < hostGroup.getHosts().size(); i++) {
                final Map<String, String> host = hostGroup.getHosts().get(i);
                final String fqdn = host.get("fqdn");
                if (StringUtils.isNotBlank(fqdn)) {
                    final List<String> components = componentsByNodeName.get(hostGroup.getName());
                    ambariAgent = getAmbariAgentByFqdn(fqdn);
                    if (ambariAgent != null && components != null) {
                        ambariAgent.setComponents(components);
                    }
                }
            }
            if (entitySpecsByNode.containsKey(hostGroup.getName()) && ambariAgent != null) {
                bindExtraServices(entitySpecsByNode.get(hostGroup.getName()), ambariAgent.getParent());
            }
        }
        if (entitySpecsByNode.containsKey(SERVER_HOST_GROUP)) {
            bindExtraServices(entitySpecsByNode.get(SERVER_HOST_GROUP), getMasterAmbariServer());
        }

        LOG.info("{} calling pre-cluster-deploy on all Ambari nodes", this);
        try {
            Task<List<?>> preDeployClusterTasks = parallelListenerTask(new PreClusterDeployFunction());
            Entities.submit(this, preDeployClusterTasks).get();
        } catch (ExecutionException | InterruptedException ex) {
            // If something failed within an extra service, we propagate the exception for the cluster to handle it properly.
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if (rootCause != null && rootCause instanceof ExtraServiceException) {
                throw (ExtraServiceException) rootCause;
            } else {
                throw new ExtraServiceException(ex.getMessage());
            }
        }

        LOG.info("{} calling cluster-deploy with services: {}", this, services);
        try {
            Request request = getMasterAmbariServer().deployCluster("Cluster1", "mybp", recommendationWrapper, configuration);
        } catch (AmbariApiException ex) {
            // If the cluster failed to deploy, we first put the server "ON FIRE" and throw again the exception for the
            // cluster to handle it properly.
            ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) getMasterAmbariServer(), "ambari.api", ex.getMessage());
            throw ex;
        }
    }

    @Override
    public void postDeployCluster() throws ExtraServiceException {
        // Set the flag to true so the post deployment won't happen multiple times
        setAttribute(CLUSTER_SERVICES_INSTALLED, true);

        LOG.info("{} calling post-cluster-deploy on all Ambari nodes", this);
        try {
            Task<List<?>> postDeployClusterTasks = parallelListenerTask(new PostClusterDeployFunction());
            Entities.submit(this, postDeployClusterTasks).get();
        } catch (ExecutionException | InterruptedException ex) {
            // If something failed within an extra service, we propagate the exception for the cluster to handle it properly.
            Throwable rootCause = ExceptionUtils.getRootCause(ex);
            if (rootCause != null && rootCause instanceof ExtraServiceException) {
                throw (ExtraServiceException) rootCause;
            } else {
                throw new ExtraServiceException(ex.getMessage());
            }
        }
    }

    private EntitySpec<? extends AmbariServer> createServerSpec(Object securityGroup) {
        EntitySpec<? extends AmbariServer> serverSpec = EntitySpec.create(getConfig(SERVER_SPEC))
                .configure(SoftwareProcess.SUGGESTED_VERSION, getConfig(AmbariCluster.SUGGESTED_VERSION))
                .configure(config().getBag().getAllConfig())
                .displayName("Ambari Server");
        if (securityGroup != null) {
            serverSpec.configure(SoftwareProcess.PROVISIONING_PROPERTIES.subKey("securityGroups"), securityGroup);
        }
        return serverSpec;
    }

    private RecommendationWrapper getRecommendationWrapperFromAmbariHostGroups() {
        final Blueprint.Builder blueprintBuilder = new Blueprint.Builder();
        final Bindings.Builder bindingsBuilder = new Bindings.Builder();

        for (AmbariHostGroup ambariHostGroup : getHostGroups()) {
            HostGroup.Builder hostGroupBuilder = new HostGroup.Builder()
                    .setName(ambariHostGroup.getDisplayName())
                    .addComponents(ambariHostGroup.getComponents());
            if (componentsByNode.containsKey(ambariHostGroup.getDisplayName())) {
                hostGroupBuilder.addComponents(componentsByNode.get(ambariHostGroup.getDisplayName()));
            }
            blueprintBuilder.addHostGroup(hostGroupBuilder.build());

            bindingsBuilder.addHostGroup(new HostGroup.Builder()
                    .setName(ambariHostGroup.getDisplayName())
                    .addHosts(ambariHostGroup.getHostFQDNs())
                    .build());
        }

        List<String> serverComponentsList = getConfig(AmbariCluster.SERVER_COMPONENTS);
        if (!serverComponentsList.isEmpty()) {
            HostGroup.Builder hostGroupBuilder = new HostGroup.Builder()
                    .setName(SERVER_HOST_GROUP)
                    .addComponents(serverComponentsList);
            if (componentsByNode.containsKey(SERVER_HOST_GROUP)) {
                hostGroupBuilder.addComponents(componentsByNode.get(SERVER_HOST_GROUP));
            }
            blueprintBuilder.addHostGroup(hostGroupBuilder.build());
            Iterable<AmbariServer> ambariServers = getAmbariServers();
            Iterable<String> fqdns = Iterables.transform(ambariServers, mapAmbariServerToFQDN);

            bindingsBuilder.addHostGroup(new HostGroup.Builder()
                    .setName(SERVER_HOST_GROUP)
                    .addHosts(Lists.newArrayList(fqdns))
                    .build());
        }

        return new RecommendationWrapper.Builder()
                .setStack(new Stack.Builder()
                        .setName(getConfig(HADOOP_STACK_NAME))
                        .setVersion(getConfig(HADOOP_STACK_VERSION))
                        .build())
                .setRecommendation(new Recommendation.Builder()
                        .setBlueprint(blueprintBuilder.build())
                        .setBindings(bindingsBuilder.build())
                        .build())
                .build();
    }

    private RecommendationWrapper getRecommendationWrapperFromAmbariServer() {
        final List<String> hosts = getMasterAmbariServer().getAttribute(AmbariServer.REGISTERED_HOSTS);
        final RecommendationWrappers recommendationWrappers = getMasterAmbariServer()
                .getRecommendations(getConfig(HADOOP_STACK_NAME), getConfig(HADOOP_STACK_VERSION), hosts, services);

        return recommendationWrappers.getRecommendationWrappers().size() > 0
                ? recommendationWrappers.getRecommendationWrappers().get(0)
                : null;
    }

    private Task<List<?>> parallelListenerTask(final Function<ExtraService, ?> fn) {
        List<Task<?>> tasks = Lists.newArrayList();
        for (final ExtraService extraService : getExtraServices()) {
            Task<?> t = Tasks.builder()
                    .name(extraService.toString())
                    .description("Invocation on " + extraService.toString())
                    .body(new FunctionRunningCallable<ExtraService>(extraService, fn))
                    .build();
            tasks.add(t);
        }
        return Tasks.parallel("Parallel invocation of " + fn + " on extra services", tasks);
    }

    @Nonnull
    private AmbariCluster getAmbariCluster() {
        return this;
    }

    @Nonnull
    private Iterable<ExtraService> getExtraServices() {
        return Entities.descendants(this, ExtraService.class);
    }

    private void bindExtraServices(List<EntitySpec<? extends ExtraService>> entitySpecs, Entity entity) {
        for (EntitySpec<? extends ExtraService> entitySpec : entitySpecs != null ? entitySpecs : ImmutableList.<EntitySpec<? extends ExtraService>>of()) {
            ExtraService child = entity.addChild(entitySpec);
            Entities.manage(child);
            if (child.getAmbariConfig() != null) {
                configuration.putAll(child.getAmbariConfig());
            }
        }
    }

    @Nullable
    private AmbariAgent getAmbariAgentByFqdn(@Nonnull String fqdn) {
        Preconditions.checkNotNull(fqdn);

        for (AmbariAgent ambariAgent : Entities.descendants(this, AmbariAgent.class)) {
            if (StringUtils.equals(ambariAgent.getFqdn(), fqdn)) {
                return ambariAgent;
            }
        }
        return null;
    }

    private class PreClusterDeployFunction implements Function<ExtraService, Void> {
        @Override
        public Void apply(ExtraService extraService) {
            extraService.preClusterDeploy(getAmbariCluster());
            return null;
        }
    }

    private class PostClusterDeployFunction implements Function<ExtraService, Void> {
        @Override
        public Void apply(ExtraService extraService) {
            extraService.postClusterDeploy(getAmbariCluster());
            return null;
        }
    }

    private void calculateTotalAgents() {
        int agentsToExpect = 0;

        if (isHostGroupsDeployment) {
            for (AmbariHostGroup hostGroup : getHostGroups()) {
                agentsToExpect += hostGroup.getConfig(AmbariHostGroup.INITIAL_SIZE);
            }
        } else {
            agentsToExpect += getRequiredConfig(INITIAL_SIZE);
        }

        if (getMasterAmbariServer().agentOnServer()) {
            agentsToExpect += Iterables.size(getAmbariServers());
        }

        setAttribute(EXPECTED_AGENTS, agentsToExpect);
    }

    private Iterable<AmbariHostGroup> getHostGroups() {
        return Entities.descendants(this, AmbariHostGroup.class);
    }

    private void createClusterTopology() {
        for (int i = 0; i < getAttribute(EXPECTED_AGENTS); i++) {
            addChild(EntitySpec.create(AmbariHostGroup.class)
                    .configure(AmbariHostGroup.INITIAL_SIZE, 1)
                    .displayName(String.format("host-group-%d", (i + 1))));
        }
    }

    private <T> T getRequiredConfig(ConfigKey<T> key) {
        return checkNotNull(getConfig(key), "config %s", key);
    }
}
