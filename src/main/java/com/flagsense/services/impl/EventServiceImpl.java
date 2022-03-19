package com.flagsense.services.impl;

import com.flagsense.model.Metrics;
import com.flagsense.model.SdkConfig;
import com.flagsense.request.ExperimentEventsRequest;
import com.flagsense.request.VariantsRequest;
import com.flagsense.services.EventService;
import com.flagsense.util.FlagsenseHttpClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.*;

import static com.flagsense.util.Constants.*;

public class EventServiceImpl implements EventService, AutoCloseable {

    private final ObjectMapper objectMapper;

    private final SdkConfig sdkConfig;
    private final VariantsRequest request;
    private final ExperimentEventsRequest experimentEventsRequest;
    private final ConcurrentMap<Long, String> requests;
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> data;
    private final ConcurrentMap<String, ConcurrentMap<String, Long>> codeBugs;
    private final ConcurrentMap<String, Long> errors;
    private final ConcurrentMap<Long, String> experimentEventsRequests;
    private final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, Metrics>>> experimentEvents;
    private final long MILLIS_IN_EVENT_FLUSH_INTERVAL;
    private long timeSlot;

    private volatile boolean started;
    private final Runnable eventSender;
    private ScheduledFuture<?> scheduledFuture;
    private final ScheduledExecutorService scheduledExecutorService;

