package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.AbstractApplication;
import brooklyn.entity.proxying.EntitySpec;
import org.apache.brooklyn.ambari.server.AmbariServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmbariApp extends AbstractApplication {
	
    public static final Logger LOG = LoggerFactory.getLogger(AmbariApp.class);
    
    @Override
    public void init() {
    	AmbariServer server = addChild(EntitySpec.create(AmbariServer.class));

    }
    
}
