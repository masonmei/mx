package com.newrelic.agent.service.analytics;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.simple.JSONStreamAware;

public abstract class AnalyticsEvent implements JSONStreamAware {
    private static final Pattern TYPE_VALID = Pattern.compile("^[a-zA-Z0-9:_ ]{1,255}$");
    final String type;
    final long timestamp;
    Map<String, Object> userAttributes;

    public AnalyticsEvent(String type, long timestamp) {
        this.type = type;
        this.timestamp = timestamp;
    }

    public static boolean isValidType(String type) {
        return TYPE_VALID.matcher(type).matches();
    }

    public abstract void writeJSONString(Writer paramWriter) throws IOException;

    public boolean isValid() {
        return isValidType(type);
    }
}