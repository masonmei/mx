//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;

public enum KafkaMetricGenerator {
  COUNT_MONOTONIC {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  VALUE_SIMPLE {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "", JmxType.SIMPLE);
    }
  },
  QUEUE_SIZE {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "", JmxType.SIMPLE);
    }
  },
  REQ_MEAN {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "", JmxType.SIMPLE);
    }
  };

  private KafkaMetricGenerator() {
  }

  public abstract JmxMetric createMetric(String var1);
}
