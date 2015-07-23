package com.newrelic.agent.bridge;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

import com.newrelic.api.agent.ApplicationNamePriority;
import com.newrelic.api.agent.InboundHeaders;
import com.newrelic.api.agent.Request;
import com.newrelic.api.agent.Response;

public class NoOpTransaction implements Transaction {
    public static final Transaction INSTANCE = new NoOpTransaction();
    public static final NoOpMap<String, Object> AGENT_ATTRIBUTES = new NoOpMap();

    public void beforeSendResponseHeaders() {
    }

    public boolean setTransactionName(com.newrelic.api.agent.TransactionNamePriority namePriority, boolean override,
                                      String category, String[] parts) {
        return false;
    }

    public boolean setTransactionName(TransactionNamePriority namePriority, boolean override, String category,
                                      String[] parts) {
        return false;
    }

    public boolean isTransactionNameSet() {
        return false;
    }

    public TracedMethod getLastTracer() {
        return null;
    }

    public TracedMethod getTracedMethod() {
        return null;
    }

    public boolean isStarted() {
        return false;
    }

    public void setApplicationName(ApplicationNamePriority priority, String appName) {
    }

    public boolean isAutoAppNamingEnabled() {
        return false;
    }

    public boolean isWebRequestSet() {
        return false;
    }

    public boolean isWebResponseSet() {
        return false;
    }

    public void setWebRequest(Request request) {
    }

    public WebResponse getWebResponse() {
        return NoOpWebResponse.INSTANCE;
    }

    public void setWebResponse(Response response) {
    }

    public void convertToWebTransaction() {
    }

    public boolean isWebTransaction() {
        return false;
    }

    public void requestInitialized(Request request, Response response) {
    }

    public void requestDestroyed() {
    }

    public void ignore() {
    }

    public void ignoreApdex() {
    }

    public void saveMessageParameters(Map<String, String> parameters) {
    }

    public CrossProcessState getCrossProcessState() {
        return NoOpCrossProcessState.INSTANCE;
    }

    public TracedMethod startFlyweightTracer() {
        return null;
    }

    public void finishFlyweightTracer(TracedMethod parent, long startInNanos, long finishInNanos, String className,
                                      String methodName, String methodDesc, String metricName,
                                      String[] rollupMetricNames) {
    }

    public Map<String, Object> getAgentAttributes() {
        return AGENT_ATTRIBUTES;
    }

    public boolean registerAsyncActivity(Object activityContext) {
        return false;
    }

    public boolean startAsyncActivity(Object activityContext) {
        return false;
    }

    public boolean ignoreAsyncActivity(Object activityContext) {
        return false;
    }

    public void provideHeaders(InboundHeaders headers) {
    }

    public String getRequestMetadata() {
        return NoOpCrossProcessState.INSTANCE.getRequestMetadata();
    }

    public void processRequestMetadata(String metadata) {
    }

    public String getResponseMetadata() {
        return NoOpCrossProcessState.INSTANCE.getResponseMetadata();
    }

    public void processResponseMetadata(String metadata) {
    }

    static final class NoOpMap<K, V> implements Map<K, V> {
        public int size() {
            return 0;
        }

        public boolean isEmpty() {
            return true;
        }

        public boolean containsKey(Object key) {
            return false;
        }

        public boolean containsValue(Object value) {
            return false;
        }

        public V get(Object key) {
            return null;
        }

        public V put(K key, V value) {
            return null;
        }

        public V remove(Object key) {
            return null;
        }

        public void putAll(Map<? extends K, ? extends V> m) {
        }

        public void clear() {
        }

        public Set<K> keySet() {
            return Collections.emptySet();
        }

        public Collection<V> values() {
            return Collections.emptyList();
        }

        public Set<Entry<K, V>> entrySet() {
            return Collections.emptySet();
        }
    }
}