//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.stats;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;

import com.newrelic.agent.Agent;
import com.newrelic.agent.MetricData;
import com.newrelic.agent.metric.MetricIdRegistry;
import com.newrelic.agent.metric.MetricName;
import com.newrelic.agent.normalization.Normalizer;

public class StatsEngineImpl implements StatsEngine {
  public static final int DEFAULT_CAPACITY = 140;
  public static final int DEFAULT_SCOPED_CAPACITY = 32;
  public static final int DOUBLE = 2;
  private static final float HASH_SET_LOAD_FACTOR = 0.75F;
  private final SimpleStatsEngine unscopedStats;
  private final Map<String, SimpleStatsEngine> scopedStats;

  public StatsEngineImpl() {
    this(DEFAULT_CAPACITY);
  }

  public StatsEngineImpl(int capacity) {
    this.unscopedStats = new SimpleStatsEngine(capacity);
    this.scopedStats = new HashMap(capacity);
  }

  static List<MetricData> aggregate(MetricIdRegistry metricIdRegistry, List<MetricData> result) {
    if (metricIdRegistry.getSize() == 0) {
      return result;
    } else {
      int hashMapSize = (int) ((float) result.size() / HASH_SET_LOAD_FACTOR) + 1;
      HashMap data = new HashMap(hashMapSize);
      Iterator i$ = result.iterator();

      while (i$.hasNext()) {
        MetricData md = (MetricData) i$.next();
        MetricData existing = (MetricData) data.get(md.getKey());
        if (existing == null) {
          data.put(md.getKey(), md);
        } else {
          existing.getStats().merge(md.getStats());
        }
      }

      if (data.size() == result.size()) {
        return result;
      } else {
        return new ArrayList(data.values());
      }
    }
  }

  public Stats getStats(String name) {
    return this.getStats(MetricName.create(name));
  }

  public Stats getStats(MetricName metricName) {
    if (metricName == null) {
      throw new RuntimeException("Cannot get a stat for a null metric");
    } else {
      return this.getStatsEngine(metricName).getStats(metricName.getName());
    }
  }

  public void recordEmptyStats(String name) {
    this.recordEmptyStats(MetricName.create(name));
  }

  public void recordEmptyStats(MetricName metricName) {
    if (metricName == null) {
      throw new RuntimeException("Cannot create stats for a null metric");
    } else {
      this.getStatsEngine(metricName).recordEmptyStats(metricName.getName());
    }
  }

  private SimpleStatsEngine getStatsEngine(MetricName metricName) {
    if (metricName.isScoped()) {
      SimpleStatsEngine statsEngine = (SimpleStatsEngine) this.scopedStats.get(metricName.getScope());
      if (statsEngine == null) {
        statsEngine = new SimpleStatsEngine(DEFAULT_SCOPED_CAPACITY);
        this.scopedStats.put(metricName.getScope(), statsEngine);
      }

      return statsEngine;
    } else {
      return this.unscopedStats;
    }
  }

  public ResponseTimeStats getResponseTimeStats(String name) {
    return this.getResponseTimeStats(MetricName.create(name));
  }

  public ResponseTimeStats getResponseTimeStats(MetricName metricName) {
    if (metricName == null) {
      throw new RuntimeException("Cannot get a stat for a null metric");
    } else {
      return this.getStatsEngine(metricName).getResponseTimeStats(metricName.getName());
    }
  }

  public ApdexStats getApdexStats(MetricName metricName) {
    if (metricName == null) {
      throw new RuntimeException("Cannot get a stat for a null metric");
    } else {
      return this.getStatsEngine(metricName).getApdexStats(metricName.getName());
    }
  }

  public List<MetricName> getMetricNames() {
    ArrayList result = new ArrayList(this.getSize());
    Iterator i$ = this.unscopedStats.getStatsMap().keySet().iterator();

    while (i$.hasNext()) {
      String entry = (String) i$.next();
      result.add(MetricName.create(entry));
    }

    i$ = this.scopedStats.entrySet().iterator();

    while (i$.hasNext()) {
      Entry entry1 = (Entry) i$.next();
      Iterator i$1 = ((SimpleStatsEngine) entry1.getValue()).getStatsMap().keySet().iterator();

      while (i$1.hasNext()) {
        String name = (String) i$1.next();
        result.add(MetricName.create(name, (String) entry1.getKey()));
      }
    }

    return result;
  }

  public void clear() {
    this.unscopedStats.clear();
    this.scopedStats.clear();
  }

