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

import com.google.common.base.Function;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.mockito.Mockito.verify;

public class FunctionRunningCallableTest {

    @Mock
    Function<String, ?> function;

    String listener = "I'm a listener";

    @BeforeClass
    public void before() {
        MockitoAnnotations.initMocks(this);
    }

    @Test(
            expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "Parameter listener must not be null"
    )
    public void testConstructorThrowExceptionIfListenerIsNull() {
        new FunctionRunningCallable<String>(null, null);
    }

    @Test(
            expectedExceptions = NullPointerException.class,
            expectedExceptionsMessageRegExp = "Parameter function must not be null"
    )
    public void testConstructorThrowExceptionIfFunctionIsNull() {
        new FunctionRunningCallable<String>(listener, null);
    }

    @Test
    public void testRunCorrectly() {
        FunctionRunningCallable<String> functionRunningCallable = new FunctionRunningCallable<String>(listener, function);
        functionRunningCallable.run();

        verify(function).apply(listener);
    }
}
