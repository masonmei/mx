package com.newrelic.agent.jmx.values;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.newrelic.agent.jmx.create.JmxMetricModifier;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;
import com.newrelic.agent.jmx.metrics.KafkaMetricGenerator;

public class KafkaConsumerJmxValues extends JmxFrameworkValues {
    protected static final Pattern BYTES_RECEIVED = Pattern.compile("^JMX/\"(.+)-(.+?)-BytesPerSec\"/");
    protected static final Pattern MESSAGES_RECEIVED = Pattern.compile("^JMX/\"(.+)-(.+?)-MessagesPerSec\"/");
    private static final JmxMetricModifier TOPIC_MODIFIER = new JmxMetricModifier() {
        public String getMetricName(String fullMetricName) {
            Matcher m = KafkaConsumerJmxValues.BYTES_RECEIVED.matcher(fullMetricName);
            if ((m.matches()) && (m.groupCount() == 2)) {
                return "MessageBroker/Kafka/Topic/Consume/Named/" + m.group(2) + "/Received/Bytes";
            }
            m = KafkaConsumerJmxValues.MESSAGES_RECEIVED.matcher(fullMetricName);
            if ((m.matches()) && (m.groupCount() == 2)) {
                return "MessageBroker/Kafka/Topic/Consume/Named/" + m.group(2) + "/Received/Messages";
            }
            return "";
        }
    };
    private static final JmxMetric COUNT = KafkaMetricGenerator.COUNT_MONOTONIC.createMetric("Count");
    public static String PREFIX = "kafka.consumer";
    private static List<BaseJmxValue> METRICS = new ArrayList(1);

    static {
        METRICS.add(new BaseJmxValue("\"kafka.consumer\":type=\"ConsumerTopicMetrics\",name=*", "JMX/{name}/",
                                            TOPIC_MODIFIER, new JmxMetric[] {COUNT}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}