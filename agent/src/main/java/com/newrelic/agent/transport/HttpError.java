package com.newrelic.agent.transport;

import java.text.MessageFormat;
import java.util.Map;

import com.newrelic.deps.com.google.common.collect.ImmutableMap;

public class HttpError extends Exception {
    private static final long serialVersionUID = 1L;
    private static final Map<Integer, String> RESPONSE_MESSAGES = ImmutableMap.of(Integer.valueOf(413),
                                                                                         "The data post was too large"
                                                                                                 + " ({1})",
                                                                                         Integer.valueOf(415),
                                                                                         "An error occurred "
                                                                                                 + "serializing data "
                                                                                                 + "({1})",
                                                                                         Integer.valueOf(500),
                                                                                         "{0} encountered an internal"
                                                                                                 + " error ({1})",
                                                                                         Integer.valueOf(503),
                                                                                         "{0} is temporarily "
                                                                                                 + "unavailable ({1})");
    private final int statusCode;

    public HttpError(String message, int statusCode) {
        super(message == null ? Integer.toString(statusCode) : message);
        this.statusCode = statusCode;
    }

    public static HttpError create(int statusCode, String host) {
        String messageFormat = (String) RESPONSE_MESSAGES.get(Integer.valueOf(statusCode));
        if (messageFormat == null) {
            messageFormat = "Received a {1} response from {0}";
        }

        String message = MessageFormat.format(messageFormat, new Object[] {host, Integer.valueOf(statusCode)});

        return new HttpError(message, statusCode);
    }

    public int getStatusCode() {
        return statusCode;
    }

    public boolean isRetryableError() {
        return (statusCode != 413) && (statusCode != 415);
    }
}