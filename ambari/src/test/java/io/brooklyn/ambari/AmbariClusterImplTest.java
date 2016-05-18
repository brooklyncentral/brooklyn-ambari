package io.brooklyn.ambari;

import static org.apache.brooklyn.test.Asserts.assertThat;
import static org.apache.brooklyn.util.collections.CollectionFunctionals.contains;
import static org.testng.AssertJUnit.assertEquals;

import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import com.google.common.collect.ImmutableMap;

public class AmbariClusterImplTest {
    private AmbariClusterImpl ambariCluster = new AmbariClusterImpl();

    private Map<String, Map> testMap;
    private Set<String> keys;

    @SuppressWarnings("unchecked")
    @BeforeClass(alwaysRun = true)
    public void setUpClass() throws Exception {
        ImmutableMap<String, Map> origMap = ImmutableMap.<String, Map>builder()
                .put("oozie-site", ImmutableMap.builder()
                        .put("oozie.service.ProxyUserService.proxyuser.falcon.groups", "*")
                        .put("oozie.service.ProxyUserService.proxyuser.falcon.hosts", "*")
                        .put("oozie.service.ProxyUserService.proxyuser.hue.groups", "*")
                        .put("oozie.service.ProxyUserService.proxyuser.hue.hosts", "*")
                        .build())
                .build();

        ImmutableMap<String, Map> newMap = ImmutableMap.<String, Map>builder()
                .put("oozie-site",  ImmutableMap.builder()
                        .put("oozie.db.schema.name", "oozie")
                        .put("oozie.service.JPAService.jdbc.driver", "org.postgresql.Driver")
                        .put("oozie.service.ProxyUserService.proxyuser.hue.hosts", "localhost")
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
    public void testNewMergedValue() {
        assertEquals(testMap.get("oozie-site").get("oozie.service.ProxyUserService.proxyuser.hue.hosts"), "localhost");
    }
}
