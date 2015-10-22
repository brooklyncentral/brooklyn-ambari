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

import static org.testng.Assert.assertEquals;

import org.testng.annotations.Test;

public class ComponentMappingTest {

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Mapping is required")
    public void constructorThrowsExIfMappingIsNull() {
        new ExtraService.ComponentMapping(null, "");
    }

    @Test(expectedExceptions = NullPointerException.class, expectedExceptionsMessageRegExp = "Default host is required")
    public void constructorThrowsExIfDefaultHostIsNull() {
        new ExtraService.ComponentMapping("", null);
    }

    @Test
    public void defaultHostIsUsedIfComponentIsNotAMapping() {
        final String component = "my-component";
        final String defaultHost = "my-default-host";
        final ExtraService.ComponentMapping componentMapping = new ExtraService.ComponentMapping(component, defaultHost);

        assertEquals(defaultHost, componentMapping.getHost());
    }

    @Test
    public void defaultHostIsUsedIfComponentIsNotAMappingWithSpaces() {
        final String component = "   my-component";
        final String defaultHost = "my-default-host";
        final ExtraService.ComponentMapping componentMapping = new ExtraService.ComponentMapping(component, defaultHost);

        assertEquals(defaultHost, componentMapping.getHost());
    }

    @Test
    public void defaultHostIsIgnoredIfComponentIsMapping() {
        final String component = "my-component";
        final String host = "my-host";
        final String defaultHost = "my-default-host";
        final ExtraService.ComponentMapping componentMapping = new ExtraService.ComponentMapping(String.format("%s|%s", component, host), defaultHost);

        assertEquals(host, componentMapping.getHost());
    }

    @Test
    public void defaultHostIsIgnoredIfComponentIsMappingWithSpaces() {
        final String component = "my-component";
        final String host = "my-host";
        final String defaultHost = "my-default-host";
        final ExtraService.ComponentMapping componentMapping = new ExtraService.ComponentMapping(String.format(" %s  | %s   ", component, host), defaultHost);

        assertEquals(host, componentMapping.getHost());
    }
}
