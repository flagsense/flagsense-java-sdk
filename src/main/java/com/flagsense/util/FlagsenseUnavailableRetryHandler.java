package com.flagsense.util;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.protocol.HttpContext;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.apache.http.HttpStatus.*;
import static org.apache.http.HttpStatus.SC_REQUEST_TIMEOUT;
import static org.apache.http.HttpStatus.SC_RESET_CONTENT;
import static org.apache.http.HttpStatus.SC_UNPROCESSABLE_ENTITY;

public class FlagsenseUnavailableRetryHandler implements ServiceUnavailableRetryStrategy {

    private static final int MAX_RETRY = 5;
    private static final long RETRY_INTERVAL = 5000;
    private static final Set<Integer> RETRYABLE_ERROR_CODES = new HashSet<>(Arrays.asList(SC_RESET_CONTENT, SC_REQUEST_TIMEOUT, SC_UNPROCESSABLE_ENTITY));

    @Override
    public boolean retryRequest(HttpResponse httpResponse, int executionCount, HttpContext httpContext) {
        if (executionCount > MAX_RETRY)
            return false;
        return this.isResponseRetryable(httpResponse);
    }

    @Override
    public long getRetryInterval() {
        return RETRY_INTERVAL;
    }

    private boolean isResponseRetryable(HttpResponse httpResponse) {
        StatusLine statusLine = httpResponse.getStatusLine();
        if (statusLine == null)
            return true;

        int statusCode = statusLine.getStatusCode();
        if (statusCode == 429 || (500 <= statusCode && statusCode < 600) || RETRYABLE_ERROR_CODES.contains(statusCode))
            return true;

        return false;
    }
}
