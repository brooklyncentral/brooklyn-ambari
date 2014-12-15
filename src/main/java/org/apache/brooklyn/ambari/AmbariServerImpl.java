package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.Attributes;
import brooklyn.entity.basic.SoftwareProcessImpl;
import brooklyn.entity.webapp.JavaWebAppSoftwareProcessImpl;
import brooklyn.event.feed.http.HttpFeed;
import brooklyn.event.feed.http.HttpPollConfig;
import brooklyn.event.feed.http.HttpValueFunctions;
import brooklyn.location.access.BrooklynAccessUtils;
import com.google.common.base.Functions;
import com.google.common.net.HostAndPort;

import java.net.URI;
import java.util.concurrent.TimeUnit;

public class AmbariServerImpl extends SoftwareProcessImpl implements AmbariServer {

    private volatile HttpFeed httpFeed;

    @Override
    public Class getDriverInterface() {
        return AmbariServerDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();

        //TODO need configured param not 8080
        HostAndPort hp = BrooklynAccessUtils.getBrooklynAccessibleAddress(this, 8080);

        String ambariUri = String.format("http://%s:%d/", hp.getHostText(), hp.getPort());
        setAttribute(Attributes.MAIN_URI, URI.create(ambariUri));

        httpFeed = HttpFeed.builder()
                .entity(this)
                .period(500, TimeUnit.MILLISECONDS)
                .baseUri(ambariUri)
                .poll(new HttpPollConfig<Boolean>(SERVICE_UP)
                        .onSuccess(HttpValueFunctions.responseCodeEquals(200))
                        .onFailureOrException(Functions.constant(false)))
                .build();
    }

    @Override
    public void disconnectSensors() {
        super.disconnectSensors();

        if (httpFeed != null) httpFeed.stop();
    }
}
