package com.newrelic.agent.stats;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import com.newrelic.deps.org.json.simple.JSONArray;

public class ApdexStatsImpl implements ApdexStats {
    private static final Integer ZERO = Integer.valueOf(0);
    private int satisfying;
    private int tolerating;
    private int frustrating;
    private long apdexTInMillis = ZERO.intValue();

    protected ApdexStatsImpl() {
    }

    public ApdexStatsImpl(int s, int t, int f) {
        this.satisfying = s;
        this.tolerating = t;
        this.frustrating = f;
    }

    public Object clone() throws CloneNotSupportedException {
        ApdexStatsImpl newStats = new ApdexStatsImpl();
        newStats.frustrating = this.frustrating;
        newStats.satisfying = this.satisfying;
        newStats.tolerating = this.tolerating;
        return newStats;
    }

    public String toString() {
        return super.toString() + " [s=" + this.satisfying + ", t=" + this.tolerating + ", f=" + this.frustrating + "]";
    }

    public void recordApdexFrustrated() {
        this.frustrating += 1;
    }

    public int getApdexSatisfying() {
        return this.satisfying;
    }

    public int getApdexTolerating() {
        return this.tolerating;
    }

    public int getApdexFrustrating() {
        return this.frustrating;
    }

    public void recordApdexResponseTime(long responseTimeMillis, long apdexTInMillis) {
        this.apdexTInMillis = apdexTInMillis;
        ApdexPerfZone perfZone = ApdexPerfZone.getZone(responseTimeMillis, apdexTInMillis);
        switch (perfZone) {
            case SATISFYING:
                this.satisfying += 1;
                break;
            case TOLERATING:
                this.tolerating += 1;
                break;
            case FRUSTRATING:
                recordApdexFrustrated();
        }
    }

    public boolean hasData() {
        return (this.satisfying > 0) || (this.tolerating > 0) || (this.frustrating > 0);
    }

    public void reset() {
        this.satisfying = 0;
        this.tolerating = 0;
        this.frustrating = 0;
    }

    public void writeJSONString(Writer writer) throws IOException {
        double apdexT = Long.valueOf(this.apdexTInMillis).doubleValue() / 1000.0D;

        List data = Arrays.asList(new Number[] {Integer.valueOf(this.satisfying), Integer.valueOf(this.tolerating),
                                                       Integer.valueOf(this.frustrating), Double.valueOf(apdexT),
                                                       Double.valueOf(apdexT), ZERO});

        JSONArray.writeJSONString(data, writer);
    }

    public void merge(StatsBase statsObj) {
        if ((statsObj instanceof ApdexStatsImpl)) {
            ApdexStatsImpl stats = (ApdexStatsImpl) statsObj;

            this.satisfying += stats.satisfying;
            this.tolerating += stats.tolerating;
            this.frustrating += stats.frustrating;
        }
    }
}