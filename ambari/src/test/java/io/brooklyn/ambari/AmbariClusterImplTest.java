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

import static org.apache.brooklyn.test.Asserts.assertThat;
import static org.apache.brooklyn.util.collections.CollectionFunctionals.contains;
import static org.testng.AssertJUnit.assertEquals;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.google.common.collect.ImmutableMap;
import io.brooklyn.ambari.AmbariClusterImpl;

public class AmbariClusterImplTest {
    private AmbariClusterImpl ambariCluster = new AmbariClusterImpl();

    private Map<String, Map> testMap;
    private Set<String> keys;
    
    private ImmutableMap<String, Map> newRootMap;
    ImmutableMap<String, Map> origMap;
    ImmutableMap<String, Map> newMap;

    @SuppressWarnings("unchecked")
    @BeforeClass(alwaysRun = true)
    public void setUpClass() throws Exception {
        origMap = ImmutableMap.<String, Map>builder()
                .put("oozie-site", ImmutableMap.builder()
                        .put("oozie.service.ProxyUserService.proxyuser.falcon.groups", "*")
                        .put("oozie.service.ProxyUserService.proxyuser.falcon.hosts", "*")
                        .put("oozie.service.ProxyUserService.proxyuser.hue.groups", "*")
                        .put("oozie.service.ProxyUserService.proxyuser.hue.hosts", "*")
                        .build())
                .build();

        newMap = ImmutableMap.<String, Map>builder()
                .put("oozie-site",  ImmutableMap.builder()
                        .put("oozie.db.schema.name", "oozie")
                        .put("oozie.service.JPAService.jdbc.driver", "org.postgresql.Driver")
                        .put("oozie.service.ProxyUserService.proxyuser.hue.hosts", "localhost")
                        .build())
                .build();

        newRootMap = ImmutableMap.<String, Map>builder()
                .put("hdfs-site", ImmutableMap.builder()
                        .put("dfs.webhdfs.enabled", "true")
                        .put("dfs.permissions.enabled", "false")
                        .put("dfs.namenode.acls.enabled", "true")
                        .build())
                .build();

        testMap = ambariCluster.mergeMaps(origMap, newMap);
        keys = testMap.get("oozie-site").keySet();
    }

    @Test
    public void testNumberOfMergedValues() {
        assertEquals(6, testMap.get("oozie-site").size());
    }

    @Test
    public void testPresenceOfFirstMap() {
        assertThat(keys, contains("oozie.service.ProxyUserService.proxyuser.hue.groups"));
    }

    @Test
    public void testPresenceOfSecondMap() {
        assertThat(keys, contains("oozie.service.JPAService.jdbc.driver"));
    }

    @Test
    public void testMergedValues() {
        assertEquals(testMap.get("oozie-site").get("oozie.service.JPAService.jdbc.driver"), "org.postgresql.Driver");
    }

    @Test
    public void testOriginalValueHasPrecedence() {
        assertEquals(testMap.get("oozie-site").get("oozie.service.ProxyUserService.proxyuser.hue.hosts"), "*");
    }

    @Test
    public void testAddingNewRootMap() {
        Map<String, Map> rootMap = ambariCluster.mergeMaps(testMap, newRootMap);
        
        assertEquals(rootMap.get("oozie-site").get("oozie.service.ProxyUserService.proxyuser.hue.hosts"), "*");
        assertEquals(rootMap.get("hdfs-site").get("dfs.webhdfs.enabled"), "true");
    }

    @Test
    public void testAddingNullNewMap() {
        Map<String, Map> tmpMap = ambariCluster.mergeMaps(origMap, null);
        
        assertEquals(tmpMap.get("oozie-site").get("oozie.service.ProxyUserService.proxyuser.hue.hosts"), "*");
    }

    @Test
    public void testAddingNullOrigMap() {
        Map<String, Map> tmpMap = ambariCluster.mergeMaps(null, newMap);
        
        assertEquals(tmpMap.get("oozie-site").get("oozie.service.ProxyUserService.proxyuser.hue.hosts"), "localhost");
    }
}
