package io.brooklyn.ambari.rest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.repackaged.com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

public class AmbariConfig {

    private boolean hasHosts = false;
    private Map<String, List<String>> hostGroups = new HashMap<String, List<String>>();
    private Map<String, List<String>> hostGroupsToHosts = new HashMap<String, List<String>>();

    public void add(
            String groupName,
            List<String> hostFQDNs,
            List<String> components) {
        Preconditions.checkNotNull(hostFQDNs, "No hosts in list");
        Preconditions.checkNotNull(components, "No components to install");
        hostGroups.put(groupName, ImmutableList.copyOf(components));
        hostGroupsToHosts.put(groupName, ImmutableList.copyOf(hostFQDNs));
        hasHosts = true;
    }

    public boolean hasHostGroups() {
        return hasHosts;
    }

    public Map<String, List<String>> getHostGroups() {
        return hostGroups;
    }

    public Map<String, List<String>> getHostGroupsToHosts() {
        return hostGroupsToHosts;
    }

}
