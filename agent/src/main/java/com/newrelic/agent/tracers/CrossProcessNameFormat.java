package com.newrelic.agent.tracers;

import java.io.UnsupportedEncodingException;
import java.text.MessageFormat;

import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.parser.JSONParser;
import com.newrelic.deps.org.json.simple.parser.ParseException;

import com.newrelic.agent.Agent;
import com.newrelic.agent.tracers.metricname.MetricNameFormat;
import com.newrelic.agent.util.Obfuscator;
import com.newrelic.agent.util.Strings;

public class CrossProcessNameFormat implements MetricNameFormat {
    private final String transactionName;
    private final String crossProcessId;
    private final String hostName;
    private final String uri;
    private final String transactionId;

    private CrossProcessNameFormat(String transactionName, String crossProcessId, String hostName, String uri,
                                   String transactionId) {
        this.hostName = hostName;
        this.crossProcessId = crossProcessId;
        this.transactionName = transactionName;
        this.uri = uri;
        this.transactionId = transactionId;
    }

    public static CrossProcessNameFormat create(String host, String uri, String decodedAppData) {
        if (decodedAppData == null) {
            return null;
        }

        if ((host == null) || (host.length() == 0)) {
            return null;
        }
        try {
            JSONParser parser = new JSONParser();
            JSONArray arr = (JSONArray) parser.parse(decodedAppData);
            String crossProcessId = (String) arr.get(0);
            String transactionName = (String) arr.get(1);
            String transactionId = null;
            if (arr.size() > 5) {
                transactionId = (String) arr.get(5);
            }
            return new CrossProcessNameFormat(transactionName, crossProcessId, host, uri, transactionId);
        } catch (ParseException ex) {
            if (Agent.LOG.isFinerEnabled()) {
                String msg = MessageFormat.format("Unable to parse application data {0}: {1}",
                                                         new Object[] {decodedAppData, ex});
                Agent.LOG.finer(msg);
            }
        }
        return null;
    }

    public static CrossProcessNameFormat create(String host, String uri, String encodedAppData, String encodingKey) {
        if (encodedAppData == null) {
            return null;
        }
        if (encodingKey == null) {
            return null;
        }
        if ((host == null) || (host.length() == 0)) {
            return null;
        }
        String decodedAppData = null;
        try {
            decodedAppData = Obfuscator.deobfuscateNameUsingKey(encodedAppData, encodingKey);
        } catch (UnsupportedEncodingException ex) {
            String msg =
                    MessageFormat.format("Error decoding application data {0}: {1}", new Object[] {encodedAppData, ex});
            Agent.LOG.error(msg);
            return null;
        }

        return create(host, uri, decodedAppData);
    }

    public String getHostCrossProcessIdRollupMetricName() {
        return Strings.join('/', new String[] {"ExternalApp", hostName, crossProcessId, "all"});
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(100);
        sb.append("host:").append(hostName).append(" crossProcessId:").append(crossProcessId)
                .append(" transactionName:").append(transactionName).append(" uri:").append(uri)
                .append(" transactionId:").append(transactionId);

        return sb.toString();
    }

    public String getMetricName() {
        return Strings.join('/', new String[] {"ExternalTransaction", hostName, crossProcessId, transactionName});
    }

    public String getTransactionSegmentName() {
        return getMetricName();
    }

    public String getTransactionSegmentUri() {
        return uri;
    }
}