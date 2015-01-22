package org.apache.brooklyn.ambari.agent;

import brooklyn.catalog.Catalog;
import brooklyn.config.ConfigKey;
import brooklyn.entity.basic.ConfigKeys;
import brooklyn.entity.basic.SoftwareProcess;
import brooklyn.entity.java.UsesJava;
import brooklyn.entity.proxying.ImplementedBy;
import brooklyn.util.flags.SetFromFlag;
import brooklyn.util.javalang.JavaClassNames;

/**
 * Created by duncangrant on 15/12/14.
 */
@Catalog(name="Ambari Agent", description="Ambari Agent: part of an ambari cluster that runs on each node that will form part of the Hadoop cluster")
@ImplementedBy(AmbariAgentImpl.class)
public interface AmbariAgent extends SoftwareProcess, UsesJava {

    @SetFromFlag("configFileUrl")
    ConfigKey<String> TEMPLATE_CONFIGURATION_URL = ConfigKeys.newConfigKey(
            "ambari.templateConfigurationUrl", "Template file (in freemarker format) for the ambari-agent.ini file",
            JavaClassNames.resolveClasspathUrl(AmbariAgent.class, "ambari-agent.ini"));

// TODO remove Random
    @SetFromFlag("ambariServerFQDN")
    ConfigKey<String> AMBARI_SERVER_FQDN = ConfigKeys.newStringConfigKey(
            "ambari.server.fqdn", "Fully Qualified Domain Name of ambari server that agent should register to", "Random");
}
