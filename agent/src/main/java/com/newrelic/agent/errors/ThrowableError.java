package com.newrelic.agent.errors;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.newrelic.agent.instrumentation.pointcuts.container.jetty.MultiException;
import com.newrelic.agent.util.StackTraces;

public class ThrowableError extends TracedError {
    private final Throwable throwable;

    public ThrowableError(String appName, String frontendMetricName, Throwable error, String requestPath,
                          long timestamp, Map<String, Map<String, String>> prefixedParams,
                          Map<String, Object> userParams, Map<String, Object> agentParams,
                          Map<String, String> errorParams, Map<String, Object> intrinsics) {
        super(appName, frontendMetricName, requestPath, timestamp, prefixedParams, userParams, agentParams, errorParams,
                     intrinsics);

        throwable = error;
    }

    public Throwable getThrowable() {
        return throwable;
    }

    public String getMessage() {
        String message = throwable == null ? null : ErrorService.getStrippedExceptionMessage(throwable);
        if (message == null) {
            return "";
        }
        return message;
    }

    public String getExceptionClass() {
        return throwable.getClass().getName();
    }

    public Collection<String> stackTrace() {
        Collection stackTrace = new ArrayList();

        if ((throwable instanceof MultiException)) {
            List throwables = ((MultiException) throwable).getThrowables();
            for (int i = 0; i < throwables.size(); i++) {
                if (i > 0) {
                    stackTrace.add(" ");
                }
                stackTrace.addAll(StackTraces.stackTracesToStrings(((Throwable) throwables.get(i)).getStackTrace()));
            }
        } else {
            Throwable t = throwable;
            boolean inner = false;
            while (t != null) {
                if (inner) {
                    stackTrace.add(" ");
                    stackTrace.add(" caused by " + t.toString());
                }
                stackTrace.addAll(StackTraces.stackTracesToStrings(t.getStackTrace()));
                t = t.equals(t.getCause()) ? null : t.getCause();
                inner = true;
            }
        }

        return stackTrace;
    }

    public String toString() {
        return getMessage();
    }

    public int hashCode() {
        return throwable.hashCode();
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
        ThrowableError other = (ThrowableError) obj;
        return throwable.equals(other.throwable);
    }
}