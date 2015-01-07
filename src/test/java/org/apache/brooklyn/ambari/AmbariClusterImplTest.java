package org.apache.brooklyn.ambari;

import brooklyn.entity.Entity;
import brooklyn.event.Sensor;
import brooklyn.event.SensorEvent;
import com.google.common.collect.ImmutableList;
import org.apache.brooklyn.ambari.testdoubles.AmbariServerStub;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.LinkedList;
import java.util.List;

import static org.testng.Assert.*;

public class AmbariClusterImplTest {

    public static final int ONCE = 1;
    public static final int TWICE = 2;
    private static AmbariServerSpy ambariServerSpy;
    private AmbariClusterImpl ambariClusterUnderTest;
    private ImmutableList<String> hosts;

    @BeforeMethod
    public void setUp() throws Exception {
        ambariServerSpy = new AmbariServerSpy();
        ambariClusterUnderTest = new AmbariClusterImpl();
        ambariClusterUnderTest.setAttribute(ambariClusterUnderTest.AMBARI_SERVER, ambariServerSpy);
    }

    @Test
    public void testEmitListWithOneItemCallsRegisterOnce() throws Exception {
        emitRegisteredHostsEvent("host1");
        assertHostsRegistered(ONCE);
    }

    @Test
    public void testEmitListWithTwoItemsCallsRegisterTwice() throws Exception {
        emitRegisteredHostsEvent("host1", "host2");
        assertHostsRegistered(TWICE);
        assertAmbariApiHelperSpyCalledWith("host1", "host2");
    }

    @Test
    public void testEmitListWithOneOldAndOneNewItemOnlyEmitsNewItem() throws Exception {
        emitRegisteredHostsEvent("host1");
        ambariServerSpy.initialiseFields();
        emitRegisteredHostsEvent("host1", "host2");
        assertAmbariApiHelperSpyNotCalledWith("host1");
        assertAmbariApiHelperSpyCalledWith("host2");
    }

    private static class AmbariServerSpy extends AmbariServerStub {

        public boolean wasCalled;

        public List<String> calledWith;

        public AmbariServerSpy() {
            initialiseFields();
        }

        @Override
        public void addHostToCluster(String cluster, String host) {
            wasCalled = true;
            calledWith.add(host);
        }

        public void initialiseFields() {
            wasCalled = false;
            calledWith = new LinkedList<String>();
        }
    }

    private SensorEvent<List<String>> event = new SensorEvent<List<String>>() {
        @Override
        public Entity getSource() {
            return null;
        }

        @Override
        public Sensor<List<String>> getSensor() {
            return null;
        }

        @Override
        public List<String> getValue() {
            return hosts;
        }

        @Override
        public long getTimestamp() {
            return 0;
        }
    };

    private void assertAmbariApiHelperSpyCalledWith(String... hosts) {
        for (String host : hosts) {
            assertTrue(ambariServerSpy.calledWith.contains(host));
        }
    }

    private void assertAmbariApiHelperSpyNotCalledWith(String... hosts) {
        for (String host : hosts) {
            assertFalse(ambariServerSpy.calledWith.contains(host));
        }
    }


    private void emitRegisteredHostsEvent(String... host1) {
        hosts = ImmutableList.<String>copyOf(host1);
        ambariClusterUnderTest.registeredHostsEventListener.onEvent(event);
    }

    private void assertHostsRegistered(int registrationCount) {
        assertEquals(ambariServerSpy.calledWith.size(), registrationCount);
    }

}