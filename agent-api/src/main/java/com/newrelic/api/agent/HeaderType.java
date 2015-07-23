package com.newrelic.api.agent;

/**
 * Used for specifying header key syntax for {@link InboundHeaders} and {@link OutboundHeaders}.
 */
public enum HeaderType {
    /**
     * To be used with HTTP and HTTPS requests and responses.
     */
    HTTP,
    /**
     * To be used with message queue systems.
     */
    MESSAGE
}
