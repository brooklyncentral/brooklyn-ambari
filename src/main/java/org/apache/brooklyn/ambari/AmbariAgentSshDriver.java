package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.entity.software.SshEffectorTasks;
import brooklyn.location.Location;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.os.Os;
import brooklyn.util.ssh.BashCommands;
import brooklyn.util.task.DynamicTasks;

import static brooklyn.util.ssh.BashCommands.installPackage;
import static brooklyn.util.ssh.BashCommands.sudo;
import static java.lang.String.format;

/**
 * Created by duncangrant on 15/12/14.
 */
public class AmbariAgentSshDriver extends JavaSoftwareProcessSshDriver implements AmbariAgentDriver {
    public AmbariAgentSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    protected String getLogFileLocation() {
        return null;
    }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", false), CHECK_RUNNING)
                .body.append(sudo("ambari-agent status"))
                .execute() == 0;
    }

    @Override
    public void stop() {
        newScript(STOPPING).body.append(sudo("ambari-agent stop")).execute();
    }

    @Override
    public void install() {
        newScript(INSTALLING).body.append(
                BashCommands.INSTALL_WGET,
                sudo("wget http://public-repo-1.hortonworks.com/ambari/ubuntu12/1.x/updates/1.7.0/ambari.list -O /etc/apt/sources.list.d/ambari.list"),
                sudo("apt-key adv --recv-keys --keyserver keyserver.ubuntu.com B9733A7A07513CAD"),
                sudo("apt-get update"),
                installPackage("ambari-agent"))
                .execute();
    }

    @Override
    public void customize() {
        String content = processTemplate(getTemplateConfigurationUrl());
        String tmpConfigFileLoc = "/tmp/ambari-agent.ini";
        String destinationConfigFile = "/etc/ambari-agent/conf/ambari-agent.ini";

        copyTemplate(getTemplateConfigurationUrl(), tmpConfigFileLoc);

        newScript(CUSTOMIZING)
                             .body.append(sudo(format("mv %s %s",tmpConfigFileLoc,destinationConfigFile)))
                .execute();

    }

    @Override
    public void launch() {
        newScript(LAUNCHING).body.append(sudo("ambari-agent start")).execute();
    }

    protected String getTemplateConfigurationUrl() {
        return entity.getConfig(AmbariAgent.TEMPLATE_CONFIGURATION_URL);
    }
}
