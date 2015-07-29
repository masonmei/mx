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
        this.path = (frontendMetricName == null ? "Unknown" : frontendMetricName);
        this.requestUri = (requestPath == null ? "Unknown" : requestPath);
        this.timestamp = timestamp;
        this.prefixAtts = setAtts(prefixedParams);
        this.userAtts = setAtts(userParams);
        this.agentAtts = setAtts(agentParams);
        this.errorAtts = setAtts(errorParams);
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
        return this.timestamp / 1000L;
    }

    public String getPath() {
        return this.path;
    }

    public abstract Collection<String> stackTrace();

    public Map<String, Collection<String>> stackTraces() {
        return Collections.emptyMap();
    }

    private Map<String, Object> getUserAtts() {
        Map atts = Maps.newHashMap();
        atts.putAll(this.errorAtts);
        atts.putAll(this.userAtts);
        return atts;
    }

    private Map<String, Object> getAgentAtts() {
        Map atts = Maps.newHashMap();
        atts.putAll(this.agentAtts);
        if ((this.prefixAtts != null) && (!this.prefixAtts.isEmpty())) {
            atts.putAll(AttributesUtils.appendAttributePrefixes(this.prefixAtts));
        }
        return atts;
    }

    private void filterAndAddIfNotEmpty(String key, Map<String, Object> wheretoAdd,
                                        Map<String, ? extends Object> toAdd) {
        Map output = ServiceFactory.getAttributesService().filterErrorAttributes(this.appName, toAdd);

        if ((output != null) && (!output.isEmpty())) {
            wheretoAdd.put(key, output);
        }
    }

    private Map<String, Object> getAttributes() {
        Map params = Maps.newHashMap();

        if (ServiceFactory.getAttributesService().isAttributesEnabledForErrors(this.appName)) {
            filterAndAddIfNotEmpty("agentAttributes", params, getAgentAtts());

            if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
                filterAndAddIfNotEmpty("userAttributes", params, getUserAtts());
            }

        }

        if ((this.intrinsics != null) && (!this.intrinsics.isEmpty())) {
            params.put("intrinsics", this.intrinsics);
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

        params.put("request_uri", this.requestUri);

        return params;
    }

    public void writeJSONString(Writer writer) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(new Object[] {Long.valueOf(getTimestamp()), getPath(), getMessage(),
                                                                     getExceptionClass(), getAttributes()}), writer);
    }

    public int compareTo(TracedError other) {
        return (int) (this.timestamp - other.timestamp);
    }

    public boolean incrementsErrorMetric() {
        return true;
    }
}