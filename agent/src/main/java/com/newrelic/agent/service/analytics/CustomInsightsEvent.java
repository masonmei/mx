package com.newrelic.agent.service.analytics;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class CustomInsightsEvent extends AnalyticsEvent {
    public CustomInsightsEvent(String type, long timestamp, Map<String, Object> attributes) {
        super(type, timestamp);
        userAttributes = attributes;
    }

    public void writeJSONString(Writer out) throws IOException {
        JSONObject intrinsics = new JSONObject();
        intrinsics.put("type", type);
        intrinsics.put("timestamp", Long.valueOf(timestamp));

        JSONArray.writeJSONString(Arrays.asList(new Map[] {intrinsics, userAttributes}), out);
    }
}