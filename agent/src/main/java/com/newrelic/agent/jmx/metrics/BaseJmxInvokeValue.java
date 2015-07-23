package com.newrelic.agent.jmx.metrics;

public class BaseJmxInvokeValue {
    private final String objectNameString;
    private final String operationName;
    private final Object[] params;
    private final String[] signature;

    public BaseJmxInvokeValue(String pObjectName, String pOperationName, Object[] pParams, String[] pSignature) {
        objectNameString = pObjectName;
        operationName = pOperationName;
        params = pParams;
        signature = pSignature;
    }

    public String getObjectNameString() {
        return objectNameString;
    }

    public String getOperationName() {
        return operationName;
    }

    public Object[] getParams() {
        return params;
    }

    public String[] getSignature() {
        return signature;
    }
}