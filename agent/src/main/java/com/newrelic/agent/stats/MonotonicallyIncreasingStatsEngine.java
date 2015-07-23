package com.newrelic.agent.stats;

import java.util.HashMap;
import java.util.Map;

public class MonotonicallyIncreasingStatsEngine {
    private final Map<String, MonotonicallyIncreasingStatsHelper> monoStatsHelpers;

    public MonotonicallyIncreasingStatsEngine() {
        monoStatsHelpers = new HashMap();
    }

    public void recordMonoStats(StatsEngine statsEngine, String name, float value) {
        MonotonicallyIncreasingStatsHelper monoStatsHelper = getMonotonicallyIncreasingStatsHelper(name);
        Stats stats = statsEngine.getStats(name);
        monoStatsHelper.recordDataPoint(stats, value);
    }

    private MonotonicallyIncreasingStatsHelper getMonotonicallyIncreasingStatsHelper(String name) {
        MonotonicallyIncreasingStatsHelper monoStatsHelper =
                (MonotonicallyIncreasingStatsHelper) monoStatsHelpers.get(name);
        if (monoStatsHelper == null) {
            monoStatsHelper = new MonotonicallyIncreasingStatsHelper();
            monoStatsHelpers.put(name, monoStatsHelper);
        }
        return monoStatsHelper;
    }

    private class MonotonicallyIncreasingStatsHelper {
        private float lastValue = 0.0F;

        public MonotonicallyIncreasingStatsHelper() {
        }

        public void recordDataPoint(Stats stats, float value) {
            if (lastValue > value) {
                lastValue = 0.0F;
            }
            stats.recordDataPoint(value - lastValue);
            lastValue = value;
        }
    }
}