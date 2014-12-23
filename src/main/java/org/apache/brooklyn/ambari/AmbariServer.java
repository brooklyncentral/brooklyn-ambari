package org.apache.brooklyn.ambari;

import brooklyn.catalog.Catalog;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.java.UsesJava;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.util.time.Duration;

@Catalog(name="Ambari Server", description="Ambari Server: part of an ambari cluster used to install and monitor a hadoop cluster.")
@ImplementedBy(AmbariServerImpl.class)
public interface AmbariServer extends SoftwareProcess, UsesJava {

    /**
     * @throws IllegalStateException if times out.
     */
    public void waitForServiceUp();
}
