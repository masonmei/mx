package com.newrelic.agent.stats;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;

public class ApdexStatsImpl implements ApdexStats {
    private static final Integer ZERO = Integer.valueOf(0);
    private int satisfying;
    private int tolerating;
    private int frustrating;
    private long apdexTInMillis = ZERO.intValue();

    protected ApdexStatsImpl() {
    }

    public ApdexStatsImpl(int s, int t, int f) {
        satisfying = s;
        tolerating = t;
        frustrating = f;
    }

    public Object clone() throws CloneNotSupportedException {
        ApdexStatsImpl newStats = new ApdexStatsImpl();
        newStats.frustrating = frustrating;
        newStats.satisfying = satisfying;
        newStats.tolerating = tolerating;
        return newStats;
    }

    public String toString() {
        return super.toString() + " [s=" + satisfying + ", t=" + tolerating + ", f=" + frustrating + "]";
    }

    public void recordApdexFrustrated() {
        frustrating += 1;
    }

    public int getApdexSatisfying() {
        return satisfying;
    }

    public int getApdexTolerating() {
        return tolerating;
    }

    public int getApdexFrustrating() {
        return frustrating;
    }

    public void recordApdexResponseTime(long responseTimeMillis, long apdexTInMillis) {
        this.apdexTInMillis = apdexTInMillis;
        ApdexPerfZone perfZone = ApdexPerfZone.getZone(responseTimeMillis, apdexTInMillis);
        switch (perfZone.ordinal()) {
            case 1:
                satisfying += 1;
                break;
            case 2:
                tolerating += 1;
                break;
            case 3:
                recordApdexFrustrated();
        }
    }

    public boolean hasData() {
        return (satisfying > 0) || (tolerating > 0) || (frustrating > 0);
    }

    public void reset() {
        satisfying = 0;
        tolerating = 0;
        frustrating = 0;
    }

    public void writeJSONString(Writer writer) throws IOException {
        double apdexT = Long.valueOf(apdexTInMillis).doubleValue() / 1000.0D;

        List data = Arrays.asList(new Number[] {Integer.valueOf(satisfying), Integer.valueOf(tolerating),
                                                       Integer.valueOf(frustrating), Double.valueOf(apdexT),
                                                       Double.valueOf(apdexT), ZERO});

        JSONArray.writeJSONString(data, writer);
    }

    public void merge(StatsBase statsObj) {
        if ((statsObj instanceof ApdexStatsImpl)) {
            ApdexStatsImpl stats = (ApdexStatsImpl) statsObj;

            satisfying += stats.satisfying;
            tolerating += stats.tolerating;
            frustrating += stats.frustrating;
        }
    }
}