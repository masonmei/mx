//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.newrelic.agent.stats;

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.List;

import org.json.simple.JSONArray;

public abstract class AbstractStats implements CountStats {
    public static final StatsBase EMPTY_STATS;
    private static final List<Number> ZERO_ARRAY_LIST;

    static {
        Integer zero = Integer.valueOf(0);
        ZERO_ARRAY_LIST = Arrays.asList(new Number[] {zero, zero, zero, zero, zero, zero});
        EMPTY_STATS = new StatsBase() {
            public boolean hasData() {
                return true;
            }

            public void merge(StatsBase stats) {
            }

            public void reset() {
            }

            public Object clone() throws CloneNotSupportedException {
                return super.clone();
            }

            public void writeJSONString(Writer writer) throws IOException {
                JSONArray.writeJSONString(AbstractStats.ZERO_ARRAY_LIST, writer);
            }
        };
    }

    protected int count;

    public AbstractStats() {
    }

    public AbstractStats(int count) {
        this.count = count;
    }

    public void incrementCallCount(int value) {
        this.count += value;
    }

    public void incrementCallCount() {
        ++this.count;
    }

    public int getCallCount() {
        return this.count;
    }

    public void setCallCount(int count) {
        this.count = count;
    }

    public final void writeJSONString(Writer writer) throws IOException, InvalidStatsException {
        List list;
        if (this.count < 0) {
            list = ZERO_ARRAY_LIST;
        } else {
            list = Arrays.asList(new Number[] {Integer.valueOf(this.count), Float.valueOf(this.getTotal()),
                                                      Float.valueOf(this.getTotalExclusiveTime()),
                                                      Float.valueOf(this.getMinCallTime()),
                                                      Float.valueOf(this.getMaxCallTime()),
                                                      Double.valueOf(this.getSumOfSquares())});
        }

        JSONArray.writeJSONString(list, writer);
    }

    public abstract Object clone() throws CloneNotSupportedException;
}
