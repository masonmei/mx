package com.newrelic.agent.tracers.servlet;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.stats.TransactionStats;
import com.newrelic.api.agent.Request;

public class ExternalTimeTracker {
    static final long EARLIEST_ACCEPTABLE_TIMESTAMP_NANO = 946684800000000000L;
    static final List<Integer> MAGNITUDES =
            Arrays.asList(new Integer[] {Integer.valueOf(1), Integer.valueOf(1000), Integer.valueOf(1000000),
                                                Integer.valueOf(1000000000)});
    private final QueueTimeTracker queueTimeTracker;
    private final ServerTimeTracker serverTimeTracker;
    private final long externalTime;

    private ExternalTimeTracker(Request httpRequest, long txStartTimeInMillis) {
        if (httpRequest == null) {
            Agent.LOG.finer("Unable to get headers: HttpRequest is null");
        }
        long txStartTimeInNanos = parseTimestampToNano(txStartTimeInMillis);
        this.queueTimeTracker = QueueTimeTracker.create(httpRequest, txStartTimeInNanos);
        long queueTime = this.queueTimeTracker.getQueueTime();
        this.serverTimeTracker = ServerTimeTracker.create(httpRequest, txStartTimeInNanos, queueTime);
        long serverTime = this.serverTimeTracker.getServerTime();
        this.externalTime = TimeUnit.MILLISECONDS.convert(queueTime + serverTime, TimeUnit.NANOSECONDS);
    }

    protected static String getRequestHeader(Request httpRequest, String headerName) {
        if (httpRequest == null) {
            return null;
        }
        try {
            String header = httpRequest.getHeader(headerName);
            if ((header != null) && (Agent.LOG.isLoggable(Level.FINER))) {
                String msg = MessageFormat.format("Got {0} header: {1}", new Object[] {headerName, header});
                Agent.LOG.finer(msg);
            }

            return header;
        } catch (Throwable t) {
            String msg = MessageFormat.format("Error getting {0} header: {1}", new Object[] {headerName, t.toString()});
            if (Agent.LOG.isLoggable(Level.FINEST)) {
                Agent.LOG.log(Level.FINEST, msg, t);
            } else {
                Agent.LOG.finer(msg);
            }
        }
        return null;
    }

    public static ExternalTimeTracker create(Request httpRequest, long txStartTimeInMillis) {
        return new ExternalTimeTracker(httpRequest, txStartTimeInMillis);
    }

    public static long parseTimestampToNano(String strTime) throws NumberFormatException {
        double time = Double.parseDouble(strTime);
        return parseTimestampToNano(time);
    }

    public static long parseTimestampToNano(double time) throws NumberFormatException {
        Iterator i$;
        if (time > 0.0D) {
            for (i$ = MAGNITUDES.iterator(); i$.hasNext(); ) {
                long magnitude = ((Integer) i$.next()).intValue();
                long candidate = (long) (time * magnitude);
                if ((946684800000000000L < candidate) && (candidate < 4954167440812867584L)) {
                    return candidate;
                }
            }
        }
        throw new NumberFormatException("The long " + time
                                                + " could not be converted to a timestamp in nanoseconds (wrong "
                                                + "magnitude).");
    }

    public long getExternalTime() {
        return this.externalTime;
    }

    public void recordMetrics(TransactionStats statsEngine) {
        this.queueTimeTracker.recordMetrics(statsEngine);
        this.serverTimeTracker.recordMetrics(statsEngine);
    }
}