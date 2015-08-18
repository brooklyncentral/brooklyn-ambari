package io.brooklyn.ambari;

import brooklyn.launcher.blueprints.AbstractBlueprintTest;
import brooklyn.util.ResourceUtils;
import brooklyn.util.collections.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.StringReader;
import java.util.Map;

public class AmbariBlueprintLiveTest extends AbstractBlueprintTest {

    private static final Logger LOG = LoggerFactory.getLogger(AmbariBlueprintLiveTest.class);

    @DataProvider(name = "providerData")
    public Object[][] providerData(){

        return new Object[][]{
                new Object[] {
                        "ambari-cluster.yaml", "aws-ec2", "us-east-1", "UbuntuOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[] {
                        "ambari-cluster.yaml", "aws-ec2", "us-east-1", "CentOSOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
                        .put("osFamily", "centos")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[] {
                        "ambari-cluster.yaml", "aws-ec2", "us-east-1", "RHEDOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
                        .put("osFamily", "rhel")
                        .put("osVersionRegex", "6.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[] {
                        "ambari-cluster-by-hostgroup.yaml", "aws-ec2", "us-east-1", "UbuntuOnAWS", MutableMap.<String, String>builder()
                        .put("minRam", "8192")
                        .put("osFamily", "ubuntu")
                        .put("osVersionRegex", "12.*")
                        .put("stopIptables", "true")
                        .build()
                },
                new Object[] {
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

    private String buildLocation(String provider, String region, Map<String, String> options) {
        StringBuffer sb = new StringBuffer("location:\n").append(String.format("  %s:%s:\n", provider, region));
        for (Map.Entry<String, String> o : options.entrySet()) {
            sb.append(String.format("    %s: %s\n", o.getKey(), o.getValue()));
        }

        return sb.toString();
    }
}
