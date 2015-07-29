package com.newrelic.agent;

import java.util.Set;

import com.newrelic.api.agent.HeaderType;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.OutboundHeaders;
import com.newrelic.deps.com.google.common.collect.ImmutableSet;

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
    public static final Set<String> NEWRELIC_HEADERS = ImmutableSet.of(NEWRELIC_ID_HEADER, NEWRELIC_ID_MESSAGE_HEADER,
                                                                              NEWRELIC_TRANSACTION_HEADER,
                                                                              NEWRELIC_TRANSACTION_MESSAGE_HEADER,
                                                                              NEWRELIC_APP_DATA_HEADER,
                                                                              NEWRELIC_APP_DATA_MESSAGE_HEADER,
                                                                              NEWRELIC_SYNTHETICS_HEADER,
                                                                              NEWRELIC_SYNTHETICS_MESSAGE_HEADER);

    private HeadersUtil() {
        throw new UnsupportedOperationException();
    }

    public static String getIdHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_ID_HEADER, NEWRELIC_ID_MESSAGE_HEADER);
        return key == null ? null : headers.getHeader(key);
    }

    public static String getTransactionHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_TRANSACTION_HEADER,
                                              NEWRELIC_TRANSACTION_MESSAGE_HEADER);

        return key == null ? null : headers.getHeader(key);
    }

    public static String getAppDataHeader(InboundHeaders headers) {
        String key =
                getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_APP_DATA_HEADER, NEWRELIC_APP_DATA_MESSAGE_HEADER);

        return key == null ? null : headers.getHeader(key);
    }

    public static String getSyntheticsHeader(InboundHeaders headers) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_SYNTHETICS_HEADER,
                                              NEWRELIC_SYNTHETICS_MESSAGE_HEADER);

        return key == null ? null : headers.getHeader(key);
    }

    public static void setIdHeader(OutboundHeaders headers, String crossProcessId) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_ID_HEADER, NEWRELIC_ID_MESSAGE_HEADER);
        headers.setHeader(key, crossProcessId);
    }

    public static void setTransactionHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_TRANSACTION_HEADER,
                                              NEWRELIC_TRANSACTION_MESSAGE_HEADER);

        headers.setHeader(key, value);
    }

    public static void setAppDataHeader(OutboundHeaders headers, String value) {
        String key =
                getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_APP_DATA_HEADER, NEWRELIC_APP_DATA_MESSAGE_HEADER);

        headers.setHeader(key, value);
    }

    public static void setSyntheticsHeader(OutboundHeaders headers, String value) {
        String key = getTypedHeaderKey(headers.getHeaderType(), NEWRELIC_SYNTHETICS_HEADER,
                                              NEWRELIC_SYNTHETICS_MESSAGE_HEADER);

        headers.setHeader(key, value);
    }

    private static String getTypedHeaderKey(HeaderType type, String httpHeader, String messageHeader) {
        if (type == null) {
            return null;
        }

        switch (type) {
            case MESSAGE:
                return messageHeader;
            case HTTP:
            default:
                return httpHeader;
        }

    }
}