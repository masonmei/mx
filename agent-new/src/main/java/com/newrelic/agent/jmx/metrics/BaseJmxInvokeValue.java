package com.newrelic.agent.jmx.metrics;

public class BaseJmxInvokeValue {
    private final String objectNameString;
    private final String operationName;
    private final Object[] params;
    private final String[] signature;

    public BaseJmxInvokeValue(String pObjectName, String pOperationName, Object[] pParams, String[] pSignature) {
        this.objectNameString = pObjectName;
        this.operationName = pOperationName;
        this.params = pParams;
        this.signature = pSignature;
    }

    public String getObjectNameString() {
        return this.objectNameString;
    }

    public String getOperationName() {
        return this.operationName;
    }

    public Object[] getParams() {
        return this.params;
    }

    public String[] getSignature() {
        return this.signature;
    }
}