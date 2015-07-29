package com.newrelic.agent.tracers.metricname;

public class OtherTransSimpleMetricNameFormat implements MetricNameFormat {
    private final String metricName;
    private final String transactionSegmentName;

    public OtherTransSimpleMetricNameFormat(String metricName) {
        this.metricName = (this.transactionSegmentName = appendOtherTrans(metricName));
    }

    public OtherTransSimpleMetricNameFormat(String metricName, String transactionSegmentName) {
        this.metricName = appendOtherTrans(metricName);
        this.transactionSegmentName = transactionSegmentName;
    }

    private static String appendOtherTrans(String pMetricName) {
        if (pMetricName != null) {
            StringBuilder sb = new StringBuilder();
            if (!pMetricName.startsWith("OtherTransaction/")) {
                sb.append("OtherTransaction");

                if (!pMetricName.startsWith("/")) {
                    sb.append("/");
                }
            }

            sb.append(pMetricName);
            return sb.toString();
        }
        return pMetricName;
    }

    public final String getMetricName() {
        return this.metricName;
    }

    public String getTransactionSegmentName() {
        return this.transactionSegmentName;
    }

    public String getTransactionSegmentUri() {
        return null;
    }
}