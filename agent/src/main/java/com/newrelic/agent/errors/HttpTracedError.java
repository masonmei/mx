package com.newrelic.agent.errors;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;

public class HttpTracedError extends TracedError {
    private final int responseStatus;
    private final String errorMessage;

    public HttpTracedError(String appName, String frontendMetricName, int responseStatus, String errorMessage,
                           String requestPath, long timestamp, Map<String, Map<String, String>> prefixedParams,
                           Map<String, Object> userParams, Map<String, Object> agentParams,
                           Map<String, String> errorParams, Map<String, Object> intrinsics) {
        super(appName, frontendMetricName, requestPath, timestamp, prefixedParams, userParams, agentParams, errorParams,
                     intrinsics);

        this.responseStatus = responseStatus;
        if (errorMessage == null) {
            if ((responseStatus >= 400) && (responseStatus < 500)) {
                this.errorMessage = ("HttpClientError " + responseStatus);
            } else {
                this.errorMessage = ("HttpServerError " + responseStatus);
            }
        } else {
            this.errorMessage = errorMessage;
        }
    }

    public Collection<String> stackTrace() {
        return null;
    }

    public String getExceptionClass() {
        return getMessage();
    }

    public String getMessage() {
        return errorMessage;
    }

    public String toString() {
        return MessageFormat.format("{0} ({1})", new Object[] {getMessage(), Integer.valueOf(responseStatus)});
    }
}