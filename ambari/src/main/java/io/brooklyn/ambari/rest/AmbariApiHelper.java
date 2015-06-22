package io.brooklyn.ambari.rest;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ImmutableList;
import com.squareup.okhttp.OkHttpClient;

import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.client.OkClient;
import retrofit.converter.JacksonConverter;

// TODO Name better.
public class AmbariApiHelper {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private AmbariApiHelper() {}

    public static AmbariApi newApi(String uri, String username, String password) {
        RequestInterceptor interceptor = Interceptors.newAggregatingInterceptor(ImmutableList.of(
                Interceptors.newBasicAuthenticationInterceptor(username, password),
                Interceptors.newRequestedByInterceptor()));

        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(uri)
                .setClient(new OkClient(new OkHttpClient()))
                .setConverter(new JacksonConverter(MAPPER))
                .setRequestInterceptor(interceptor)
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        return restAdapter.create(AmbariApi.class);
    }

}
