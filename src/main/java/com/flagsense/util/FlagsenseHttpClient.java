package com.flagsense.util;

import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class FlagsenseHttpClient implements Closeable {

    private static final int CONNECTION_TIMEOUT_MS = 10000;
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = 5000;
    private static final int SOCKET_TIMEOUT_MS = 10000;
    private static final int MAX_TOTAL_CONNECTIONS = 10;
    private static final int MAX_PER_ROUTE = 10;
    private static final int VALIDATE_AFTER_INACTIVITY = 5000;
    private static final long EVICT_CONNECTION_IDLE_TIMEOUT = 60;

    private final CloseableHttpClient httpClient;

    private FlagsenseHttpClient(CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public static Builder builder() {
        return new Builder();
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    @Override
    public void close() throws IOException {
        this.httpClient.close();
    }

    public CloseableHttpResponse execute(final HttpUriRequest request) throws IOException {
        return httpClient.execute(request);
    }

    public static class Builder {
        public FlagsenseHttpClient build() {
            PoolingHttpClientConnectionManager poolingHttpClientConnectionManager = new PoolingHttpClientConnectionManager();
            poolingHttpClientConnectionManager.setMaxTotal(MAX_TOTAL_CONNECTIONS);
            poolingHttpClientConnectionManager.setDefaultMaxPerRoute(MAX_PER_ROUTE);
            poolingHttpClientConnectionManager.setValidateAfterInactivity(VALIDATE_AFTER_INACTIVITY);

            HttpClientBuilder builder = HttpClients.custom()
                    .setDefaultRequestConfig(RequestConfig.custom()
                            .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                            .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
                            .setSocketTimeout(SOCKET_TIMEOUT_MS)
                            .build())
                    .setConnectionManager(poolingHttpClientConnectionManager)
                    .setRetryHandler(new FlagsenseRetryHandler())
                    .setServiceUnavailableRetryStrategy(new FlagsenseUnavailableRetryHandler())
                    .disableCookieManagement()
                    .useSystemProperties();

            if (EVICT_CONNECTION_IDLE_TIMEOUT > 0)
                builder.evictIdleConnections(EVICT_CONNECTION_IDLE_TIMEOUT, TimeUnit.SECONDS);

            CloseableHttpClient closableHttpClient = builder.build();
            return new FlagsenseHttpClient(closableHttpClient);
        }
    }
}
