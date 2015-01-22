package org.apache.brooklyn.ambari.rest;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.apache.brooklyn.ambari.rest.DefaultAmbariApiHelper;
import org.apache.brooklyn.ambari.rest.RecommendationResponse;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.codehaus.jackson.map.ObjectMapper;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import static org.testng.Assert.*;

public class DefaultAmbariApiHelperTest {

    Map<String, Boolean> hosts = ImmutableMap.<String, Boolean>of(
            "u1202.ambari.apache.org", true,
            "u1203.ambari.apache.org", true,
            "u1204.ambari.apache.org", true,
            "u1205.ambari.apache.org", true);

    private Iterable<String> services = ImmutableList.<String>of("ZOOKEEPER", "HDFS");
    private UsernamePasswordCredentials usernamePasswordCredentials = new UsernamePasswordCredentials("admin", "admin");
    private URI baseUri = URI.create("http://u1201.ambari.apache.org:8080/");

    @Test
    public void testGetRecommendations() throws Exception {
        DefaultAmbariApiHelper defaultAmbariApiHelper = new DefaultAmbariApiHelper();
        RecommendationResponse recommendations = defaultAmbariApiHelper.getRecommendations(hosts.keySet(), services, usernamePasswordCredentials, baseUri);
        assertEquals(recommendations.resources[0].recommendations.blueprint.host_groups[0].name, "ddsfsf");
    }

    @Test
    public void testStuff() {
        ObjectMapper objectMapper = new ObjectMapper();
        MyClass myClass;
        try {
            myClass = objectMapper.readValue("{" +
                    "  \"resources\" : [{\"test\":\"test\"}]}"
                    , MyClass.class);
        } catch (IOException e) {
            e.printStackTrace();
            assertFalse(true);
            return;
        }
        assertEquals(myClass.resources[0].get("test"), "test");
    }

    static class MyClass {
        public Map[] resources;
    }

    static class Resource{
        public String test;
    }
}