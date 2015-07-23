package com.newrelic.agent.tracers.metricname;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.tracers.ClassMethodSignature;
import com.newrelic.agent.util.Strings;

public class MetricNameFormats {
    private static final Pattern METRIC_NAME_REPLACE = Pattern.compile("${className}", 16);

    public static MetricNameFormat replaceFirstSegment(MetricNameFormat metricName, String newSegmentName) {
        String metricNameString = metricName.getMetricName();
        String txName = metricName.getTransactionSegmentName();

        String newMetricName = replaceFirstSegment(metricNameString, newSegmentName);
        String newTxName;
        if (metricNameString.equals(txName)) {
            newTxName = newMetricName;
        } else {
            newTxName = replaceFirstSegment(txName, newSegmentName);
        }
        return new SimpleMetricNameFormat(newMetricName, newTxName);
    }

    private static String replaceFirstSegment(String name, String newSegmentName) {
        String[] segments = name.split("/");
        segments[0] = newSegmentName;
        return Strings.join('/', segments);
    }

    public static MetricNameFormat getFormatter(Object invocationTarget, ClassMethodSignature sig, String metricName,
                                                int flags) {
        if (null == metricName) {
            return sig.getMetricNameFormat(invocationTarget, flags);
        }
        return new SimpleMetricNameFormat(getTracerMetricName(invocationTarget, sig.getClassName(), metricName));
    }

    private static String getTracerMetricName(Object invocationTarget, String className, String metricName) {
        Matcher matcher = METRIC_NAME_REPLACE.matcher(metricName);

        return matcher.replaceFirst(Matcher.quoteReplacement(invocationTarget == null ? className
                                                                     : invocationTarget.getClass().getName()));
    }
}