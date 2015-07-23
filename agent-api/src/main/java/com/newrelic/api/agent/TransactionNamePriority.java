package com.newrelic.api.agent;

/**
 * The priority to give to a call to
 * {@link Transaction#setTransactionName(TransactionNamePriority, boolean, String, String...)}.
 * 
 * 
 * @author sdaubin
 * 
 */
public enum TransactionNamePriority {

    /**
     * Use the request URI.
     */
    REQUEST_URI,
    /**
     * Use for framework instrumentation.
     */
    FRAMEWORK_LOW,
    /**
     * Use for framework instrumentation.
     */
    FRAMEWORK_HIGH,
    /**
     * Use a custom transaction name (user defined).
     */
    CUSTOM_LOW,
    /**
     * Use a custom transaction name (user defined).
     */
    CUSTOM_HIGH;

}