  public int getSize() {
    int size = this.unscopedStats.getStatsMap().size();

    SimpleStatsEngine engine;
    for (Iterator i$ = this.scopedStats.values().iterator(); i$.hasNext(); size += engine.getStatsMap().size()) {
      engine = (SimpleStatsEngine) i$.next();
    }

    return size;
  }

  public void mergeStats(StatsEngine statsEngine) {
    if (statsEngine instanceof StatsEngineImpl) {
      this.mergeStats((StatsEngineImpl) statsEngine);
    }

  }

  private void mergeStats(StatsEngineImpl other) {
    this.unscopedStats.mergeStats(other.unscopedStats);

    Entry<String, SimpleStatsEngine> entry;
    SimpleStatsEngine scopedStatsEngine;
    Iterator<Entry<String, SimpleStatsEngine>> iterator = other.scopedStats.entrySet().iterator();
    for (; iterator.hasNext(); scopedStatsEngine.mergeStats(entry.getValue())) {
      entry = iterator.next();
      scopedStatsEngine = this.scopedStats.get(entry.getKey());
      if (scopedStatsEngine == null) {
        scopedStatsEngine = new SimpleStatsEngine((entry.getValue()).getSize());
        this.scopedStats.put(entry.getKey(), scopedStatsEngine);
      }
    }

  }

  public void mergeStatsResolvingScope(TransactionStats txStats, String resolvedScope) {
    this.unscopedStats.mergeStats(txStats.getUnscopedStats());
    if (resolvedScope != null) {
      SimpleStatsEngine scopedStatsEngine = (SimpleStatsEngine) this.scopedStats.get(resolvedScope);
      if (scopedStatsEngine == null) {
        scopedStatsEngine = new SimpleStatsEngine(txStats.getScopedStats().getSize());
        this.scopedStats.put(resolvedScope, scopedStatsEngine);
      }

      scopedStatsEngine.mergeStats(txStats.getScopedStats());
    }
  }

  public List<MetricData> getMetricData(Normalizer metricNormalizer, MetricIdRegistry metricIdRegistry) {
    ArrayList result = new ArrayList(this.unscopedStats.getStatsMap().size()
                                             + this.scopedStats.size() * DEFAULT_SCOPED_CAPACITY * DOUBLE);
    Iterator i$ = this.scopedStats.entrySet().iterator();

    while (i$.hasNext()) {
      Entry entry = (Entry) i$.next();
      result.addAll(((SimpleStatsEngine) entry.getValue())
                            .getMetricData(metricNormalizer, metricIdRegistry, (String) entry.getKey()));
    }

    result.addAll(this.createUnscopedCopies(metricNormalizer, metricIdRegistry, result));
    result.addAll(this.unscopedStats.getMetricData(metricNormalizer, metricIdRegistry, ""));
    return aggregate(metricIdRegistry, result);
  }

  private List<MetricData> createUnscopedCopies(Normalizer metricNormalizer, MetricIdRegistry metricIdRegistry,
                                                List<MetricData> scopedMetrics) {
    int size = (int) ((double) scopedMetrics.size() / 0.75D) + DOUBLE;
    HashMap allUnscopedMetrics = new HashMap(size);
    ArrayList results = new ArrayList(scopedMetrics.size());
    Iterator i$ = scopedMetrics.iterator();

    while (i$.hasNext()) {
      MetricData scoped = (MetricData) i$.next();
      String theMetricName = scoped.getMetricName().getName();
      MetricData unscopedMetric =
              this.getUnscopedCloneOfData(metricNormalizer, metricIdRegistry, theMetricName, scoped.getStats());
      if (unscopedMetric != null) {
        MetricData mapUnscoped = (MetricData) allUnscopedMetrics.get(theMetricName);
        if (mapUnscoped == null) {
          allUnscopedMetrics.put(theMetricName, unscopedMetric);
          results.add(unscopedMetric);
        } else {
          mapUnscoped.getStats().merge(unscopedMetric.getStats());
        }
      }
    }

    return results;
  }

  private MetricData getUnscopedCloneOfData(Normalizer metricNormalizer, MetricIdRegistry metricIdRegistry,
                                            String metricName, StatsBase stats) {
    if (stats != null) {
      MetricName metricNameUnscoped = MetricName.create(metricName);

      try {
        MetricData e = SimpleStatsEngine.createMetricData(metricNameUnscoped, (StatsBase) stats.clone(),
                                                                 metricNormalizer, metricIdRegistry);
        return e;
      } catch (CloneNotSupportedException var7) {
        Agent.LOG.log(Level.INFO, "Unscoped metric not created because stats base could not be cloned for "
                                          + metricNameUnscoped.getName());
        return null;
      }
    } else {
      return null;
    }
  }
}
