package com.newrelic.agent.service.analytics;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.stats.ApdexPerfZone;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONObject;

public class TransactionEvent extends AnalyticsEvent {
    static final float UNASSIGNED = (1.0F / -1.0F);
    static final int UNASSIGNED_INT = -2147483648;
    static final String TYPE = "Transaction";
    final String guid;
    final String referrerGuid;
    final String tripId;
    final Integer referringPathHash;
    final String alternatePathHashes;
    final ApdexPerfZone apdexPerfZone;
    final String syntheticsResourceId;
    final String syntheticsMonitorId;
    final String syntheticsJobId;
    final int port;
    final String name;
    final float duration;
    Integer pathHash;
    float queueDuration = (1.0F / -1.0F);

    float externalDuration = (1.0F / -1.0F);

    float externalCallCount = (1.0F / -1.0F);

    float databaseDuration = (1.0F / -1.0F);

    float databaseCallCount = (1.0F / -1.0F);

    float gcCumulative = (1.0F / -1.0F);
    Map<String, Object> agentAttributes;
    String appName;

    public TransactionEvent(String appName, String subType, long timestamp, String name, float duration, String guid,
                            String referringGuid, Integer port, String tripId, Integer referringPathHash,
                            String alternatePathHashes, ApdexPerfZone apdexPerfZone, String syntheticsResourceId,
                            String syntheticsMonitorId, String syntheticsJobId) {
        super("Transaction", timestamp);
        this.name = name;
        this.duration = duration;
        this.guid = guid;
        this.referrerGuid = referringGuid;
        this.tripId = tripId;
        this.referringPathHash = referringPathHash;
        this.alternatePathHashes = alternatePathHashes;
        this.port = (port == null ? -2147483648 : port.intValue());
        this.appName = appName;
        this.apdexPerfZone = apdexPerfZone;
        this.syntheticsResourceId = syntheticsResourceId;
        this.syntheticsMonitorId = syntheticsMonitorId;
        this.syntheticsJobId = syntheticsJobId;
    }

    public void writeJSONString(Writer out) throws IOException {
        JSONObject obj = new JSONObject();
        obj.put("type", this.type);
        obj.put("timestamp", Long.valueOf(this.timestamp));
        obj.put("name", this.name);
        obj.put("duration", Float.valueOf(this.duration));

        if (this.apdexPerfZone != null) {
            obj.put("apdexPerfZone", this.apdexPerfZone.getZone());
        }

        if (this.guid != null) {
            obj.put("nr.guid", this.guid);
        }
        if (this.referrerGuid != null) {
            obj.put("nr.referringTransactionGuid", this.referrerGuid);
        }
        if (this.tripId != null) {
            obj.put("nr.tripId", this.tripId);
        }
        if (this.pathHash != null) {
            obj.put("nr.pathHash", ServiceUtils.intToHexString(this.pathHash.intValue()));
        }
        if (this.referringPathHash != null) {
            obj.put("nr.referringPathHash", ServiceUtils.intToHexString(this.referringPathHash.intValue()));
        }
        if (this.alternatePathHashes != null) {
            obj.put("nr.alternatePathHashes", this.alternatePathHashes);
        }
        if (this.syntheticsResourceId != null) {
            obj.put("nr.syntheticsResourceId", this.syntheticsResourceId);
        }
        if (this.syntheticsMonitorId != null) {
            obj.put("nr.syntheticsMonitorId", this.syntheticsMonitorId);
        }
        if (this.syntheticsJobId != null) {
            obj.put("nr.syntheticsJobId", this.syntheticsJobId);
        }
        if (this.port != -2147483648) {
            obj.put("port", Integer.valueOf(this.port));
        }

        if (this.queueDuration != (1.0F / -1.0F)) {
            obj.put("queueDuration", Float.valueOf(this.queueDuration));
        }
        if (this.externalDuration != (1.0F / -1.0F)) {
            obj.put("externalDuration", Float.valueOf(this.externalDuration));
        }
        if (this.externalCallCount > 0.0F) {
            obj.put("externalCallCount", Float.valueOf(this.externalCallCount));
        }
        if (this.databaseDuration != (1.0F / -1.0F)) {
            obj.put("databaseDuration", Float.valueOf(this.databaseDuration));
        }
        if (this.databaseCallCount > 0.0F) {
            obj.put("databaseCallCount", Float.valueOf(this.databaseCallCount));
        }
        if (this.gcCumulative != (1.0F / -1.0F)) {
            obj.put("gcCumulative", Float.valueOf(this.gcCumulative));
        }

        Map filteredUserAtts = getUserFilteredMap(this.userAttributes);
        Map filteredAgentAtts = getFilteredMap(this.agentAttributes);
        if (filteredAgentAtts.isEmpty()) {
            if (filteredUserAtts.isEmpty()) {
                JSONArray.writeJSONString(Arrays.asList(new JSONObject[] {obj}), out);
            } else {
                JSONArray.writeJSONString(Arrays.asList(new Map[] {obj, filteredUserAtts}), out);
            }
        } else {
            JSONArray.writeJSONString(Arrays.asList(new Map[] {obj, filteredUserAtts, filteredAgentAtts}), out);
        }
    }

    private Map<String, ? extends Object> getFilteredMap(Map<String, Object> input) {
        return ServiceFactory.getAttributesService().filterEventAttributes(this.appName, input);
    }

    private Map<String, ? extends Object> getUserFilteredMap(Map<String, Object> input) {
        if (!ServiceFactory.getConfigService().getDefaultAgentConfig().isHighSecurity()) {
            return getFilteredMap(input);
        }
        return Collections.emptyMap();
    }

    public boolean isValid() {
        return true;
    }
}