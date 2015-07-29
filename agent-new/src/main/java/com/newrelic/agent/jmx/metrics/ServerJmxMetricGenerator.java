//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;

public enum ServerJmxMetricGenerator {
  MAX_THREAD_POOL_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Max", JmxType.SIMPLE);
    }
  },
  ACTIVE_THREAD_POOL_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Active", JmxType.SIMPLE);
    }
  },
  IDLE_THREAD_POOL_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Idle", JmxType.SIMPLE);
    }
  },
  STANDBY_THREAD_POOL_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Standby", JmxType.SIMPLE);
    }
  },
  SESSION_AVG_ALIVE_TIME {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "AverageAliveTime", JmxType.SIMPLE);
    }
  },
  SESSION_ACTIVE_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Active", JmxType.SIMPLE);
    }
  },
  SESSION_REJECTED_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Rejected", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  SESSION_EXPIRED_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Expired", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  TRANS_ACTIVE_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Currently/Active", JmxType.SIMPLE);
    }
  },
  TRANS_NESTED_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Created/Nested", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  TRANS_COMMITED_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Outcome/Committed", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  TRANS_ROLLED_BACK_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Outcome/Rolled Back", JmxType.MONOTONICALLY_INCREASING);
    }
  },
  TRANS_TOP_LEVEL_COUNT {
    public JmxMetric createMetric(String pAttributeName) {
      return JmxMetric.create(pAttributeName, "Created/Top Level", JmxType.MONOTONICALLY_INCREASING);
    }
  };

  private ServerJmxMetricGenerator() {
  }

  public abstract JmxMetric createMetric(String var1);
}
