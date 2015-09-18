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

import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.base.Predicates;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import brooklyn.entity.Application;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.rebind.RebindOptions;
import brooklyn.entity.rebind.RebindTestUtils;
import brooklyn.launcher.blueprints.AbstractBlueprintTest;
import brooklyn.management.ManagementContext;
import brooklyn.management.internal.LocalManagementContext;
import brooklyn.test.EntityTestUtils;
import brooklyn.util.ResourceUtils;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.time.Duration;
import io.brooklyn.ambari.server.AmbariServer;

@Test(singleThreaded = false, threadPoolSize = 2)
public class AmbariBlueprintLiveTest extends AbstractBlueprintTest {

    private static final Logger LOG = LoggerFactory.getLogger(AmbariBlueprintLiveTest.class);

    // Move setup behaviour to class so that we can run tests against single brooklyn instance
    @BeforeClass(alwaysRun = true)
    public void setupClass() throws Exception {
        super.setUp();
    }

    @BeforeMethod
    @Override
    public void setUp() throws Exception {
        // Do nothing
    }

    @AfterClass(alwaysRun = true)
    public void tearDownClass() throws Exception {
        super.tearDown();
    }

    @AfterMethod
    @Override
    public void tearDown() throws Exception {
        //Do nothing
    }

    @Override
    protected void runTest(Reader yaml) throws Exception {
        Application app = this.launcher.launchAppYaml(yaml);
        this.assertNoFires(app);
        Application newApp = this.rebind(app, RebindOptions.create());
        this.assertNoFires(newApp);
        this.assertHadoopClusterEventuallyDeployed(newApp);
    }

    @DataProvider(name = "providerData", parallel = true)
    public Object[][] providerData() {

        return new Object[][]{
                new Object[]{
                        "ambari-cluster.yaml", "softlayer", "ams01", "UbuntuOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster.yaml", "softlayer", "ams03", "CentOSOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "centos")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster.yaml", "softlayer", "lon02", "RHEDOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "rhel")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster-by-hostgroup.yaml", "softlayer", "sea01", "UbuntuOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster-w-extra-services.yaml", "softlayer", "dal05", "UbuntuOnSoftlayer", MutableMap.<String, String>builder()
                        .put("minRam", "16384")
                        .put("minCores", "4")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                }
        };
    }

    @Test(groups = {"Live"}, dataProvider = "providerData")
    public void testYamlBlueprint(String yamlFile, String provider, String region, String description, Map<String, String> options) throws Exception {
        LOG.info("Testing {} on {}:{} using {} ({})", new Object[]{yamlFile, provider, region, description, options});

        String yaml = buildLocation(provider, region, options) + new ResourceUtils(this).getResourceAsString(yamlFile);

        runTest(new StringReader(yaml));
    }

    protected Application rebind(Application currentApp, RebindOptions options) throws Exception {
        ManagementContext origMgmt = this.mgmt;
        LocalManagementContext newMgmt = this.createNewManagementContext();
        options = RebindOptions.create(options);
        if(options.classLoader == null) {
            options.classLoader(this.classLoader);
        }

        if(options.mementoDir == null) {
            options.mementoDir(this.mementoDir);
        }

        if(options.origManagementContext == null) {
            options.origManagementContext(origMgmt);
        }

        if(options.newManagementContext == null) {
            options.newManagementContext(newMgmt);
        }

        RebindTestUtils.waitForPersisted(currentApp);

        this.mgmt = options.newManagementContext;
        final Collection<Application> newApps = RebindTestUtils.rebindAll(options);
        for (Application newApp : newApps) {
            if (newApp.getId().equals(currentApp.getId())) {
                return newApp;
            }
        }

        throw new IllegalStateException("Application could not be rebinded; serialization probably failed");
    }

    protected void assertHadoopClusterEventuallyDeployed(Application newApp) {
        AmbariServer ambariServer = Entities.descendants(newApp, AmbariServer.class).iterator().next();
        EntityTestUtils.assertAttributeEventually(
                ImmutableMap.of("timeout", Duration.minutes(60)),
                ambariServer,
                AmbariServer.CLUSTER_STATE,
                Predicates.and(
                        Predicates.equalTo("COMPLETED"),
                        Predicates.not(Predicates.in(ImmutableList.of(
                                "ABORTED",
                                "FAILED",
                                "TIMEDOUT"
                        )))));
    }

    private String buildLocation(String provider, String region, Map<String, String> options) {
        StringBuffer sb = new StringBuffer("location:\n").append(String.format("  %s:%s:\n", provider, region));
        for (Map.Entry<String, String> o : options.entrySet()) {
            sb.append(String.format("    %s: %s\n", o.getKey(), o.getValue()));
        }

        return sb.toString();
    }
}
