package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static brooklyn.util.ssh.BashCommands.installPackage;
import static brooklyn.util.ssh.BashCommands.sudo;
import static java.lang.String.format;

class AmbariAgentSshDriver extends JavaSoftwareProcessSshDriver implements AmbariAgentDriver {
    public static final Logger log = LoggerFactory.getLogger(AmbariAgentSshDriver.class);
    private final DefaultAmbariInstallHelper defaultAmbariInstallHelper = new DefaultAmbariInstallHelper();

    public AmbariAgentSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    protected String getLogFileLocation() {
        return "/var/log/ambari-agent/ambari-agent.log";
    }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", false), CHECK_RUNNING).body.append(sudo("ambari-agent status")).execute() == 0;
    }

    @Override
    public void stop() {
        newScript(STOPPING).body.append(sudo("ambari-agent stop")).execute();
    }

    @Override
    public void install() {
        newScript(INSTALLING).body.append(
                defaultAmbariInstallHelper.installAmbariRequirements(getMachine()),
                installPackage("ambari-agent"))
                .execute();
    }

    @Override
    public void customize() {
        String tmpConfigFileLoc = "/tmp/ambari-agent.ini";
        String destinationConfigFile = "/etc/ambari-agent/conf/ambari-agent.ini";

        copyTemplate(getTemplateConfigurationUrl(), tmpConfigFileLoc);

        newScript(CUSTOMIZING)
                .body.append(sudo(format("mv %s %s", tmpConfigFileLoc, destinationConfigFile)))
                .execute();

    }

    @Override
    public void launch() {
        newScript(LAUNCHING).body.append(sudo("ambari-agent start")).execute();
    }

    String getTemplateConfigurationUrl() {
        return entity.getConfig(AmbariAgent.TEMPLATE_CONFIGURATION_URL);
    }

}
