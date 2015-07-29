package com.newrelic.agent.config;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONObject;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

import com.newrelic.deps.com.google.common.base.CharMatcher;
import com.newrelic.deps.com.google.common.collect.ImmutableMap;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.agent.Agent;

public class LabelsConfigImpl implements LabelsConfig, JSONStreamAware {
    private final Map<String, String> labels = Maps.newHashMap();

    LabelsConfigImpl(Object labelsObj) {
        parseLabels(labelsObj);
    }

    private static String validateLabelPart(String keyOrValue) throws LabelParseException {
        if ((keyOrValue == null) || (keyOrValue.equals(""))) {
            throw new LabelParseException("empty name or value");
        }

        if ((keyOrValue.contains(":")) || (keyOrValue.contains(";"))) {
            throw new LabelParseException("illegal character ':' or ';' in name or value '" + keyOrValue + "'");
        }

        if (keyOrValue.length() > 255) {
            keyOrValue = keyOrValue.substring(0, 255);
            Agent.LOG.log(Level.WARNING, "Label name or value over 255 characters.  Truncated to ''{0}''.",
                                 new Object[] {keyOrValue});
        }

        return keyOrValue.trim();
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    private void parseLabels(Object labelsObj) {
        if (labelsObj == null) {
            return;
        }
        try {
            if ((labelsObj instanceof Map)) {
                parseLabelsMap((Map) labelsObj);
            } else if ((labelsObj instanceof String)) {
                parseLabelsString((String) labelsObj);
            }
        } catch (LabelParseException lpe) {
            Agent.LOG.log(Level.WARNING, "Error parsing labels - {0}", new Object[] {lpe.getMessage()});
            Agent.LOG.log(Level.WARNING, "Labels will not be sent to New Relic");
            labels.clear();
        }
    }

    private void parseLabelsString(String labelsString) throws LabelParseException {
        labelsString = CharMatcher.is(';').trimFrom(labelsString);

        String[] labelsArray = labelsString.split(";");
        for (String labelArray : labelsArray) {
            String[] labelKeyAndValue = labelArray.split(":");

            if (labelKeyAndValue.length != 2) {
                throw new LabelParseException("invalid syntax");
            }

            addLabelPart(labelKeyAndValue[0], labelKeyAndValue[1]);
        }
    }

    private void parseLabelsMap(Map<String, Object> labelsMap) throws LabelParseException {
        for (Entry entry : labelsMap.entrySet()) {
            if (entry.getValue() == null) {
                throw new LabelParseException("empty value");
            }

            addLabelPart((String) entry.getKey(), entry.getValue().toString());
        }
    }

    private void addLabelPart(String key, String value) throws LabelParseException {
        key = validateLabelPart(key);
        value = validateLabelPart(value);

        if (labels.size() == 64) {
            Agent.LOG
                    .log(Level.WARNING, "Exceeded 64 label limit - only the first 64 labels will be sent to New Relic");
            return;
        }

        labels.put(key, value);
    }

    public void writeJSONString(Writer out) throws IOException {
        List jsonLabels = new ArrayList(labels.size());
        for (Entry entry : labels.entrySet()) {
            final String name = (String) entry.getKey();
            final String value = (String) entry.getValue();
            jsonLabels.add(new JSONStreamAware() {
                public void writeJSONString(Writer out) throws IOException {
                    JSONObject.writeJSONString(ImmutableMap.of("label_type", name, "label_value", value), out);
                }
            });
        }
        JSONArray.writeJSONString(jsonLabels, out);
    }

    private static class LabelParseException extends Exception {
        public LabelParseException(String message) {
            super();
        }
    }
}