package com.newrelic.agent.tracers.servlet;

import com.newrelic.agent.Agent;
import com.newrelic.agent.logging.IAgentLogger;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.stats.ResponseTimeStats;
import com.newrelic.agent.stats.SimpleStatsEngine;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Request;
import java.text.MessageFormat;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueueTimeTracker
{
  protected static final String REQUEST_X_QUEUE_START_HEADER = "X-Queue-Start";
  private static final Pattern REQUEST_X_QUEUE_HEADER_PATTERN = Pattern.compile("\\s*(?:t=)?(-?[0-9.]+)");
  private final long queueTime;

  private QueueTimeTracker(Request httpRequest, long txStartTimeInNanos)
  {
    String requestXQueueStartHeader = ExternalTimeTracker.getRequestHeader(httpRequest, "X-Queue-Start");

    this.queueTime = initQueueTime(requestXQueueStartHeader, txStartTimeInNanos);
  }

  private long initQueueTime(String requestXQueueStartHeader, long txStartTimeInNanos) {
    long queueStartTimeInNanos = getQueueStartTimeFromHeader(requestXQueueStartHeader);
    if (queueStartTimeInNanos > 0L) {
      long queueTime = txStartTimeInNanos - queueStartTimeInNanos;
      if (Agent.LOG.isLoggable(Level.FINEST)) {
        String msg = MessageFormat.format("Transaction start time (nanoseconds): {0}, queue start time (nanoseconds): {1}, queue time (nanoseconds): {2}", new Object[] { Long.valueOf(txStartTimeInNanos), Long.valueOf(queueStartTimeInNanos), Long.valueOf(queueTime) });

        Agent.LOG.finest(msg);
      }
      return Math.max(0L, queueTime);
    }
    return 0L;
  }

  private long getQueueStartTimeFromHeader(String requestXQueueStartHeader) {
    if (requestXQueueStartHeader != null) {
      Matcher matcher = REQUEST_X_QUEUE_HEADER_PATTERN.matcher(requestXQueueStartHeader);
      if (matcher.find()) {
        String queueStartTime = matcher.group(1);
        try {
          return ExternalTimeTracker.parseTimestampToNano(queueStartTime);
        } catch (NumberFormatException e) {
          String msg = MessageFormat.format("Error parsing queue start time {0} in {1} header: {2}", new Object[] { queueStartTime, "X-Queue-Start", e });

          Agent.LOG.log(Level.FINER, msg);
        }
      } else {
        String msg = MessageFormat.format("Failed to parse queue start time in {0} header: {1}", new Object[] { "X-Queue-Start", requestXQueueStartHeader });

        Agent.LOG.log(Level.WARNING, msg);
      }
    }
    return 0L;
  }

  public long getQueueTime()
  {
    return this.queueTime;
  }

  public void recordMetrics(TransactionStats statsEngine) {
    if (this.queueTime > 0L) {
      MetricName name = MetricName.QUEUE_TIME;
      statsEngine.getUnscopedStats().getResponseTimeStats(name.getName()).recordResponseTimeInNanos(this.queueTime);
    }
  }

  public static QueueTimeTracker create(Request httpRequest, long txStartTimeInNanos) {
    return new QueueTimeTracker(httpRequest, txStartTimeInNanos);
  }
}