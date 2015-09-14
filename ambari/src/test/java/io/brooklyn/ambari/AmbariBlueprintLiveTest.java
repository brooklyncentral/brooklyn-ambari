package io.brooklyn.ambari;

import java.io.Reader;
import java.io.StringReader;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;

import brooklyn.entity.Application;
import brooklyn.entity.basic.Entities;
import brooklyn.launcher.blueprints.AbstractBlueprintTest;
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

    protected void runTest(Reader yaml) throws Exception {
        Application app = this.launcher.launchAppYaml(yaml);
        this.assertNoFires(app);
        Application newApp = this.rebind();
        this.assertNoFires(newApp);
        assertHadoopClusterEventuallyDeployed(newApp);
    }

    @DataProvider(name = "providerData", parallel = true)
    public Object[][] providerData() {

        return new Object[][]{
                new Object[]{
                        "ambari-cluster.yaml", "aws-ec2", "us-east-1", "UbuntuOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster.yaml", "aws-ec2", "us-west-1", "CentOSOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
                        .put("osFamily", "centos")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster.yaml", "aws-ec2", "eu-west-1", "RHEDOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
                        .put("osFamily", "rhel")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster-by-hostgroup.yaml", "aws-ec2", "eu-west-1", "UbuntuOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[]{
                        "ambari-cluster-w-extra-services.yaml", "aws-ec2", "us-east-1", "UbuntuOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
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

    private void assertHadoopClusterEventuallyDeployed(Application newApp) {
        AmbariServer ambariServer = Entities.descendants(newApp, AmbariServer.class).iterator().next();
        EntityTestUtils.assertAttributeEqualsEventually(
                ImmutableMap.of("timeout", Duration.minutes(60)),
                ambariServer, AmbariServer.CLUSTER_STATE,
                "COMPLETED");
    }

    private String buildLocation(String provider, String region, Map<String, String> options) {
        StringBuffer sb = new StringBuffer("location:\n").append(String.format("  %s:%s:\n", provider, region));
        for (Map.Entry<String, String> o : options.entrySet()) {
            sb.append(String.format("    %s: %s\n", o.getKey(), o.getValue()));
        }

        return sb.toString();
    }
}
