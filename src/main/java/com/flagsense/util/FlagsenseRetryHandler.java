package com.flagsense.util;

import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.protocol.HttpContext;

import java.io.IOException;

public class FlagsenseRetryHandler implements HttpRequestRetryHandler {

    private static final int MAX_HTTP_REQUEST_RETRY = 5;
    private static final int[] RETRY_WAIT_SECONDS = {4, 8, 12, 16, 20};

    @Override
    public boolean retryRequest(IOException e, int executionCount, HttpContext httpContext) {
        if (executionCount > MAX_HTTP_REQUEST_RETRY)
            return false;

        try {
            Thread.sleep(RETRY_WAIT_SECONDS[executionCount - 1] * 1000L);
            return true;
        }
        catch (Exception exception) {
            return true;
        }
    }
}
