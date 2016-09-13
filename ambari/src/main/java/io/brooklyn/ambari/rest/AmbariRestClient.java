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
package io.brooklyn.ambari.rest;

import static com.google.api.client.repackaged.com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import java.util.Map;

import org.apache.brooklyn.util.collections.MutableMap;
import org.apache.brooklyn.util.executor.HttpExecutorFactory;
import org.apache.brooklyn.util.http.executor.HttpExecutor;
import org.apache.brooklyn.util.http.executor.HttpRequest;
import org.apache.brooklyn.util.http.executor.HttpResponse;
import org.apache.brooklyn.util.stream.Streams;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;

import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.client.UrlConnectionClient;
import retrofit.mime.TypedByteArray;
import retrofit.mime.TypedInput;
import retrofit.mime.TypedOutput;

/**
 * Represent a REST client which provides the ability to use different HttpExecetor implementations as defined by
 * {@link org.apache.brooklyn.util.http.executor.HttpExecutor} or produced by {@link org.apache.brooklyn.util.executor.HttpExecutorFactory}.
 *
 * It uses the Retrofit library default client, if factory is not set.
 */
public class AmbariRestClient implements Client {

    private static Client client;

    public AmbariRestClient() {
        client = new UrlConnectionClient(); //Default Retrofit client
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private HttpExecutorFactory factory;
        private Map<?, ?> props;

        public Builder httpExecutorFactory(HttpExecutorFactory val) {
            checkNotNull(val, "AmbariRestClient factory");
            factory = val;
            return this;
        }

        public Builder httpExecutorProps(Map<?, ?> val) {
            checkNotNull(val, "AmbariRestClient props");
            props = val;
            return this;
        }

        public AmbariRestClientImpl build() {
            checkNotNull(factory, "AmbariRestClient factory");
            checkNotNull(props, "AmbariRestClient props");

            return new AmbariRestClientImpl();
        }

        private class AmbariRestClientImpl implements Client {
            HttpExecutor httpExecutor = null;
            AmbariRestClientImpl() {
                this.httpExecutor = factory.getHttpExecutor(props);
            }

            @Override
            public Response execute(Request request) throws IOException {
                Map<String, String> headersMap = MutableMap.of();
                for (Header header: request.getHeaders()) {
                    headersMap.put(header.getName(), header.getValue());
                }

                String mimeType = "text/plain"; // Default Content-Type used in Brooklyn Ambari
                byte[] content = null;
                TypedOutput body = request.getBody();
                if (body != null) {
                    mimeType = request.getBody().mimeType();
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try {
                        body.writeTo(baos);
                        content = baos.toByteArray();
                    } catch (IOException e) {
                        throw e;
                    } finally {
                        Streams.closeQuietly(baos);
                    }
                }

                final HttpResponse response = httpExecutor.execute(new HttpRequest.Builder()
                        .headers(headersMap)
                        .uri(URI.create(request.getUrl()))
                        .method(request.getMethod())
                        .body(content)
                        .build());

                List<Header> responseHeaders = Lists.newArrayList();
                for (Map.Entry<String, String> entry: response.headers().entries()) {
                    responseHeaders.add(new Header(entry.getKey(), entry.getValue()));
                }
                TypedInput responseBody = new TypedByteArray(mimeType, ByteStreams.toByteArray(response.getContent()));
                return new Response(request.getUrl(), 
                        response.code(), 
                        (response.reasonPhrase() != null) ? response.reasonPhrase() : "", 
                        responseHeaders, 
                        responseBody);
            }
        }
    }

    @Override
    public Response execute(Request request) throws IOException {
        return client.execute(request);
    }
}

