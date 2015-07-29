//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;

public enum EjbTransactionJmxMetricGenerator {
    COUNT {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName, "Count", JmxAction.SUM_ALL, JmxType.MONOTONICALLY_INCREASING);
        }
    },
    COMMIT {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], "Committed", JmxType.MONOTONICALLY_INCREASING);
        }
    },
    ROLLBACK {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], "Rolled Back", JmxType.MONOTONICALLY_INCREASING);
        }
    },
    TIMEOUT {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], "Timed Out", JmxType.MONOTONICALLY_INCREASING);
        }
    };

    private EjbTransactionJmxMetricGenerator() {
    }

    public abstract JmxMetric createMetric(String... var1);
}
