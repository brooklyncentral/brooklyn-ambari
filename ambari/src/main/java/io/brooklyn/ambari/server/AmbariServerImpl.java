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

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

import org.apache.brooklyn.api.entity.Entity;
import org.apache.brooklyn.api.mgmt.Task;
import org.apache.brooklyn.core.annotation.EffectorParam;
import org.apache.brooklyn.core.entity.Attributes;
import org.apache.brooklyn.core.entity.Entities;
import org.apache.brooklyn.core.location.access.BrooklynAccessUtils;
import org.apache.brooklyn.enricher.stock.Enrichers;
import org.apache.brooklyn.entity.software.base.SoftwareProcessImpl;
import org.apache.brooklyn.feed.http.HttpFeed;
import org.apache.brooklyn.feed.http.HttpPollConfig;
import org.apache.brooklyn.feed.http.HttpValueFunctions;
import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.http.HttpTool;
import org.apache.brooklyn.util.core.task.DynamicTasks;
import org.apache.brooklyn.util.core.task.Tasks;
import org.apache.brooklyn.util.guava.Functionals;
import org.apache.brooklyn.util.text.Identifiers;
import org.apache.brooklyn.util.text.Strings;
import org.apache.brooklyn.util.time.Duration;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.api.client.util.Lists;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.net.HostAndPort;
import com.google.common.net.HttpHeaders;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.JsonPath;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.rest.AmbariApiException;
import io.brooklyn.ambari.rest.AmbariRequestInterceptor;
import io.brooklyn.ambari.rest.RequestCheckRunnable;
import io.brooklyn.ambari.rest.domain.RecommendationWrapper;
import io.brooklyn.ambari.rest.domain.RecommendationWrappers;
import io.brooklyn.ambari.rest.domain.Request;
import io.brooklyn.ambari.rest.domain.HostGroup;
import io.brooklyn.ambari.rest.domain.AlertTargets;
import io.brooklyn.ambari.rest.endpoint.BlueprintEndpoint;
import io.brooklyn.ambari.rest.endpoint.ClusterEndpoint;
import io.brooklyn.ambari.rest.endpoint.ConfigurationEnpoint;
import io.brooklyn.ambari.rest.endpoint.HostEndpoint;
import io.brooklyn.ambari.rest.endpoint.ServiceEndpoint;
import io.brooklyn.ambari.rest.endpoint.StackEndpoint;
import io.brooklyn.ambari.rest.endpoint.UsersEndpoint;
import io.brooklyn.ambari.rest.endpoint.AlertTargetEndpoint;
import io.brooklyn.ambari.rest.endpoint.AlertGroupEndpoint;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

public class AmbariServerImpl extends SoftwareProcessImpl implements AmbariServer {

    public static final Logger LOG = LoggerFactory.getLogger(AmbariServerImpl.class);
    public static final Map<String, String> BASE_BLUEPRINTS = ImmutableMap.of("stack_name", "HDP", "stack_version", "2.2");
    public static final List<? extends Map<?, ?>> CONFIGURATIONS = ImmutableList.of(ImmutableMap.of("nagios-env", ImmutableMap.of("nagios_contact", "admin@localhost")));
    private volatile HttpFeed serviceUpHttpFeed;
    private volatile HttpFeed hostsHttpFeed;
    private volatile HttpFeed clusterHttpFeed;
    private String ambariUri;

    private RestAdapter restAdapter;

    private UsernamePasswordCredentials usernamePasswordCredentials;

    private static final String USERNAME = "admin";
    private static final String INITIAL_PASSWORD = "admin";

    private Map<String, Integer> registeredAlertNotifications;

    @Override
    public Class<AmbariServerDriver> getDriverInterface() {
        return AmbariServerDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();
        connectServiceUpIsRunning();

        HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, getAttribute(HTTP_PORT));

        ambariUri = String.format("http://%s:%d", hp.getHostText(), hp.getPort());

        setAttribute(Attributes.MAIN_URI, URI.create(ambariUri));

        sensors().set(AmbariServer.USERNAME, USERNAME);
        usernamePasswordCredentials = new UsernamePasswordCredentials(
                USERNAME,
                getAttribute(AmbariServer.PASSWORD) == null ? INITIAL_PASSWORD : getAttribute(AmbariServer.PASSWORD));

