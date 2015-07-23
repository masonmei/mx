package com.newrelic.agent.service.analytics;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.newrelic.agent.service.ServiceFactory;
import com.newrelic.agent.service.ServiceUtils;
import com.newrelic.agent.stats.ApdexPerfZone;

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
        referrerGuid = referringGuid;
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
        obj.put("type", type);
        obj.put("timestamp", Long.valueOf(timestamp));
        obj.put("name", name);
        obj.put("duration", Float.valueOf(duration));

        if (apdexPerfZone != null) {
            obj.put("apdexPerfZone", apdexPerfZone.getZone());
        }

        if (guid != null) {
            obj.put("nr.guid", guid);
        }
        if (referrerGuid != null) {
            obj.put("nr.referringTransactionGuid", referrerGuid);
        }
        if (tripId != null) {
            obj.put("nr.tripId", tripId);
        }
        if (pathHash != null) {
            obj.put("nr.pathHash", ServiceUtils.intToHexString(pathHash.intValue()));
        }
        if (referringPathHash != null) {
            obj.put("nr.referringPathHash", ServiceUtils.intToHexString(referringPathHash.intValue()));
        }
        if (alternatePathHashes != null) {
            obj.put("nr.alternatePathHashes", alternatePathHashes);
        }
        if (syntheticsResourceId != null) {
            obj.put("nr.syntheticsResourceId", syntheticsResourceId);
        }
        if (syntheticsMonitorId != null) {
            obj.put("nr.syntheticsMonitorId", syntheticsMonitorId);
        }
        if (syntheticsJobId != null) {
            obj.put("nr.syntheticsJobId", syntheticsJobId);
        }
        if (port != -2147483648) {
            obj.put("port", Integer.valueOf(port));
        }

        if (queueDuration != (1.0F / -1.0F)) {
            obj.put("queueDuration", Float.valueOf(queueDuration));
        }
        if (externalDuration != (1.0F / -1.0F)) {
            obj.put("externalDuration", Float.valueOf(externalDuration));
        }
        if (externalCallCount > 0.0F) {
            obj.put("externalCallCount", Float.valueOf(externalCallCount));
        }
        if (databaseDuration != (1.0F / -1.0F)) {
            obj.put("databaseDuration", Float.valueOf(databaseDuration));
        }
        if (databaseCallCount > 0.0F) {
            obj.put("databaseCallCount", Float.valueOf(databaseCallCount));
        }
        if (gcCumulative != (1.0F / -1.0F)) {
            obj.put("gcCumulative", Float.valueOf(gcCumulative));
        }

        Map filteredUserAtts = getUserFilteredMap(userAttributes);
        Map filteredAgentAtts = getFilteredMap(agentAttributes);
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
        return ServiceFactory.getAttributesService().filterEventAttributes(appName, input);
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