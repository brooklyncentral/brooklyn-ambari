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
package io.brooklyn.ambari.service;

import javax.annotation.Nullable;

import com.google.common.base.Function;

import brooklyn.entity.Entity;
import brooklyn.entity.basic.BrooklynTaskTags;
import brooklyn.entity.basic.Entities;
import brooklyn.entity.basic.EntityLocal;
import brooklyn.entity.basic.ServiceStateLogic;
import brooklyn.management.Task;

public abstract class AbstractExtraServicesTask<T extends Entity> implements Function<T, Void> {

    protected String errorKey = "ranger.mysql";
    protected String errorDescription = "Error initialising Ranger requirements";

    public abstract Task<Integer> sshTaskApply(T node);

    @Nullable
    @Override
    public Void apply(T node) {
        Task<Integer> task = sshTaskApply(node);
        Entities.submit(node, task);
        task.blockUntilEnded();

        Integer result = task.getUnchecked();
        if (result != 0) {

            BrooklynTaskTags.WrappedStream stream = BrooklynTaskTags.stream(task, "stderr");
            final String errorMessage = String.format("%s: %s", errorDescription, stream != null ? stream.streamContents.get() : "Unexpected error");

            ServiceStateLogic.ServiceNotUpLogic.updateNotUpIndicator((EntityLocal) node, errorKey, errorMessage);
            throw new RuntimeException(String.format("[Node %s] %s", node.getDisplayName(), errorMessage));
        }

        return null;
    }


}
