package org.apache.brooklyn.ambari.agent;

import brooklyn.entity.basic.SoftwareProcessImpl;
import org.apache.brooklyn.ambari.agent.AmbariAgent;
import org.apache.brooklyn.ambari.agent.AmbariAgentDriver;

/**
 * Created by duncangrant on 15/12/14.
 */
public class AmbariAgentImpl extends SoftwareProcessImpl implements AmbariAgent {
    @Override
    public Class getDriverInterface() {
        return AmbariAgentDriver.class;
    }

    @Override
    protected void connectSensors() {
        super.connectSensors();

        //TODO - Need to wire isrunning to service up (I think)
        setAttribute(SERVICE_UP, true);
    }

    @Override
    protected void disconnectSensors() {
        super.disconnectSensors();
    }
    public String getAmbariServerFQDN() {
        return getConfig(AMBARI_SERVER_FQDN);
    }
}
