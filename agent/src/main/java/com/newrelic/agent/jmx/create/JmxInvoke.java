package com.newrelic.agent.jmx.create;

import javax.management.MalformedObjectNameException;

public class JmxInvoke extends JmxObject {
    private final String operationName;
    private final Object[] params;
    private final String[] signature;
    private int errorCount = 0;

    public JmxInvoke(String pObjectName, String safeName, String pOperationName, Object[] pParams, String[] pSignature)
            throws MalformedObjectNameException {
        super(pObjectName, safeName);
        operationName = pOperationName;
        params = pParams;
        signature = pSignature;
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

    public int getErrorCount() {
        return errorCount;
    }

    public void incrementErrorCount() {
        errorCount += 1;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("object_name: ").append(getObjectNameString());
        sb.append(" operation_name: ").append(operationName);
        sb.append(" error_count: ").append(errorCount);
        return sb.toString();
    }
}