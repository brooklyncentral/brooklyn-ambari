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
package io.brooklyn.ambari.rest;

import static org.testng.Assert.assertEquals;

import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.brooklyn.api.location.LocationSpec;
import org.apache.brooklyn.api.mgmt.ManagementContext;
import org.apache.brooklyn.core.location.AbstractLocation;
import org.apache.brooklyn.core.test.entity.LocalManagementContextForTests;
import org.apache.brooklyn.util.core.http.BetterMockWebServer;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.RecordedRequest;

import retrofit.RestAdapter;
import retrofit.client.Client;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;

public class AmbariRestClientTest {
    protected BetterMockWebServer server;
    protected URL baseUrl;
    
    private ManagementContext mgmt;

    private Client ambariRestClient;
    private RestAdapter restAdapter;
    private Service service;
    
    private interface Service {
        @GET("/testGetService")
        Response get();

        @POST("/testPostService")
        Response post(@Body List<String> body);
    }

    
    @SuppressWarnings("deprecation")
    @BeforeMethod(alwaysRun = true)
    public void setUp() throws Exception {
        server = BetterMockWebServer.newInstanceLocalhost();
        server.play();

        mgmt = new LocalManagementContextForTests();
        ConcreteLocation location = createConcrete();

        location.addExtension(AmbariHttpExecutorHelper.class, new AmbariHttpExecutorHelper());

        ambariRestClient = AmbariRestClient.builder()
                .httpExecutorFactory(location.getExtension(AmbariHttpExecutorHelper.class))
                .httpExecutorProps(location.getAllConfig(true))
                .build();

        restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://" + server.getHostName() + ":" + server.getPort())
                .setClient(ambariRestClient)
                .build();

        service = restAdapter.create(Service.class);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() throws Exception {
        if (server != null) {
            server.shutdown();
        }
        if (ambariRestClient !=null) {
            ambariRestClient = null;
        }
    }

    @Test
    public void testGetRestCall() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("{}"));
        service.get();

        RecordedRequest request = server.takeRequest();
        assertEquals(request.getPath(),"/testGetService");
        assertEquals(request.getBody(), new byte[0]);
    }

    @Test
    public void testPostRestCall() throws InterruptedException {
        server.enqueue(new MockResponse().setBody("{}"));
        service.post(Arrays.asList("Brooklyn", "Ambari"));

        RecordedRequest request = server.takeRequest();
        assertEquals(request.getPath(),"/testPostService");
        assertEquals(request.getHeader("Content-Length"),"21");
        assertEquals(request.getUtf8Body(), "[\"Brooklyn\",\"Ambari\"]");
    }

    private ConcreteLocation createConcrete() {
        return mgmt.getLocationManager().createLocation(LocationSpec.create(ConcreteLocation.class));
    }

    public static class ConcreteLocation extends AbstractLocation {
        public ConcreteLocation() {
            super();
        }
    }
}
