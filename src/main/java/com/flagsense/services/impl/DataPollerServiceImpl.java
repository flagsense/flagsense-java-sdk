package com.flagsense.services.impl;

import com.flagsense.model.Data;
import com.flagsense.model.SdkConfig;
import com.flagsense.request.FetchLatestRequest;
import com.flagsense.services.DataPollerService;
import com.flagsense.util.FlagsenseHttpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.concurrent.*;

import static com.flagsense.util.Constants.*;

public class DataPollerServiceImpl implements DataPollerService, AutoCloseable {

    private final Data data;
    private final SdkConfig sdkConfig;
    private final ScheduledExecutorService scheduledExecutorService;

    private volatile boolean started;
    private ScheduledFuture<?> scheduledFuture;

    public DataPollerServiceImpl(Data data, SdkConfig sdkConfig) {
        this.data = data;
        this.sdkConfig = sdkConfig;

        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = threadFactory.newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });
    }

    @Override
    public synchronized void start() {
        if (started || scheduledExecutorService.isShutdown())
            return;

        Runnable runnable = new DataPoller(this.data, this.sdkConfig);
        long period = DATA_REFRESH_INTERVAL > 0 ? DATA_REFRESH_INTERVAL : 5L;
        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(runnable, 0, period, TimeUnit.MINUTES);
        started = true;
    }

    @Override
    public synchronized void close() {
        stop();
        scheduledExecutorService.shutdownNow();
        started = false;
    }

    public synchronized void stop() {
        if (!started || scheduledExecutorService.isShutdown())
            return;

        scheduledFuture.cancel(true);
        started = false;
    }

    private class DataPoller implements Runnable {

        private final Data data;
        private final SdkConfig sdkConfig;
        private final FlagsenseHttpClient httpClient;
        private final ObjectMapper objectMapper;

        public DataPoller(Data data, SdkConfig sdkConfig) {
            this.data = data;
            this.sdkConfig = sdkConfig;
            this.httpClient = FlagsenseHttpClient.builder().build();
            this.objectMapper = new ObjectMapper();
        }

        @Override
        public void run() {
            CloseableHttpResponse response = null;
            try {
//                System.out.println("fetching data at: " + LocalDateTime.now());
                HttpPost httpPost = createRequest();
                response = httpClient.execute(httpPost);
                parseResponseAndUpdateData(response);
            }
            catch (Exception exception) {
                System.out.println(exception.getMessage());
            }
            finally {
                if (response != null) {
                    try {
                        response.close();
                    }
                    catch (Exception ignored) {
                    }
                }
            }
        }

        private HttpPost createRequest() throws JsonProcessingException, UnsupportedEncodingException {
            HttpPost httpPost = new HttpPost(BASE_URL + "fetchLatest");
            httpPost.setHeader(HEADER_AUTH_TYPE, "sdk");
            httpPost.setHeader(HEADER_SDK_ID, this.sdkConfig.getSdkId());
            httpPost.setHeader(HEADER_SDK_SECRET, this.sdkConfig.getSdkSecret());
            httpPost.setHeader("Content-Type", "application/json");

            final FetchLatestRequest request = new FetchLatestRequest(this.sdkConfig.getEnvironment(), this.data.getLastUpdatedOn());
            httpPost.setEntity(new StringEntity(objectMapper.writeValueAsString(request)));
            return httpPost;
        }

        private void parseResponseAndUpdateData(CloseableHttpResponse response) throws IOException {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine == null)
                return;

            int status = statusLine.getStatusCode();
            if (status != HttpStatus.SC_OK)
                return;

            HttpEntity responseEntity = response.getEntity();
            if (responseEntity == null)
                return;

            String responseString = EntityUtils.toString(response.getEntity());
            if (StringUtils.isBlank(responseString))
                return;

            Data newData = objectMapper.readValue(responseString, Data.class);
            if (newData != null && newData.getLastUpdatedOn() != null && newData.getSegments() != null && newData.getFlags() != null) {
                if (!newData.getSegments().isEmpty())
                    this.data.setSegments(newData.getSegments());
                if (!newData.getFlags().isEmpty())
                    this.data.setFlags(newData.getFlags());
                this.data.setLastUpdatedOn(newData.getLastUpdatedOn());

                synchronized (this.data) {
                    this.data.notifyAll();
                }
            }
        }
    }
}
