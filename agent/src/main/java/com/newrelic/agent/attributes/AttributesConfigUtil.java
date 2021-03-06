package com.newrelic.agent.attributes;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.config.AgentConfig;
import com.newrelic.agent.config.BaseConfig;
import com.newrelic.deps.com.google.common.collect.Sets;

public class AttributesConfigUtil {
    public static final String IGNORED_PARAMS = "ignored_params";
    public static final String IGNORED_MESSAGING_PARAMS = "ignored_messaging_params";
    protected static final String[] DEFAULT_BROWSER_EXCLUDES =
            {"request.parameters.*", "message.parameters.*", "library.solr.*", "jvm.*", "httpResponseMessage",
                    "request.headers.referer", "httpResponseCode", "host.displayName", "process.instanceName"};
    protected static final String[] DEFAULT_EVENTS_EXCLUDES =
            {"request.parameters.*", "message.parameters.*", "library.solr.*", "jvm.*", "httpResponseMessage",
                    "request.headers.referer"};
    protected static final String[] DEFAULT_ERRORS_EXCLUDES = new String[0];
    protected static final String[] DEFAULT_TRACES_EXCLUDES = new String[0];
    protected static final String ATTS_ENABLED = "attributes.enabled";
    protected static final String ATTS_EXCLUDE = "attributes.exclude";
    protected static final String ATTS_INCLUDE = "attributes.include";
    protected static final String CAPTURE_ATTRIBUTES = ".capture_attributes";

    protected static boolean isCaptureAttributes(AgentConfig config) {
        return getBooleanValue(config, "capture_params", Boolean.FALSE);
    }

    protected static boolean isCaptureMessageAttributes(AgentConfig config) {
        return getBooleanValue(config, "capture_messaging_params", Boolean.FALSE);
    }

    protected static boolean isAttsEnabled(AgentConfig config, boolean defaultProp, String[] dest) {
        Boolean enabledRoot = config.getValue(ATTS_ENABLED);
        if ((enabledRoot != null) && (!enabledRoot)) {
            return enabledRoot;
        }

        boolean toEnable = false;
        Boolean destEnabled;
        for (String current : dest) {
            destEnabled = getBooleanValue(config, current + "." + ATTS_ENABLED);
            if (destEnabled != null) {
                if (!destEnabled) {
                    return destEnabled;
                }
                toEnable = true;
            }
        }

        boolean toCapture = false;
        for (String current : dest) {
            destEnabled = getBooleanValue(config, current + CAPTURE_ATTRIBUTES);
            if (destEnabled != null) {
                if (!destEnabled) {
                    return destEnabled;
                }
                toCapture = true;
            }

        }

        return (toEnable) || (toCapture) || defaultProp;
    }

    private static Boolean getBooleanValue(AgentConfig config, String value) {
        return getBooleanValue(config, value, null);
    }

    private static Boolean getBooleanValue(AgentConfig config, String value, Object theDefault) {
        try {
            Object inputObj = config.getValue(value, theDefault);
            if (inputObj != null) {
                if ((inputObj instanceof Boolean)) {
                    return (Boolean) inputObj;
                }
                if ((inputObj instanceof String)) {
                    return Boolean.parseBoolean((String) inputObj);
                }
            }
        } catch (Exception e) {
            Agent.LOG.log(Level.FINE, MessageFormat
                                              .format("The configuration property {0} should be a boolean but is not.",
                                                             value));
        }

        return null;
    }

    protected static List<String> getBaseList(AgentConfig config, String key, String prefix) {
        Object val = config.getValue(key);
        if ((val instanceof String)) {
            return BaseConfig.getUniqueStringsFromString((String) val, ",", prefix);
        }
        if ((val instanceof Collection)) {
            return BaseConfig.getUniqueStringsFromCollection((Collection) val, prefix);
        }
        return Collections.emptyList();
    }

    protected static List<String> getBaseList(AgentConfig config, String key) {
        return getBaseList(config, key, null);
    }

    protected static Set<String> getExcluded(AgentConfig config, List<String> baseList, String dest) {
        Set<String> output = Sets.newHashSet();
        output.addAll(baseList);
        output.addAll(getBaseList(config, dest + "." + ATTS_EXCLUDE));
        return output;
    }

    protected static Set<String> getIncluded(AgentConfig config, List<String> baseList, String dest) {
        Set<String> output = Sets.newHashSet();
        output.addAll(baseList);
        output.addAll(getBaseList(config, dest + "." + ATTS_INCLUDE));
        return output;
    }
}