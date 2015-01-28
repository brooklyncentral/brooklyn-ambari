package org.apache.brooklyn.ambari.rest;

import brooklyn.util.http.HttpToolResponse;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class AmbariApiErrorTest {

    @Test
    public void testName() throws Exception {
        assertEquals(AmbariApiError.messageFormat.format(AmbariApiError.ERROR_MESSAGE, "fdsfs", "Dsfsd"), "Unnaceptable Response Code from Ambari Rest API fdsfs\n" +
                "Message from Server Dsfsd");

    }
}