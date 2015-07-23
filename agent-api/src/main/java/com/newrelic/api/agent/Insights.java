package com.newrelic.api.agent;

import java.util.Map;

/**
 * Used to send custom events to Insights.
 *
 */
public interface Insights {

    /**
     * Sends an Insights event for the current application.
     * @param eventType Must match /^[a-zA-Z0-9:_ ]+$/ and be less than 256 chars.
     * @param attributes A map of event data. The value should be a String, Number, or Boolean.
     * @since 3.13.0 
     */
    void recordCustomEvent(String eventType, Map<String, Object> attributes);
}
