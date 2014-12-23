package org.apache.brooklyn.ambari;

import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.java.JavaSoftwareProcessSshDriver;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.collections.MutableMap;
import brooklyn.util.ssh.BashCommands;

import static brooklyn.util.ssh.BashCommands.installPackage;

public class AmbariServerSshDriver extends JavaSoftwareProcessSshDriver implements AmbariServerDriver {

    private final DefaultAmbariInstallHelper defaultAmbariInstallHelper = new DefaultAmbariInstallHelper();

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
        return "/var/log/ambari-server/ambari-server.log";
    }

    @Override
    public void stop() {
        newScript(STOPPING).body.append(BashCommands.sudo("ambari-server stop")).execute();
    }

    @Override
    public void install() {
        newScript(INSTALLING).body.append(
                defaultAmbariInstallHelper.installAmbariRequirements(getMachine()),
                installPackage("ambari-server"),
                BashCommands.sudo("ambari-server setup -s"))
                .execute();
    }

    @Override
    public void customize() {
    }

    @Override
    public void launch() {
        newScript(LAUNCHING).body.append(BashCommands.sudo("ambari-server start")).execute();
    }

    @Override
    public void postLaunch() {
        super.postLaunch();
    }
}
