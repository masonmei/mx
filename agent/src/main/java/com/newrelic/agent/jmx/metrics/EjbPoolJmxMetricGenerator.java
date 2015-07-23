//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.jmx.metrics;

import com.newrelic.agent.jmx.JmxType;

public enum EjbPoolJmxMetricGenerator {
    SUCCESS {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName, "Attempts/Successful", JmxAction.SUBTRACT_ALL_FROM_FIRST,
                                           JmxType.MONOTONICALLY_INCREASING);
        }
    },
    THREADS_WAITING {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], "Threads/Waiting", JmxType.SIMPLE);
        }
    },
    DESTROY {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], "Beans/Destroyed", JmxType.MONOTONICALLY_INCREASING);
        }
    },
    FAILURE {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], "Attempts/Failed", JmxType.MONOTONICALLY_INCREASING);
        }
    },
    AVAILABLE {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], "Beans/Available", JmxType.SIMPLE);
        }
    },
    ACTIVE {
        public JmxMetric createMetric(String... pAttributeName) {
            return JmxMetric.create(pAttributeName[0], "Beans/Active", JmxType.SIMPLE);
        }
    };

    private EjbPoolJmxMetricGenerator() {
    }

    public abstract JmxMetric createMetric(String... var1);
}
