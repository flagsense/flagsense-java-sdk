package com.flagsense.util;

public class Constants {
    public static final String HEADER_AUTH_TYPE = "authType";
    public static final String HEADER_SDK_ID = "sdkId";
    public static final String HEADER_SDK_SECRET = "sdkSecret";
    public static final String BASE_URL = "https://app-apis.flagsense.com/v1/sdk-service/";
    public static final String EVENTS_BASE_URL = "https://app-events.flagsense.com/v1/events-service/";

    public static final double MAX_HASH_VALUE = Math.pow(2, 32);
    public static final int TOTAL_THREE_DECIMAL_TRAFFIC = 100000;
    public static final long DATA_REFRESH_INTERVAL = 1L;
    public static final boolean CAPTURE_EVENTS_FLAG = true;
    public static final long EVENT_FLUSH_INTITAL_DELAY = 2L;
    public static final long EVENT_FLUSH_INTERVAL = 5L;
    public static final long MAX_INITIALIZATION_WAIT_TIME = 60 * 1000L;
}
