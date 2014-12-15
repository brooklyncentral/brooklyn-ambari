package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.entity.webapp.JavaWebAppSoftwareProcessImpl;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.ssh.BashCommands;

import static brooklyn.util.ssh.BashCommands.installPackage;

public class AmbariServerSshDriver extends JavaSoftwareProcessSshDriver implements AmbariServerDriver {


    public AmbariServerSshDriver(EntityLocal entity, SshMachineLocation machine) {
        super(entity, machine);
    }

    @Override
    public boolean isRunning() {
        return newScript(MutableMap.of("usePidFile", false), CHECK_RUNNING)
                .body.append(BashCommands.sudo("ambari-server status"))
                .execute() == 0;
    }

    @Override
    protected String getLogFileLocation() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void stop() {
        newScript(STOPPING).body.append(BashCommands.sudo("ambari-server stop")).execute();
    }

    @Override
    public void install() {
        newScript(INSTALLING).body.append(
                BashCommands.INSTALL_WGET,
                BashCommands.sudo("wget http://public-repo-1.hortonworks.com/ambari/ubuntu12/1.x/updates/1.7.0/ambari.list -O /etc/apt/sources.list.d/ambari.list"),
                BashCommands.sudo("apt-key adv --recv-keys --keyserver keyserver.ubuntu.com B9733A7A07513CAD"),
                BashCommands.sudo("apt-get update"),
                installPackage("ambari-server"),
                BashCommands.sudo("ambari-server setup -s"))
                .execute();
    }

    @Override
    public void customize() {
        // TODO Auto-generated method stub
    }

    @Override
    public void launch() {
        newScript(LAUNCHING).body.append(BashCommands.sudo("ambari-server start")).execute();
    }

}
