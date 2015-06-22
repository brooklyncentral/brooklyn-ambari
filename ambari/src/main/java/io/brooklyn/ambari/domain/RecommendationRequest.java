package io.brooklyn.ambari.domain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;

import org.codehaus.jackson.annotate.JsonProperty;

public class RecommendationRequest {

    @JsonProperty
    private final Collection<String> hosts;

    @JsonProperty
    private final Collection<String> services;

    @JsonProperty
    private final String recommend = "host_groups";

    public RecommendationRequest(Collection<String> hosts, Collection<String> services) {
        this.hosts = checkNotNull(hosts, "hosts");
        this.services = checkNotNull(services, "services");
    }

}
