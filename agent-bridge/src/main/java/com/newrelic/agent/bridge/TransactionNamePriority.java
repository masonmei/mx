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
        switch (priority.ordinal()) {
            case 1:
                return CUSTOM_HIGH;
            case 2:
                return CUSTOM_LOW;
            case 3:
                return FRAMEWORK_HIGH;
            case 4:
                return FRAMEWORK_LOW;
            case 5:
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
