package com.newrelic.agent.attributes;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.newrelic.deps.com.google.common.collect.Maps;

public class AttributesUtils {
    public static Map<String, String> appendAttributePrefixes(Map<String, Map<String, String>> input) {
        if ((input == null) || (input.isEmpty())) {
            return Collections.emptyMap();
        }

        Map<String, String> toReturn = Maps.newHashMap();

        String prefix;
        for (Entry<String, Map<String, String>> current : input.entrySet()) {
            prefix = current.getKey();
            Map<String, String> attributes = current.getValue();
            if (attributes != null) {
                for (Entry<String, String> att : attributes.entrySet()) {
                    toReturn.put(prefix + att.getKey(), att.getValue());
                }
            }
        }

        return toReturn;
    }
}