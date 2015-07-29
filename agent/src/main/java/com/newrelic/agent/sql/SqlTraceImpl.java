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
        blameMetricName = info.getBlameMetricName();
        metricName = info.getMetricName();
        uri = info.getRequestUri();
        sql = info.getSql();
        id = info.getId();
        callCount = info.getCallCount();
        total = info.getTotalInMillis();
        min = info.getMinInMillis();
        max = info.getMaxInMillis();
        parameters = info.getParameters();
    }

    public String getBlameMetricName() {
        return blameMetricName;
    }

    public String getMetricName() {
        return metricName;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public String getUri() {
        return uri;
    }

    public int getId() {
        return id;
    }

    public int getCallCount() {
        return callCount;
    }

    public long getMax() {
        return max;
    }

    public long getMin() {
        return min;
    }

    public String getSql() {
        return sql;
    }

    public long getTotal() {
        return total;
    }

    public void writeJSONString(Writer out) throws IOException {
        JSONArray.writeJSONString(Arrays.asList(new Serializable[] {blameMetricName, uri, Integer.valueOf(id), sql,
                                                                           metricName, Integer.valueOf(callCount),
                                                                           Long.valueOf(total), Long.valueOf(min),
                                                                           Long.valueOf(max), DataSenderWriter
                                                                                                      .getJsonifiedCompressedEncodedString(parameters,
                                                                                                                                                  out)}),
                                         out);
    }
}