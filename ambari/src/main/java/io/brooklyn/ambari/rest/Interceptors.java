package io.brooklyn.ambari.rest;

import static com.google.common.base.Preconditions.checkNotNull;

import org.apache.commons.codec.binary.Base64;

import com.google.common.collect.ImmutableList;

import retrofit.RequestInterceptor;

public class Interceptors {

    private Interceptors() {}

    /**
     * @return An interceptor that passes requests to all of the given interceptors.
     */
    public static RequestInterceptor newAggregatingInterceptor(Iterable<RequestInterceptor> interceptors) {
        return new AggregatingRequestInterceptor(checkNotNull(interceptors, "interceptors"));
    }

    /**
     * @return An interceptor that adds an X-Requested-By header to requests.
     */
    public static RequestInterceptor newRequestedByInterceptor() {
        return new AmbariRequestedByInterceptor();
    }

    /**
     * @return An interceptor that adds an "Authorization: ..." header to requests with
     * basic HTTP authentication.
     */
    public static RequestInterceptor newBasicAuthenticationInterceptor(String username, String password) {
        return new BasicAuthenticationRequestInterceptor(checkNotNull(username, "username"), checkNotNull(password, "password"));
    }

    private static class AggregatingRequestInterceptor implements RequestInterceptor {
        final Iterable<RequestInterceptor> interceptors;

        public AggregatingRequestInterceptor(Iterable<RequestInterceptor> interceptors) {
            this.interceptors = ImmutableList.copyOf(interceptors);
        }

        @Override
        public void intercept(RequestFacade request) {
            for (RequestInterceptor interceptor : interceptors) {
                interceptor.intercept(request);
            }
        }
    }

    private static class AmbariRequestedByInterceptor implements RequestInterceptor {
        @Override
        public void intercept(RequestFacade request) {
            request.addHeader("X-Requested-By", "brooklyn");
        }
    }

    private static class BasicAuthenticationRequestInterceptor implements RequestInterceptor {
        final String username;
        final String password;

        private BasicAuthenticationRequestInterceptor(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void intercept(RequestFacade request) {
            String credentials = username + ":" + password;
            String string = "Basic " + Base64.encodeBase64String(credentials.getBytes());
            request.addHeader("Authorization", string);
        }
    }

}
