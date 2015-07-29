package com.newrelic.agent.jmx.values;

import java.util.ArrayList;
import java.util.List;

import com.newrelic.agent.jmx.JmxType;
import com.newrelic.agent.jmx.metrics.BaseJmxValue;
import com.newrelic.agent.jmx.metrics.JmxFrameworkValues;
import com.newrelic.agent.jmx.metrics.JmxMetric;

public class SolrJmxValues extends JmxFrameworkValues {
    private static final int METRIC_COUNT = 4;
    private static final List<BaseJmxValue> METRICS = new ArrayList(4);
    private static String PREFIX = "solr";

    static {
        METRICS.add(new BaseJmxValue("solr*:type=queryResultCache,*", null,
                                            new JmxMetric[] {JmxMetric.create("hitratio", JmxType.SIMPLE),
                                                                    JmxMetric.create("size", JmxType.SIMPLE), JmxMetric
                                                                                                                      .create("cumulative_hitratio",
                                                                                                                                     JmxType.SIMPLE),
                                                                    JmxMetric.create("lookups",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("hits",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("inserts",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("evictions",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_lookups",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_hits",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_inserts",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_evictions",
                                                                                            JmxType.MONOTONICALLY_INCREASING)}));

        METRICS.add(new BaseJmxValue("solr*:type=filterCache,*", null,
                                            new JmxMetric[] {JmxMetric.create("hitratio", JmxType.SIMPLE),
                                                                    JmxMetric.create("size", JmxType.SIMPLE), JmxMetric
                                                                                                                      .create("cumulative_hitratio",
                                                                                                                                     JmxType.SIMPLE),
                                                                    JmxMetric.create("lookups",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("hits",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("inserts",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("evictions",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_lookups",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_hits",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_inserts",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_evictions",
                                                                                            JmxType.MONOTONICALLY_INCREASING)}));

        METRICS.add(new BaseJmxValue("solr*:type=documentCache,*", null,
                                            new JmxMetric[] {JmxMetric.create("hitratio", JmxType.SIMPLE),
                                                                    JmxMetric.create("size", JmxType.SIMPLE), JmxMetric
                                                                                                                      .create("cumulative_hitratio",
                                                                                                                                     JmxType.SIMPLE),
                                                                    JmxMetric.create("lookups",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("hits",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("evictions",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("evictions",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_lookups",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_hits",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_inserts",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_evictions",
                                                                                            JmxType.MONOTONICALLY_INCREASING)}));

        METRICS.add(new BaseJmxValue("solr*:type=updateHandler,*", null,
                                            new JmxMetric[] {JmxMetric.create("docsPending", JmxType.SIMPLE), JmxMetric
                                                                                                                      .create("expungeDeletes",
                                                                                                                                     JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("rollbacks",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("optimizes",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("autocommits",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("commits",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("errors",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("adds",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("deletesById",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("deletesByQuery",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_adds",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_deletesById",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_deletesByQuery",
                                                                                            JmxType.MONOTONICALLY_INCREASING),
                                                                    JmxMetric.create("cumulative_errors",
                                                                                            JmxType.MONOTONICALLY_INCREASING)}));
    }

    public List<BaseJmxValue> getFrameworkMetrics() {
        return METRICS;
    }

    public String getPrefix() {
        return PREFIX;
    }
}