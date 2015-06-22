package io.brooklyn.ambari.domain;

import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.annotate.JsonProperty;

import com.google.common.collect.ImmutableList;

/**
 * See org.apache.ambari.server.api.handlers.BaseManagementHandler ("Base
 * handler for operations that persist state to the back-end.") for the
 * origin of the "resources" json property. There is no Ambari domain
 * class we can reuse.
 */
public class ResourceWrappedResponse<T> {

    @JsonProperty("resources")
    private List<T> resources;

    public List<T> getResources() {
        return resources != null ? ImmutableList.copyOf(resources) : Collections.<T>emptyList();
    }

}
