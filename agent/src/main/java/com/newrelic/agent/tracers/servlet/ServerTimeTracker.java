package com.newrelic.agent.tracers.servlet;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.Agent;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Request;

public class ServerTimeTracker {
    public static final String REQUEST_X_START_HEADER = "X-Request-Start";
    protected static final String MISSING_SERVER_NAME_PREFIX = "unknown";
    private static final Pattern REQUEST_X_START_HEADER_PATTERN =
            Pattern.compile("([^\\s\\/,=\\(\\)]+)? ?t=(-?[0-9.]+)|(-?[0-9.]+)");
    private final long totalServerTime;
    private final Map<String, Long> serverTimes;

    private ServerTimeTracker(Request httpRequest, long txStartTimeInNanos, long queueTime) {
        String requestXStartHeader = ExternalTimeTracker.getRequestHeader(httpRequest, "X-Request-Start");
        if (requestXStartHeader == null) {
            serverTimes = Collections.emptyMap();
            totalServerTime = 0L;
        } else {
            serverTimes = new HashMap();
            totalServerTime = initRequestTime(requestXStartHeader, txStartTimeInNanos, queueTime);
        }
    }

    public static ServerTimeTracker create(Request httpRequest, long txStartTimeInNanos, long queueTime) {
        return new ServerTimeTracker(httpRequest, txStartTimeInNanos, queueTime);
    }

    private long initRequestTime(String requestXStartHeader, long txStartTimeInNanos, long queueTime) {
        long totalServerTime = 0L;
        int index = 0;
        long lastServerStartTimeInNanos = 0L;
        String lastServerName = null;
        Matcher matcher = REQUEST_X_START_HEADER_PATTERN.matcher(requestXStartHeader);
        while (matcher.find()) {
            index++;
            String serverName = matcher.group(1);
            if ((serverName == null) || (serverName.length() == 0)) {
                serverName = "unknown" + String.valueOf(index);
            }
            long serverStartTimeInNanos = 0L;
            String serverStartTime = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
            try {
                serverStartTimeInNanos = ExternalTimeTracker.parseTimestampToNano(serverStartTime);
                if (lastServerName != null) {
                    long serverTimeInNanos = Math.max(0L, serverStartTimeInNanos - lastServerStartTimeInNanos);
                    if (Agent.LOG.isLoggable(Level.FINEST)) {
                        String msg = MessageFormat.format("{0} start time (nanoseconds): {1}, {2} start time (in "
                                                                  + "nanoseconds): {3}, {4} time (in nanoseconds): {5}",
                                                                 new Object[] {serverName,
                                                                                      Long.valueOf(serverStartTimeInNanos),
                                                                                      lastServerName,
                                                                                      Long.valueOf(lastServerStartTimeInNanos),
                                                                                      lastServerName,
                                                                                      Long.valueOf(serverTimeInNanos)});

                        Agent.LOG.finest(msg);
                    }
                    if (serverTimeInNanos > 0L) {
                        serverTimes.put(lastServerName, Long.valueOf(serverTimeInNanos));
                        totalServerTime += serverTimeInNanos;
                    }
                }
                lastServerStartTimeInNanos = serverStartTimeInNanos;
                lastServerName = serverName;
            } catch (NumberFormatException e) {
                String msg = MessageFormat.format("Error parsing server time {0} in {1}: {2}",
                                                         new Object[] {serverStartTime, "X-Request-Start", e});

                Agent.LOG.log(Level.FINER, msg);
            }
        }
        if (lastServerName != null) {
            long serverTimeInNanos = Math.max(0L, txStartTimeInNanos - lastServerStartTimeInNanos - queueTime);
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                String msg = MessageFormat.format("Transaction start time (nanoseconds): {0}, {1} start time (in "
                                                          + "nanoseconds): {2}, queue time (in nanoseconds): {3}, {4}"
                                                          + " time" + " (in nanoseconds): {5}",
                                                         new Object[] {Long.valueOf(txStartTimeInNanos), lastServerName,
                                                                              Long.valueOf(lastServerStartTimeInNanos),
                                                                              Long.valueOf(queueTime), lastServerName,
                                                                              Long.valueOf(serverTimeInNanos)});

                Agent.LOG.finest(msg);
            }
            if (serverTimeInNanos > 0L) {
                serverTimes.put(lastServerName, Long.valueOf(serverTimeInNanos));
                totalServerTime += serverTimeInNanos;
            }
        }
        return totalServerTime;
    }

    public long getServerTime() {
        return totalServerTime;
    }

    public void recordMetrics(TransactionStats statsEngine) {
        if (totalServerTime > 0L) {
            recordAllServerTimeMetric(statsEngine, totalServerTime);
            for (Entry entry : serverTimes.entrySet()) {
                recordServerTimeMetric(statsEngine, (String) entry.getKey(), ((Long) entry.getValue()).longValue());
            }
        }
    }

    private void recordServerTimeMetric(TransactionStats statsEngine, String serverName, long serverTime) {
        String name = "WebFrontend/WebServer/" + serverName;
        statsEngine.getUnscopedStats().getResponseTimeStats(name).recordResponseTimeInNanos(serverTime);
        if (Agent.LOG.isLoggable(Level.FINEST)) {
            String msg = MessageFormat.format("Recorded metric: {0}, value: {1}",
                                                     new Object[] {name, String.valueOf(serverTime)});
            Agent.LOG.finest(msg);
        }
    }

    private void recordAllServerTimeMetric(TransactionStats statsEngine, long totalServerTime) {
        MetricName name = MetricName.QUEUE_TIME;
        statsEngine.getUnscopedStats().getResponseTimeStats(name.getName()).recordResponseTimeInNanos(totalServerTime);
    }
}