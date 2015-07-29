package com.newrelic.agent.errors;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.attributes.AttributesUtils;
import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.deps.com.google.common.collect.Maps;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

public abstract class TracedError implements Comparable<TracedError>, JSONStreamAware {
    private final String path;
    private final long timestamp;
    private final String requestUri;
    private final String appName;
    private final Map<String, Map<String, String>> prefixAtts;
    private final Map<String, Object> userAtts;
    private final Map<String, Object> agentAtts;
    private final Map<String, String> errorAtts;
    private final Map<String, Object> intrinsics;

    public TracedError(String appName, String frontendMetricName, String requestPath, long timestamp,
                       Map<String, Map<String, String>> prefixedParams, Map<String, Object> userParams,
                       Map<String, Object> agentParams, Map<String, String> errorParams,
                       Map<String, Object> intrinsics) {
        this.appName = appName;
        path = (frontendMetricName == null ? "Unknown" : frontendMetricName);
        requestUri = (requestPath == null ? "Unknown" : requestPath);
        this.timestamp = timestamp;
        prefixAtts = setAtts(prefixedParams);
        userAtts = setAtts(userParams);
        agentAtts = setAtts(agentParams);
        errorAtts = setAtts(errorParams);
        this.intrinsics = setAtts(intrinsics);
    }

    private <V, K> Map<K, V> setAtts(Map<K, V> inputAtts) {
        if (inputAtts == null) {
            return Collections.emptyMap();
        }
        return inputAtts;
    }

    public abstract String getMessage();

    public abstract String getExceptionClass();

    public long getTimestamp() {
        return timestamp / 1000L;
    }

    public String getPath() {
        return path;
    }

    public abstract Collection<String> stackTrace();

    public Map<String, Collection<String>> stackTraces() {
        return Collections.emptyMap();
    }

    private Map<String, Object> getUserAtts() {
        Map atts = Maps.newHashMap();
        atts.putAll(errorAtts);
        atts.putAll(userAtts);
        return atts;
    }

    private Map<String, Object> getAgentAtts() {
        Map atts = Maps.newHashMap();
        atts.putAll(agentAtts);
        if ((prefixAtts != null) && (!prefixAtts.isEmpty())) {
            atts.putAll(AttributesUtils.appendAttributePrefixes(prefixAtts));
        }
        return atts;
    }

    private void filterAndAddIfNotEmpty(String key, Map<String, Object> wheretoAdd,
                                        Map<String, ? extends Object> toAdd) {
        Map output = ServiceFactory.getAttributesService().filterErrorAttributes(appName, toAdd);

        if ((output != null) && (!output.isEmpty())) {
            wheretoAdd.put(key, output);
        }
    }

    private Map<String, Object> getAttributes() {
        Map params = Maps.newHashMap();

        if (ServiceFactory.getAttributesService().isAttributesEnabledForErrors(appName)) {
            filterAndAddIfNotEmpty("agentAttributes", params, getAgentAtts());

            if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                filterAndAddIfNotEmpty("userAttributes", params, getUserAtts());
            }

        }

        if ((intrinsics != null) && (!intrinsics.isEmpty())) {
            params.put("intrinsics", intrinsics);
        }

        Collection stackTrace = stackTrace();
        if (stackTrace != null) {
            params.put("stack_trace", stackTrace);
        } else {
            Map stackTraces = stackTraces();
            if (stackTraces != null) {
                params.put("stack_traces", stackTraces);
            }
        }

        params.put("request_uri", requestUri);

        return params;
    }

    public void writeJSONString(Writer writer) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(new Object[] {Long.valueOf(getTimestamp()), getPath(), getMessage(),
                                                                     getExceptionClass(), getAttributes()}), writer);
    }

    public int compareTo(TracedError other) {
        return (int) (timestamp - other.timestamp);
    }

    public boolean incrementsErrorMetric() {
        return true;
    }
}