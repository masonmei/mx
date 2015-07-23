package com.newrelic.agent;

import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;

public class HeadersUtil {
    public static final String NEWRELIC_ID_HEADER = "X-NewRelic-ID";
    public static final String NEWRELIC_ID_MESSAGE_HEADER = "NewRelicID";
    public static final String NEWRELIC_TRANSACTION_HEADER = "X-NewRelic-Transaction";
    public static final String NEWRELIC_TRANSACTION_MESSAGE_HEADER = "NewRelicTransaction";
    public static final String NEWRELIC_APP_DATA_HEADER = "X-NewRelic-App-Data";
    public static final String NEWRELIC_APP_DATA_MESSAGE_HEADER = "NewRelicAppData";
    public static final String NEWRELIC_SYNTHETICS_HEADER = "X-NewRelic-Synthetics";
    public static final String NEWRELIC_SYNTHETICS_MESSAGE_HEADER = "NewRelicSynthetics";
    public static final int SYNTHETICS_MIN_VERSION = 1;
    public static final int SYNTHETICS_MAX_VERSION = 1;
    public static final int SYNTHETICS_VERSION_NONE = -1;
    public static final Set<String> NEWRELIC_HEADERS = ImmutableSet.of("X-NewRelic-ID", "NewRelicID",
                                                                              "X-NewRelic-Transaction",
                                                                              "NewRelicTransaction",
                                                                              "X-NewRelic-App-Data", "NewRelicAppData",
                                                                              new String[] {"X-NewRelic-Synthetics",
                                                                                                   "NewRelicSynthetics"});

    private HeadersUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getIdHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), "X-NewRelic-ID", "NewRelicID");
        return key == null ? null : headers.getHeader(key);
    }

    public static String getTransactionHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), "X-NewRelic-Transaction", "NewRelicTransaction");

        return key == null ? null : headers.getHeader(key);
    }

    public static String getAppDataHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), "X-NewRelic-App-Data", "NewRelicAppData");

        return key == null ? null : headers.getHeader(key);
    }

    public static String getSyntheticsHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), "X-NewRelic-Synthetics", "NewRelicSynthetics");

        return key == null ? null : headers.getHeader(key);
    }

    public static void setIdHeader(OutboundHeaders headers, String crossProcessId) {
        String key = getTypedHeaderKey(headers.getHeaderType(), "X-NewRelic-ID", "NewRelicID");
        headers.setHeader(key, crossProcessId);
    }

    public static void setTransactionHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), "X-NewRelic-Transaction", "NewRelicTransaction");

        headers.setHeader(key, value);
    }

    public static void setAppDataHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), "X-NewRelic-App-Data", "NewRelicAppData");

        headers.setHeader(key, value);
    }

    public static void setSyntheticsHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), "X-NewRelic-Synthetics", "NewRelicSynthetics");

        headers.setHeader(key, value);
    }

    private static String getTypedHeaderKey(HeaderType type, String httpHeader, String messageHeader) {
        if (type == null) {
            return null;
        }

        switch (type.ordinal()) {
            case 1:
                return messageHeader;
            case 2:
        }

        return httpHeader;
    }
}