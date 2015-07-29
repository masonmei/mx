//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;

public enum DataSourceJmxMetricGenerator {
  CONNECTIONS_AVAILABLE {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Connections/Available", JmxType.SIMPLE);
    }
  },
  CONNECTIONS_POOL_SIZE {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Connections/PoolSize", JmxType.SIMPLE);
    }
  },
  CONNECTIONS_CREATED {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Connections/Created", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  CONNECTIONS_ACTIVE {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Connections/Active", JmxType.SIMPLE);
    }
  },
  CONNECTIONS_LEAKED {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Connections/Leaked", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  CONNECTIONS_CACHE_SIZE {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Statement Cache/Size", JmxType.SIMPLE);
    }
  },
  CONNECTION_REQUEST_WAITING_COUNT {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Requests/Currently Waiting", JmxType.SIMPLE);
    }
  },
  CONNECTION_REQUEST_TOTAL_COUNT {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Requests/Count", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  CONNECTION_REQUEST_SUCCESS {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Requests/Successful", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  CONNECTION_REQUEST_FAILURE {
    public JmxMetric createMetric(String... pAttributeName) {
      return JmxMetric.create(pAttributeName[0], "Requests/Failed", JmxType.MONOTONICALLY_INCREASING);
    }
  };

  private DataSourceJmxMetricGenerator() {
  }

  public abstract JmxMetric createMetric(String... var1);
}
