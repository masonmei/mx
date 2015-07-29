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

public class KafkaProducerJmxValues extends JmxFrameworkValues {
    protected static final Pattern BYTES_SENT = Pattern.compile("^JMX/\"(.+)-(.+?)-BytesPerSec\"/");
    protected static final Pattern MESSAGES_SENT = Pattern.compile("^JMX/\"(.+)-(.+?)-MessagesPerSec\"/");
    protected static final Pattern MESSAGES_DROPPED = Pattern.compile("^JMX/\"(.+)-(.+?)-DroppedMessagesPerSec\"/");
    private static final JmxMetricModifier TOPIC_MODIFIER = new JmxMetricModifier() {
        public String getMetricName(String fullMetricName) {
            Matcher m = KafkaProducerJmxValues.BYTES_SENT.matcher(fullMetricName);
            if ((m.matches()) && (m.groupCount() == 2)) {
                return "MessageBroker/Kafka/Topic/Produce/Named/" + m.group(2) + "/Sent/Bytes";
            }
            m = KafkaProducerJmxValues.MESSAGES_SENT.matcher(fullMetricName);
            if ((m.matches()) && (m.groupCount() == 2)) {
                return "MessageBroker/Kafka/Topic/Produce/Named/" + m.group(2) + "/Sent/Messages";
            }
            m = KafkaProducerJmxValues.MESSAGES_DROPPED.matcher(fullMetricName);
            if ((m.matches()) && (m.groupCount() == 2)) {
                return "MessageBroker/Kafka/Topic/Produce/Named/" + m.group(2) + "/Dropped Messages";
            }
            return "";
        }
    };
    private static final JmxMetric COUNT = KafkaMetricGenerator.COUNT_MONOTONIC.createMetric("Count");
    public static String PREFIX = "kafka.producer";
    private static List<BaseJmxValue> METRICS = new ArrayList(1);

    static {
        METRICS.add(new BaseJmxValue("\"kafka.producer\":type=\"ProducerTopicMetrics\",name=*", "JMX/{name}/",
                                            TOPIC_MODIFIER, new JmxMetric[] {COUNT}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}