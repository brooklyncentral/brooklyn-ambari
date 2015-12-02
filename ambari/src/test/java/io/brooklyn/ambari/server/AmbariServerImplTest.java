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

import static org.apache.brooklyn.test.Asserts.assertThat;
import static org.apache.brooklyn.util.collections.CollectionFunctionals.contains;
import static org.apache.brooklyn.util.collections.CollectionFunctionals.sizeEquals;
import static org.testng.AssertJUnit.assertEquals;

import java.util.List;

import org.testng.annotations.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class AmbariServerImplTest {

    private AmbariServerImpl ambariServer = new AmbariServerImpl();

    @Test
    public void testEmptyJsonThrows() {
        assertThat(getHostsFromJson("{}"), sizeEquals(0));
    }

    @Test(expectedExceptions = NullPointerException.class)
    public void testNullJsonThrows() {
        assertThat(ambariServer.getHosts().apply(null), sizeEquals(0));
    }

    @Test
    public void testOneHostReturnsSingleItemInList() {
        List<String> hosts = getHostsFromJson(JSON_WITH_ONE_HOST);

        assertEquals(1, hosts.size());
        assertThat(hosts, contains("ip-10-121-18-69.eu-west-1.compute.internal"));
    }

    @Test
    public void testFourHostsReturnsFourItemsInList() {
        List<String> hosts = getHostsFromJson(JSON_WITH_FOUR_HOSTS);

        assertEquals(4, hosts.size());
        assertThat(hosts, contains("ip-10-121-18-69.eu-west-1.compute.internal"));
        assertThat(hosts, contains("ip-10-121-20-75.eu-west-1.compute.internal"));
        assertThat(hosts, contains("ip-10-122-4-179.eu-west-1.compute.internal"));
        assertThat(hosts, contains("ip-10-91-154-171.eu-west-1.compute.internal"));
    }

    @Test
    public void testClusterStateIsreturned() {
        String clusterState = ambariServer.getRequestState().apply(getAsJsonObject(JSON_CLUSTER_STATE));

        assertEquals("IN_PROGRESS", clusterState);
    }

    private List<String> getHostsFromJson(String json) {
        return ambariServer.getHosts().apply(getAsJsonObject(json));
    }

    private JsonObject getAsJsonObject(String jsonWithOneHost) {
        return new JsonParser().parse(jsonWithOneHost).getAsJsonObject();
    }

    private static final String JSON_CLUSTER_STATE = "{\n" +
            "  \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/cluster/c1/requests/1\",\n" +
            "  \"Requests\" : {\n" +
            "    \"cluster_name\" : \"c1\",\n" +
            "    \"request_context\" : \"My context\",\n" +
            "    \"request_status\" : \"IN_PROGRESS\",\n" +
            "    \"id\" : \"123456789\"\n" +
            "  }\n" +
            "}";

    private static final String JSON_WITH_ONE_HOST = "{\n" +
            "  \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts\",\n" +
            "  \"items\" : [\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-121-18-69.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-121-18-69.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    private static final String JSON_WITH_FOUR_HOSTS = "{\n" +
            "  \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts\",\n" +
            "  \"items\" : [\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-121-18-69.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-121-18-69.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-121-20-75.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-121-20-75.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-122-4-179.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-122-4-179.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"href\" : \"http://ec2-54-228-116-93.eu-west-1.compute.amazonaws.com:8080/api/v1/hosts/ip-10-91-154-171.eu-west-1.compute.internal\",\n" +
            "      \"Hosts\" : {\n" +
            "        \"host_name\" : \"ip-10-91-154-171.eu-west-1.compute.internal\"\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";

}