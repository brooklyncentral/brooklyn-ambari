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

package io.brooklyn.ambari.hostgroup;

import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.SameServerEntity;
import brooklyn.entity.group.DynamicClusterImpl;
import brooklyn.entity.proxying.EntitySpec;
import com.google.common.collect.ImmutableList;

import io.brooklyn.ambari.AmbariCluster;
import io.brooklyn.ambari.agent.AmbariAgent;
import io.brooklyn.ambari.agent.AmbariAgentImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;

public class AmbariHostGroupImpl extends DynamicClusterImpl implements AmbariHostGroup {
    public static final Logger LOG = LoggerFactory.getLogger(AmbariHostGroup.class);

    @Override
    public void init() {
        super.init();
        EntitySpec<?> siblingSpec = getConfig(AmbariHostGroup.SIBLING_SPEC);
        if (siblingSpec != null) {
            config().set(MEMBER_SPEC, agentWithSiblingsSpec(siblingSpec));
        } else {
            config().set(MEMBER_SPEC, ambariAgentSpec());
        }
    }

    @Override
    public List<String> getHostFQDNs() {
        ImmutableList.Builder<String> builder = ImmutableList.<String>builder();
        for (AmbariAgent agent : Entities.descendants(this, AmbariAgent.class)) {
            String fqdn = agent.getFqdn();
            if (fqdn != null) {
                builder.add(fqdn);
            }
        }
        return builder.build();
    }

    @Nullable
    @Override
    public List<String> getComponents() {
        return getConfig(HADOOP_COMPONENTS);
    }

    private EntitySpec<? extends AmbariAgent> ambariAgentSpec() {
        return AmbariAgentImpl.createAgentSpec((AmbariCluster) getParent(), config().getLocalBag());
    }

    private EntitySpec agentWithSiblingsSpec(EntitySpec<?> siblingSpec) {
        return EntitySpec.create(SameServerEntity.class)
                .child(ambariAgentSpec())
                .child(siblingSpec);
    }
}
