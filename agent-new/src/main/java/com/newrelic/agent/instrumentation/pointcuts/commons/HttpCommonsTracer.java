package com.newrelic.agent.instrumentation.pointcuts.commons;

import java.lang.reflect.Method;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.Transaction;
import com.newrelic.agent.tracers.AbstractCrossProcessTracer;
import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.tracers.IOTracer;

public class HttpCommonsTracer extends AbstractCrossProcessTracer implements IOTracer {
    public HttpCommonsTracer(Transaction transaction, ClassMethodSignature sig, Object object, String host,
                             String library, String uri, String methodName) {
        super(transaction, sig, object, host, library, uri, methodName);
    }

    protected String getHeaderValue(Object returnValue, String name) {
        Object invocationTarget = getInvocationTarget();
        if ((invocationTarget instanceof HttpMethodExtension)) {
            HttpMethodExtension httpMethod = (HttpMethodExtension) invocationTarget;
            Object header = httpMethod._nr_getResponseHeader(name);
            return header == null ? null : getHeaderValue(header);
        }
        try {
            Method method = invocationTarget.getClass().getMethod("getResponseHeader", new Class[] {String.class});
            Object header = method.invoke(invocationTarget, new Object[] {name});
            if (header != null) {
                return (header instanceof NameValuePair) ? ((NameValuePair) header).getValue() : header.toString();
            }
        } catch (Throwable t) {
            Agent.LOG.log(Level.FINEST, "Error getting response header", t);
        }
        return null;
    }

    private String getHeaderValue(Object header) {
        if (header == null) {
            return null;
        }
        if ((header instanceof Header)) {
            return ((Header) header).getValue();
        }
        return header.toString();
    }
}