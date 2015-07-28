//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.bridge;

public enum TransactionNamePriority {
    NONE,
    REQUEST_URI,
    STATUS_CODE,
    FILTER_NAME,
    FILTER_INIT_PARAM,
    SERVLET_NAME,
    SERVLET_INIT_PARAM,
    JSP,
    FRAMEWORK_LOW,
    FRAMEWORK,
    FRAMEWORK_HIGH,
    CUSTOM_LOW,
    CUSTOM_HIGH,
    FROZEN;

    private TransactionNamePriority() {
    }

    public static TransactionNamePriority convert(com.newrelic.api.agent.TransactionNamePriority priority) {
        switch (priority) {
            case CUSTOM_HIGH:
                return CUSTOM_HIGH;
            case CUSTOM_LOW:
                return CUSTOM_LOW;
            case FRAMEWORK_HIGH:
                return FRAMEWORK_HIGH;
            case FRAMEWORK_LOW:
                return FRAMEWORK_LOW;
            case REQUEST_URI:
                return REQUEST_URI;
            default:
                throw new IllegalArgumentException("Unmapped TransactionNamePriority " + priority);
        }
    }

    public boolean isGreaterThan(TransactionNamePriority other) {
        return this.compareTo(other) > 0;
    }

    public boolean isLessThan(TransactionNamePriority other) {
        return this.compareTo(other) < 0;
    }
}