    public EventServiceImpl(SdkConfig sdkConfig) {
        String machineId = UUID.randomUUID().toString();
        this.objectMapper = new ObjectMapper();

        this.sdkConfig = sdkConfig;
        this.data = new ConcurrentHashMap<>();
        this.codeBugs = new ConcurrentHashMap<>();
        this.errors = new ConcurrentHashMap<>();
        this.requests = new ConcurrentHashMap<>();
        this.experimentEvents = new ConcurrentHashMap<>();
        this.experimentEventsRequests = new ConcurrentHashMap<>();
        this.MILLIS_IN_EVENT_FLUSH_INTERVAL = EVENT_FLUSH_INTERVAL * 60 * 1000;
        this.timeSlot = getTimeSlot(System.currentTimeMillis());
        this.request = new VariantsRequest(machineId, sdkConfig.getEnvironment(), this.data, this.codeBugs, this.errors, this.timeSlot);
        this.experimentEventsRequest = new ExperimentEventsRequest(machineId, sdkConfig.getEnvironment(), this.experimentEvents, this.timeSlot);

        this.eventSender = new EventSender(this.sdkConfig, this.requests, this.experimentEventsRequests);
        final ThreadFactory threadFactory = Executors.defaultThreadFactory();
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = threadFactory.newThread(runnable);
            thread.setDaemon(true);
            return thread;
        });

        registerShutdownHook();
    }

    @Override
    public synchronized void start() {
        if (!CAPTURE_EVENTS_FLAG || started || scheduledExecutorService.isShutdown())
            return;

        scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(this.eventSender, EVENT_FLUSH_INTITAL_DELAY, EVENT_FLUSH_INTERVAL, TimeUnit.MINUTES);
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

    @Override
    public void addEvaluationCount(String flagId, String variantKey) {
        try {
            if (!CAPTURE_EVENTS_FLAG)
                return;

            long currentTimeSlot = getTimeSlot(System.currentTimeMillis());
            if (currentTimeSlot != this.timeSlot)
                checkAndRefreshData(currentTimeSlot);

            ConcurrentMap<String, Long> variantsMap = this.data.get(flagId);
            if (variantsMap == null) {
                variantsMap = this.data.putIfAbsent(flagId, new ConcurrentHashMap<>());
                if (variantsMap == null)
                    variantsMap = this.data.get(flagId);
            }

            variantsMap.merge(variantKey, 1L, Long::sum);
        }
        catch (Exception ignored) {
        }
    }

    @Override
    public void addErrorsCount(String flagId) {
        try {
            if (!CAPTURE_EVENTS_FLAG)
                return;

            long currentTimeSlot = getTimeSlot(System.currentTimeMillis());
            if (currentTimeSlot != this.timeSlot)
                checkAndRefreshData(currentTimeSlot);

            this.errors.merge(flagId, 1L, Long::sum);
        }
        catch (Exception ignored) {
        }
    }

    @Override
    public void addCodeBugsCount(String flagId, String variantKey) {
        try {
            if (!CAPTURE_EVENTS_FLAG)
                return;

            long currentTimeSlot = getTimeSlot(System.currentTimeMillis());
            if (currentTimeSlot != this.timeSlot)
                checkAndRefreshData(currentTimeSlot);

            ConcurrentMap<String, Long> variantsMap = this.codeBugs.get(flagId);
            if (variantsMap == null) {
                variantsMap = this.codeBugs.putIfAbsent(flagId, new ConcurrentHashMap<>());
                if (variantsMap == null)
                    variantsMap = this.codeBugs.get(flagId);
            }

            variantsMap.merge(variantKey, 1L, Long::sum);
        }
        catch (Exception ignored) {
        }
    }

    @Override
    public void recordExperimentEvent(String flagId, String eventName, String variantKey, double value) {
        try {
            if (!CAPTURE_EVENTS_FLAG)
                return;

            long currentTimeSlot = getTimeSlot(System.currentTimeMillis());
            if (currentTimeSlot != this.timeSlot)
                checkAndRefreshData(currentTimeSlot);

            ConcurrentMap<String, ConcurrentMap<String, Metrics>> eventNamesMap = this.experimentEvents.get(flagId);
            if (eventNamesMap == null) {
                eventNamesMap = this.experimentEvents.putIfAbsent(flagId, new ConcurrentHashMap<>());
                if (eventNamesMap == null)
                    eventNamesMap = this.experimentEvents.get(flagId);
            }

            ConcurrentMap<String, Metrics> variantsMap = eventNamesMap.get(eventName);
            if (variantsMap == null) {
                variantsMap = eventNamesMap.putIfAbsent(eventName, new ConcurrentHashMap<>());
                if (variantsMap == null)
                    variantsMap = eventNamesMap.get(eventName);
            }

            variantsMap.merge(variantKey, Metrics.of(value), (m1, m2) -> {
                Metrics metrics = Metrics.EMPTY();
                metrics.setCount(m1.getCount() + m2.getCount());
                metrics.setTotal(m1.getTotal().add(m2.getTotal()));
                metrics.setMinimum(m1.getMinimum().min(m2.getMinimum()));
                metrics.setMaximum(m1.getMaximum().max(m2.getMaximum()));
                return metrics;
            });
        }
        catch (Exception ignored) {
        }
    }

    private synchronized void checkAndRefreshData(long currentTimeSlot) {
        if (currentTimeSlot == this.timeSlot)
            return;
        refreshData(currentTimeSlot);
    }

    private synchronized void refreshData(long currentTimeSlot) {
        this.request.setTime(this.timeSlot);
        this.request.setData(this.data);
        this.request.setCodeBugs(this.codeBugs);
        this.request.setErrors(this.errors);

        try {
            if (!this.request.getData().isEmpty() || !this.request.getCodeBugs().isEmpty() || !this.request.getErrors().isEmpty())
                this.requests.put(this.timeSlot, objectMapper.writeValueAsString(this.request));
        }
        catch(JsonProcessingException ignored) {
        }

        this.experimentEventsRequest.setTime(this.timeSlot);
        this.experimentEventsRequest.setExperimentEvents(this.experimentEvents);

        try {
            if (!this.experimentEventsRequest.getExperimentEvents().isEmpty())
                this.experimentEventsRequests.put(this.timeSlot, objectMapper.writeValueAsString(this.experimentEventsRequest));
        }
        catch(JsonProcessingException ignored) {
        }

        this.data.clear();
        this.codeBugs.clear();
        this.errors.clear();
        this.experimentEvents.clear();
        this.timeSlot = currentTimeSlot;
    }

    // This method has been optimized for EVENT_FLUSH_INTERVAL = 5L
    private long initialDelay() {
        if (EVENT_FLUSH_INTERVAL == 1L)
            return EVENT_FLUSH_INTERVAL;

        long currentTime = System.currentTimeMillis();
        long currentTimeSlot = getTimeSlot(currentTime);
        long expectedStartTime = currentTimeSlot + MILLIS_IN_EVENT_FLUSH_INTERVAL / 2;
        long diffInSeconds = (expectedStartTime - currentTime) / 1000;

        if (Math.abs(diffInSeconds) <= 30)
            return EVENT_FLUSH_INTERVAL - 1;

        if (diffInSeconds > 0)
            return EVENT_FLUSH_INTERVAL + 1;

        if (EVENT_FLUSH_INTERVAL > 2L)
            return EVENT_FLUSH_INTERVAL - 2;

        return 1L;
    }

    private long getTimeSlot(long time) {
        return ((time + MILLIS_IN_EVENT_FLUSH_INTERVAL - 1) / MILLIS_IN_EVENT_FLUSH_INTERVAL) * MILLIS_IN_EVENT_FLUSH_INTERVAL;
    }

    private void registerShutdownHook() {
        if (!CAPTURE_EVENTS_FLAG)
            return;

        try {
            Thread shutdownHook = new Thread(() -> {
                refreshData(getTimeSlot(System.currentTimeMillis()));
                this.eventSender.run();
            });
            Runtime.getRuntime().addShutdownHook(shutdownHook);
        }
        catch (Exception exception) {
            // System.out.println("flagsense: unable to register shutdown hook");
        }
    }

    private class EventSender implements Runnable {

        private final SdkConfig sdkConfig;
        private final ConcurrentMap<Long, String> requests;
        private final ConcurrentMap<Long, String> experimentEventsRequests;
        private final FlagsenseHttpClient httpClient;

        public EventSender(SdkConfig sdkConfig, ConcurrentMap<Long, String> requests, ConcurrentMap<Long, String> experimentEventsRequests) {
            this.sdkConfig = sdkConfig;
            this.requests = requests;
            this.experimentEventsRequests = experimentEventsRequests;
            this.httpClient = FlagsenseHttpClient.builder().build();
        }

        @Override
        public void run() {
            Set<Long> timeKeys = this.requests.keySet();
            for (Long time : timeKeys) {
                String requestBody = this.requests.getOrDefault(time, null);
                if (requestBody != null)
                    sendEvents("variantsData", requestBody);
                this.requests.remove(time);
            }

            Set<Long> experimentEventsTimeKeys = this.experimentEventsRequests.keySet();
            for (Long time : experimentEventsTimeKeys) {
                String requestBody = this.experimentEventsRequests.getOrDefault(time, null);
                if (requestBody != null)
                    sendEvents("experimentEvents", requestBody);
                this.experimentEventsRequests.remove(time);
            }
        }

        private void sendEvents(String api, String requestBody) {
            CloseableHttpResponse response = null;
            try {
                // System.out.println("sending events at to '" + api + "' at " + LocalDateTime.now() + ": " + requestBody);
                HttpPost httpPost = createRequest(api, requestBody);
                response = httpClient.execute(httpPost);
                // System.out.println("sent");
            }
            catch (Exception exception) {
                // System.out.println(exception.getMessage());
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

        private HttpPost createRequest(String api, String requestBody) throws UnsupportedEncodingException {
            HttpPost httpPost = new HttpPost(EVENTS_BASE_URL + api);
            httpPost.setHeader(HEADER_AUTH_TYPE, "sdk");
            httpPost.setHeader(HEADER_SDK_ID, this.sdkConfig.getSdkId());
            httpPost.setHeader(HEADER_SDK_SECRET, this.sdkConfig.getSdkSecret());
            httpPost.setHeader("Content-Type", "application/json");

            checkAndRefreshData(getTimeSlot(System.currentTimeMillis()));

            httpPost.setEntity(new StringEntity(requestBody));
            return httpPost;
        }
    }
}
