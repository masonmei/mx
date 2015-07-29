package com.newrelic.agent.util;

public class TimeConversion {
    public static final long NANOSECONDS_PER_SECOND = 1000000000L;
    public static final long MICROSECONDS_PER_SECOND = 1000000L;
    public static final long MILLISECONDS_PER_SECOND = 1000L;

    public static double convertMillisToSeconds(double millis) {
        return millis / 1000.0D;
    }

    public static double convertNanosToSeconds(double nanos) {
        return nanos / 1000000000.0D;
    }

    public static long convertSecondsToMillis(double seconds) {
        return (long) (seconds * 1000.0D);
    }

    public static long convertSecondsToNanos(double seconds) {
        return (long) (seconds * 1000000000.0D);
    }
}