        restAdapter = new RestAdapter.Builder()
                .setEndpoint(ambariUri)
                .setRequestInterceptor(new AmbariRequestInterceptor(usernamePasswordCredentials))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        serviceUpHttpFeed = HttpFeed.builder()
                .entity(this)
                .period(500, TimeUnit.MILLISECONDS)
                .baseUri(ambariUri)
                .poll(new HttpPollConfig<Boolean>(URL_REACHABLE)
                        .onSuccess(HttpValueFunctions.responseCodeEquals(200))
                        .onFailureOrException(Functions.constant(false)))
                .build();

        enrichers().add(Enrichers.builder().updatingMap(Attributes.SERVICE_NOT_UP_INDICATORS)
                .from(URL_REACHABLE)
                .computing(Functionals.ifNotEquals(true).value("URL not reachable"))
                .build());

        connectAuthenticatedSensors();
    }

    Function<JsonElement, List<String>> getHosts() {
        Function<JsonElement, List<String>> path = new Function<JsonElement, List<String>>() {
            @Nullable
            @Override
            public List<String> apply(@Nullable JsonElement jsonElement) {
                String jsonString = jsonElement.toString();
                return JsonPath.read(jsonString, "$.items[*].Hosts.host_name");
            }
        };
        return path;
    }

    Function<JsonElement, String> getRequestState() {
        Function<JsonElement, String> path = new Function<JsonElement, String>() {
            @Nullable
            @Override
            public String apply(@Nullable JsonElement jsonElement) {
                String jsonString = jsonElement.toString();
                return JsonPath.read(jsonString, "$.Requests.request_status");
            }
        };
        return path;
    }

    @Override
    public void disconnectSensors() {
        super.disconnectSensors();
        disconnectServiceUpIsRunning();

        if (serviceUpHttpFeed != null) serviceUpHttpFeed.stop();

        disconnectAuthenticatedSensors();
    }

    @Override
    public RecommendationWrappers getRecommendations(String stackName, String stackVersion, List<String> hosts, List<String> services) {
        waitForServiceUp();

        return restAdapter.create(StackEndpoint.class).getRecommendations(stackName, stackVersion, ImmutableMap.builder()
                .put("hosts", hosts)
                .put("services", services)
                .put("recommend", "host_groups")
                .build());
    }

    @Override
    public Request deployCluster(String clusterName, String blueprintName, RecommendationWrapper recommendationWrapper, Map config) throws AmbariApiException {
        Preconditions.checkNotNull(recommendationWrapper);
        Preconditions.checkNotNull(recommendationWrapper.getStack());
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation());
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation().getBlueprint());
        Preconditions.checkNotNull(recommendationWrapper.getRecommendation().getBindings());

        try {
            restAdapter.create(BlueprintEndpoint.class).createBlueprint(blueprintName, ImmutableMap.builder()
                    .put("host_groups", recommendationWrapper.getRecommendation().getBlueprint().getHostGroups())
                    .put("configurations", getConfigurations(config))
                    .put("Blueprints", recommendationWrapper.getStack())
                    .build());

            List<HostGroup> confHostGroupsList = recommendationWrapper.getRecommendation().getBindings().getHostGroups();
            List<HostGroup> nonZeroHostGroupList = new LinkedList<>();

            for(HostGroup hostGroupN:confHostGroupsList) {
                if (hostGroupN.getHosts().size() > 0) {
                    nonZeroHostGroupList.add(hostGroupN);
                }
            }

            return restAdapter.create(ClusterEndpoint.class).createCluster(clusterName, ImmutableMap.builder()
                    .put("blueprint", blueprintName)
                    .put("default_password", usernamePasswordCredentials.getPassword())
                    .put("host_groups", nonZeroHostGroupList)
                    .build());
        } catch (RetrofitError retrofitError) {
            throw new AmbariApiException(retrofitError);
        }
    }

    @Override
    public void addHostToCluster(@EffectorParam(name = "Cluster name") String cluster,
                                 @EffectorParam(name = "Host FQDN") String hostName) {
        waitForServiceUp();
        restAdapter.create(HostEndpoint.class).addHost(cluster, hostName);

    }

    @Override
    public void createCluster(@EffectorParam(name = "Cluster Name") String clusterName,
                              @EffectorParam(name = "Blueprint Name") String blueprintName,
                              @EffectorParam(name = "Stack Name") String stackName,
                              @EffectorParam(name = "Stack version") String stackVersion,
                              @EffectorParam(name = "Hosts", description = "List of FQDNs to add to cluster") List<String> hosts,
                              @EffectorParam(name = "Services", description = "List of services to install on cluster") List<String> services,
                              @EffectorParam(name = "Configurations", description = "Map of configurations to apply to blueprint") Map<String, Map> config) {
        final RecommendationWrappers recommendationWrappers = getRecommendations(stackName, stackVersion, hosts, services);

        deployCluster(clusterName, blueprintName, recommendationWrappers.getRecommendationWrappers().size() > 0 ? recommendationWrappers.getRecommendationWrappers().get(0) : null, config);
    }

    @Override
    public void updateStackRepository(@EffectorParam(name = "Stack Name") String stackName, @EffectorParam(name = "Stack Version") String stackVersion, @EffectorParam(name = "Operating System") String os, @EffectorParam(name = "Repository Name") String repoName, @EffectorParam(name = "Repository URL") String url) {
        waitForServiceUp();
        restAdapter.create(StackEndpoint.class)
                .updateStackRepository(stackName, stackVersion, os, repoName, ImmutableMap.builder()
                        .put("Repositories", ImmutableMap.builder()
                                .put("base_url", url)
                                .put("verify_base_url", true)
                                .build())
                        .build());
    }

    @Override
    public void addServiceToCluster(@EffectorParam(name = "cluster", description = "Cluster name") final String cluster,
                                    @EffectorParam(name = "service", description = "Service name") final String service,
                                    @EffectorParam(name = "mappings", description = "Mappings of component to host") Map<String, String> mappings,
                                    @EffectorParam(name = "configuration", description = "Services Configuration", nullable = true, defaultValue = EffectorParam.MAGIC_STRING_MEANING_NULL) Map<String, Map<Object, Object>> configuration) {
        waitForServiceUp();

        final ServiceEndpoint serviceEndpoint = restAdapter.create(ServiceEndpoint.class);
        final HostEndpoint hostEndpoint = restAdapter.create(HostEndpoint.class);

        // Step 1 - Add the service to the cluster
        serviceEndpoint.addService(cluster, service);

        // Step 2 - Add Components to the service
        // Step 3 - Create host components
        for (Map.Entry<String, String> mapping : mappings.entrySet()) {
            serviceEndpoint.createComponent(cluster, service, mapping.getKey());
            hostEndpoint.createHostComponent(cluster, mapping.getValue(), mapping.getKey());
        }

        // Step 4 - Create configuration, if needed
        if (configuration != null) {
            for (Map.Entry<String, Map<Object, Object>> entry : configuration.entrySet()) {
                createServiceConfiguration(cluster, entry.getKey(), entry.getValue());
            }
        }

        final Task installationTask = Tasks.builder()
                .name(String.format("Install %s service", service))
                .description(String.format("Install %s service on specified hosts through Ambari REST API", service))
                .body(new Runnable() {
                    @Override
                    public void run() {
                        // Step 5 - Install the service
                        final Request request = serviceEndpoint.updateService(cluster, service, ImmutableMap.builder()
                                .put("RequestInfo", ImmutableMap.builder()
                                        .put("context", String.format("Install %s service", service))
                                        .build())
                                .put("ServiceInfo", ImmutableMap.builder()
                                        .put("state", "INSTALLED")
                                        .build())
                                .build());

                        RequestCheckRunnable.check(request)
                                .headers(ImmutableMap.of(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials)))
                                .errorMessage(String.format("Error during installation of service \"%s\". Please check the Ambari console for more details: %s", service, ambariUri))
                                .build()
                                .run();
                    }
                }).build();
        final Task startTask = Tasks.builder()
                .name(String.format("Start %s service", service))
                .description(String.format("Start %s service on specified hosts through Ambari REST API", service))
                .body(new Runnable() {
                    @Override
                    public void run() {
                        // Step 6 - Start the service
                        startService(cluster, service);
                    }
                }).build();

        // Queue the "Installation" subtask and wait for its completion. If something goes wrong during execution, an
        // exception will be thrown which will stop the effector and prevent the "start" subtask to run.
        DynamicTasks.queue(installationTask);
        // Queue the "Start" subtask. At this point, everything went fine. If something goes wrong during execution, an
        // exception will be thrown which will stop the effector.
        DynamicTasks.queue(startTask);
    }

    @Override
    public void createServiceConfiguration(@EffectorParam(name = "Cluster name") String cluster,
                                           @EffectorParam(name = "Component configuration key") String configurationKey,
                                           @EffectorParam(name = "Component configuration") Map<Object, Object> configuration) {
        waitForServiceUp();
        restAdapter.create(ConfigurationEnpoint.class).createConfiguration(cluster, ImmutableMap.builder()
                .put("Clusters", ImmutableMap.builder()
                        .put("desired_configs", ImmutableMap.builder()
                                .put("type", configurationKey)
                                .put("tag", String.format("version%d", System.currentTimeMillis()))
                                .put("properties", configuration)
                                .build())
                        .build())
                .build());
    }

    @Override
    public void addHostsToHostGroup(final String blueprintName, final String hostgroupName, final List<String> hosts, final String cluster) {
        Iterable<Map> hostGroupMapping = Iterables.transform(hosts, fqdnsToMaps(blueprintName, hostgroupName));
        LOG.info("hosts " + hostGroupMapping.iterator().hasNext());

        HostEndpoint hostEndpoint = restAdapter.create(HostEndpoint.class);
        Request request = hostEndpoint.addHosts(
                cluster,
                Lists.newArrayList(hostGroupMapping));

        RequestCheckRunnable.check(request)
                .headers(ImmutableMap.of(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials)))
                .timeout(Duration.ONE_HOUR)
                .errorMessage(String.format("Error during adding %s to %s", hosts, hostgroupName))
                .build()
                .run();
    }

    private Function<String, Map> fqdnsToMaps(final String blueprintName, final String hostgroupName) {
        return new Function<String, Map>() {
            @Nullable
            @Override
            public Map apply(@Nullable String fqdn) {
                return ImmutableMap.of("blueprint", blueprintName,
                        "host_group", hostgroupName,
                        "host_name", fqdn);
            }
        };
    }

    @Override
    public void addAlertNotification(String name, String description, Boolean global, String notificationType,
                                     List<String> alertStates, List<String> ambariDispatchRecipients,
                                     String mailSmtpHost, Integer mailSmtpPort, String mailSmtpFrom, Boolean mailSmtpAuth) {

        mailSmtpHost = mailSmtpHost.equals("default") ? (String) ((Map) getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).get("default_properties")).get("mail.smtp.host") : mailSmtpHost;
        mailSmtpPort = mailSmtpPort.equals("587") ? (Integer) ((Map) getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).get("default_properties")).get("mail.smtp.port") : mailSmtpPort;
        mailSmtpFrom = mailSmtpFrom.equals("default") ? (String) ((Map) getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).get("default_properties")).get("mail.smtp.from") : mailSmtpFrom;

        Map<String, Object> ambariAlertNotifications = ImmutableMap.<String, Object>builder()
                .put("name", name)
                .put("description", description)
                .put("global", global)
                .put("notification_type", notificationType)
                .put("alert_states", alertStates)
                .put("properties", ImmutableMap.of(
                        "ambari.dispatch.recipients", ambariDispatchRecipients,
                        "mail.smtp.host", mailSmtpHost,
                        "mail.smtp.port", mailSmtpPort,
                        "mail.smtp.from", mailSmtpFrom,
                        "mail.smtp.auth", mailSmtpAuth
                ))
                .build();

        createAlertNotification(ambariAlertNotifications);
    }

    protected void createAlertNotification(){
        Map<String, Object> ambariAlertNotifications = MutableMap.of();

        for (Map.Entry config: getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).entrySet()) {
            Object value = (config.getValue() instanceof Map && !((String) config.getKey()).contains("properties")) ?
                    ((Map) config.getValue()).keySet().toArray()[0] : config.getValue();
            ambariAlertNotifications.put((String) config.getKey(), value);
        }

        Map<String, Object> mergedPropertiesConfig = MutableMap.of();
        mergedPropertiesConfig.putAll((Map<String, Object>) getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).get("default_properties"));
        mergedPropertiesConfig.putAll((Map<String, Object>) ambariAlertNotifications.get("properties"));
        ambariAlertNotifications.put("properties", mergedPropertiesConfig);
        ambariAlertNotifications.remove("default_properties");

        createAlertNotification(ambariAlertNotifications);
    }

    protected void createAlertNotification(Map<String, Object> ambariAlertNotifications) {
        ImmutableMap<String, Map<String, Object>> alertTargetRequest = ImmutableMap.of("AlertTarget", ambariAlertNotifications);

        restAdapter.create(AlertTargetEndpoint.class).createAlertNotification(alertTargetRequest);

        registeredAlertNotifications = listAlertTargets();
    }

    @Override
    public void editAlertNotification(String name, String description, Boolean global, String notificationType,
                                      List<String> alertStates, List<String> ambariDispatchRecipients,
                                      String mailSmtpHost, Integer mailSmtpPort, String mailSmtpFrom, Boolean mailSmtpAuth) {

        registeredAlertNotifications = listAlertTargets();

        mailSmtpHost = mailSmtpHost.equals("default") ? (String) ((Map) getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).get("default_properties")).get("mail.smtp.host") : mailSmtpHost;
        mailSmtpPort = mailSmtpPort.equals("587") ? (Integer) ((Map) getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).get("default_properties")).get("mail.smtp.port") : mailSmtpPort;
        mailSmtpFrom = mailSmtpFrom.equals("default") ? (String) ((Map) getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).get("default_properties")).get("mail.smtp.from") : mailSmtpFrom;

        ImmutableMap<String, Object> alertTargetRequest = ImmutableMap.<String, Object>builder()
                .put("name", name)
                .put("description", description)
                .put("global", global)
                .put("notification_type", notificationType)
                .put("alert_states", alertStates)
                .put("properties", ImmutableMap.of(
                        "ambari.dispatch.recipients", ambariDispatchRecipients,
                        "mail.smtp.host", mailSmtpHost,
                        "mail.smtp.port", mailSmtpPort,
                        "mail.smtp.from", mailSmtpFrom,
                        "mail.smtp.auth", mailSmtpAuth
                ))
                .build();

        if (!registeredAlertNotifications.containsKey(name)) {
            throw new UnsupportedOperationException(String.valueOf(Strings.format("Alert with name %s doesn't exist and cannot be updated.", name)));
        }
        updateAlertTarget(registeredAlertNotifications.get(name), alertTargetRequest);
    }

    protected Map<String, Integer> listAlertTargets() {
        AlertTargets alertTargets = restAdapter.create(AlertTargetEndpoint.class).listAlertNotifications();

        Map<String, Integer> result = MutableMap.of();
        for (Map<String, Object> item: alertTargets.getItems()) {
            result.put((String) ((Map) item.get("AlertTarget")).get("name"), ((Double) ((Map) item.get("AlertTarget")).get("id")).intValue());
        }

        return result;
    }

    protected void updateAlertTarget(Integer targetId, Map<String, Object> ambariAlertNotifications) {
        ImmutableMap<String, Map<String, Object>> alertTargetRequest = ImmutableMap.of("AlertTarget", ambariAlertNotifications);

        restAdapter.create(AlertTargetEndpoint.class).editAlertNotification(targetId, alertTargetRequest);
    }

    @Override
    public void deleteAlertNotification(String name) {
        if (registeredAlertNotifications == null) {
            registeredAlertNotifications = listAlertTargets();
        }
        if (!registeredAlertNotifications.containsKey(name)) {
            throw new UnsupportedOperationException(String.valueOf(Strings.format("Alert with name %s doesn't exist and cannot be deleted.", name)));
        }
        restAdapter.create(AlertTargetEndpoint.class).deleteAlertNotification(registeredAlertNotifications.get(name));
    }

    @Override
    public void addAlertGroup(@EffectorParam(name = "Name") String name,
                              @EffectorParam(name = "Definitions") List<Integer> definitions) {

        ImmutableMap<String, ImmutableMap<Object, Object>> alertGroupRequest = ImmutableMap.of("AlertGroup", ImmutableMap.builder()
                .put("name", name)
                .put("definitions", definitions)
                .build());

        restAdapter.create(AlertGroupEndpoint.class).createAlertGroup(getConfig(AmbariCluster.CLUSTER_NAME), alertGroupRequest);
    }

    @Override
    public void startService(@EffectorParam(name = "Cluster name") String cluster,
                             @EffectorParam(name = "Service name") final String service) {
        waitForServiceUp();

        final Request request = restAdapter.create(ServiceEndpoint.class).updateService(cluster, service, ImmutableMap.builder()
                .put("RequestInfo", ImmutableMap.builder()
                        .put("context", String.format("Start %s service", service))
                        .build())
                .put("ServiceInfo", ImmutableMap.builder()
                        .put("state", "STARTED")
                        .build())
                .build());

        RequestCheckRunnable.check(request)
                .headers(ImmutableMap.of(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials)))
                .errorMessage(String.format("Error during the start of service \"%s\". Please check the Ambari console for more details: %s", service, ambariUri))
                .build()
                .run();
    }

    @Override
    public boolean agentOnServer() {
        Iterable<AmbariCluster> ambariClusters = Iterables.filter(Entities.ancestors(this), AmbariCluster.class);
        for (Entity parent : ambariClusters) {
            return !parent.getConfig(AmbariCluster.SERVER_COMPONENTS).isEmpty();
        }
        return false;
    }

    private List<? extends Map<?, ?>> getConfigurations(Map<String, Map> config) {
        ImmutableList.Builder<Map<?, ?>> builder = ImmutableList.<Map<?, ?>>builder();
        if (config != null) {
            for (Map.Entry<String, Map> stringMapEntry : config.entrySet()) {
                builder.add(
                        ImmutableMap.of(
                                stringMapEntry.getKey(),
                                ImmutableMap.<String, Map>of(
                                        "properties",
                                        stringMapEntry.getValue())));
            }
        }
        return builder.build();
    }

    @Override
    public String getFqdn() {
        return getAttribute(FQDN);
    }

    @Override
    public void setFqdn(String fqdn) {
        setAttribute(FQDN, fqdn);
    }

    public void setRestAdapter(RestAdapter restAdapter) {
        this.restAdapter = restAdapter;
    }

    @Override
    public void postStart() {
        super.postStart();
        generateAndUpdatePassword();

        if (!getConfig(AmbariCluster.AMBARI_ALERT_NOTIFICATIONS).isEmpty()) {
            createAlertNotification();
        }
        registeredAlertNotifications = listAlertTargets();
    }

    /**
     * Need to wait server to start before resetting the password.
     */
    protected void generateAndUpdatePassword() {
        String password = getAttribute(AmbariServer.PASSWORD);
        if (password == null) {
            password = getConfig(AmbariServer.PASSWORD);
            if (password == null) {
                password = Identifiers.makeRandomId(10);
            }
            usernamePasswordCredentials = new UsernamePasswordCredentials(USERNAME, password);

            ImmutableMap<String, ImmutableMap<String, String>> userRequest = ImmutableMap.of("Users", ImmutableMap.of(
                    "user_name", USERNAME,
                    "old_password", INITIAL_PASSWORD,
                    "password", usernamePasswordCredentials.getPassword()
            ));

            restAdapter.create(UsersEndpoint.class).updateUser(userRequest);

            restAdapter = new RestAdapter.Builder()
                    .setEndpoint(ambariUri)
                    .setRequestInterceptor(new AmbariRequestInterceptor(usernamePasswordCredentials))
                    .setLogLevel(RestAdapter.LogLevel.FULL)
                    .build();

            disconnectAuthenticatedSensors();
            connectAuthenticatedSensors();

            sensors().set(AmbariServer.PASSWORD, password);
        }
    }

    private void connectAuthenticatedSensors() {
        hostsHttpFeed = HttpFeed.builder()
                .entity(this)
                .period(1000, TimeUnit.MILLISECONDS)
                .baseUri(String.format("%s/api/v1/hosts", ambariUri))
                .credentials(usernamePasswordCredentials.getUserName(), usernamePasswordCredentials.getPassword())
                .header(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials))
                .poll(new HttpPollConfig<List<String>>(REGISTERED_HOSTS)
                        .onSuccess(Functionals.chain(HttpValueFunctions.jsonContents(), getHosts()))
                        .onFailureOrException(Functions.<List<String>>constant(ImmutableList.<String>of())))
                .build();

        clusterHttpFeed = HttpFeed.builder()
                .entity(this)
                .period(1000, TimeUnit.MILLISECONDS)
                .baseUri(String.format("%s/api/v1/clusters/%s/requests/%d",
                        ambariUri,
                        getConfig(AmbariCluster.CLUSTER_NAME),
                        1))
                .credentials(usernamePasswordCredentials.getUserName(), usernamePasswordCredentials.getPassword())
                .header(HttpHeaders.AUTHORIZATION, HttpTool.toBasicAuthorizationValue(usernamePasswordCredentials))
                .poll(new HttpPollConfig<String>(CLUSTER_STATE)
                        .onSuccess(Functionals.chain(HttpValueFunctions.jsonContents(), getRequestState()))
                        .onFailureOrException(Functions.<String>constant(null)))
                .build();
    }

    private void disconnectAuthenticatedSensors() {
        if (hostsHttpFeed != null) hostsHttpFeed.stop();
        if (clusterHttpFeed != null) clusterHttpFeed.stop();
    }
}
