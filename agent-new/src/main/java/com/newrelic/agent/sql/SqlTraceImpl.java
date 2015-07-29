package com.newrelic.agent.sql;

import java.io.IOException;
import java.io.Serializable;
import java.io.Writer;
import java.util.Arrays;
import java.util.Map;

import com.newrelic.agent.transport.DataSenderWriter;
import com.newrelic.deps.org.json.simple.JSONArray;
import com.newrelic.deps.org.json.simple.JSONStreamAware;

class SqlTraceImpl implements SqlTrace, JSONStreamAware {
    private final String blameMetricName;
    private final String metricName;
    private final String uri;
    private final String sql;
    private final int id;
    private final int callCount;
    private final long total;
    private final long max;
    private final long min;
    private final Map<String, Object> parameters;

    public SqlTraceImpl(SqlStatementInfo info) {
        this.blameMetricName = info.getBlameMetricName();
        this.metricName = info.getMetricName();
        this.uri = info.getRequestUri();
        this.sql = info.getSql();
        this.id = info.getId();
        this.callCount = info.getCallCount();
        this.total = info.getTotalInMillis();
        this.min = info.getMinInMillis();
        this.max = info.getMaxInMillis();
        this.parameters = info.getParameters();
    }

    public String getBlameMetricName() {
        return this.blameMetricName;
    }

    public String getMetricName() {
        return this.metricName;
    }

    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public String getUri() {
        return this.uri;
    }

    public int getId() {
        return this.id;
    }

    public int getCallCount() {
        return this.callCount;
    }

    public long getMax() {
        return this.max;
    }

    public long getMin() {
        return this.min;
    }

    public String getSql() {
        return this.sql;
    }

    public long getTotal() {
        return this.total;
    }

    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(new Serializable[] {this.blameMetricName, this.uri,
                                                                           Integer.valueOf(this.id), this.sql,
                                                                           this.metricName,
                                                                           Integer.valueOf(this.callCount),
                                                                           Long.valueOf(this.total),
                                                                           Long.valueOf(this.min),
                                                                           Long.valueOf(this.max), DataSenderWriter
                                                                                                           .getJsonifiedCompressedEncodedString(this.parameters,
                                                                                                                                                       out)}),
                                         out);
    }
}