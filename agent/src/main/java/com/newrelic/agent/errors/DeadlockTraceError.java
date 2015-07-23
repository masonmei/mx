package com.newrelic.agent.errors;

import java.lang.management.ThreadInfo;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.newrelic.agent.util.StackTraces;

public class DeadlockTraceError extends TracedError {
    private final String message;
    private final String exceptionClass;
    private final Map<String, StackTraceElement[]> stackTraces;

    private DeadlockTraceError(String appName, String frontendMetricName, String message, String exceptionClass,
                               Map<String, StackTraceElement[]> stackTraces, String requestPath,
                               Map<String, String> params) {
        super(appName, frontendMetricName, requestPath, System.currentTimeMillis(), null, null, null, params, null);
        this.stackTraces = stackTraces;
        this.message = message;
        this.exceptionClass = exceptionClass;
    }

    public DeadlockTraceError(String appName, ThreadInfo thread, Map<String, StackTraceElement[]> stackTraces,
                              Map<String, String> params) {
        this(appName, "Deadlock", "Deadlocked thread: " + thread.getThreadName(), "Deadlock", stackTraces, "", params);
    }

    public String getMessage() {
        return message;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public Collection<String> stackTrace() {
        return null;
    }

    public boolean incrementsErrorMetric() {
        return false;
    }

    public Map<String, Collection<String>> stackTraces() {
        Map traces = new HashMap();
        for (Entry entry : stackTraces.entrySet()) {
            traces.put(entry.getKey(), StackTraces.stackTracesToStrings((StackTraceElement[]) entry.getValue()));
        }
        return traces;
    }

    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = 31 * result + (stackTraces == null ? 0 : stackTraces.hashCode());
        return result;
    }

    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        DeadlockTraceError other = (DeadlockTraceError) obj;
        if (stackTraces == null) {
            if (other.stackTraces != null) {
                return false;
            }
        } else if (!stackTraces.equals(other.stackTraces)) {
            return false;
        }
        return true;
    }
}