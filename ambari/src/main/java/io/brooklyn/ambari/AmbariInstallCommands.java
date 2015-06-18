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
package io.brooklyn.ambari;

import static brooklyn.util.ssh.BashCommands.alternatives;
import static brooklyn.util.ssh.BashCommands.chainGroup;
import static brooklyn.util.ssh.BashCommands.commandToDownloadUrlAs;
import static brooklyn.util.ssh.BashCommands.ifExecutableElse1;
import static brooklyn.util.ssh.BashCommands.installExecutable;
import static brooklyn.util.ssh.BashCommands.sudo;

import brooklyn.location.OsDetails;
import brooklyn.location.basic.SshMachineLocation;
import brooklyn.util.ssh.BashCommands;

public class AmbariInstallCommands {

    private static final String CENTOS_6_AMBARI_REPO_LOCATION = "http://public-repo-1.hortonworks.com/ambari/centos6/%s/updates/%s/ambari.repo";
    private static final String CENTOS_REPO_LIST_LOCATION = "/etc/yum.repos.d/ambari.repo";
    private static final String CENTOS_5_AMBARI_REPO_LOCATION = "http://public-repo-1.hortonworks.com/ambari/centos5/%s/updates/%s/ambari.repo";

    private static final String SUSE_REPO_LIST_LOCATION = "/etc/zypp/repos.d/ambari.repo";
    private static final String SUSE_AMBARI_REPO_LOCATION = "http://public-repo-1.hortonworks.com/ambari/suse11/%s/updates/%s/ambari.repo";

    private static final String UBUNTU_REPO_LIST_LOCATION = "/etc/apt/sources.list.d/ambari.list";
    private static final String UBUNTU_AMBARI_REPO_LOCATION = "http://public-repo-1.hortonworks.com/ambari/ubuntu12/%s/updates/%s/ambari.list";
    private String version;

    public AmbariInstallCommands(String version) {
        this.version = version;
    }

    public String installAmbariRequirements(SshMachineLocation machine) {
        return BashCommands.chainGroup(BashCommands.INSTALL_CURL,
                installExecutable("ntp"),
                createCommandToAddAmbariToRepositoriesList(machine));
    }

    private String createCommandToAddAmbariToRepositoriesList(SshMachineLocation sshMachineLocation) {
        return alternatives(getAptRepo(), setupCentos6Repo(sshMachineLocation), setupSuseRepo());
    }

    private String getAptRepo() {
        return ifExecutableElse1("apt-get", chainGroup(sudo(commandToDownloadUrlAs(String.format(UBUNTU_AMBARI_REPO_LOCATION, getMajorVersion(), version), UBUNTU_REPO_LIST_LOCATION)),
                sudo("apt-key adv --recv-keys --keyserver keyserver.ubuntu.com B9733A7A07513CAD"),
                sudo("apt-get update")));
    }

    private String setupCentos6Repo(SshMachineLocation sshMachineLocation) {
        // Doesn't check machine name as may refer to redhat, centos, or oracle
        String osDetailsVersion = getOsVersion(sshMachineLocation);
        if (osDetailsVersion.startsWith("6")) {
            return ifExecutableElse1("yum", sudo(commandToDownloadUrlAs(String.format(CENTOS_6_AMBARI_REPO_LOCATION, getMajorVersion(), version), CENTOS_REPO_LIST_LOCATION)));
        } else {
            return ifExecutableElse1("yum", sudo(commandToDownloadUrlAs(String.format(CENTOS_5_AMBARI_REPO_LOCATION, getMajorVersion(), version), CENTOS_REPO_LIST_LOCATION)));
        }
    }

    private String setupSuseRepo() {
        return ifExecutableElse1("zypper", sudo(commandToDownloadUrlAs(String.format(SUSE_AMBARI_REPO_LOCATION, getMajorVersion(), version), SUSE_REPO_LIST_LOCATION)));
    }

    private String getOsVersion(SshMachineLocation sshMachineLocation) {
        if (sshMachineLocation == null) {
            return "";
        }
        OsDetails osDetails = sshMachineLocation.getOsDetails();
        return osDetails != null ? osDetails.getVersion() : "";
    }

    private String getMajorVersion() {
        return version.charAt(0) + ".x";
    }
}
