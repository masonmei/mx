package com.newrelic.agent.stats;

public class StatsImpl extends AbstractStats implements Stats {
    private float total;
    private float minValue;
    private float maxValue;
    private double sumOfSquares;

    protected StatsImpl() {
    }

    public StatsImpl(int count, float total, float minValue, float maxValue, double sumOfSquares) {
        super(count);
        this.total = total;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.sumOfSquares = sumOfSquares;
    }

    public Object clone() throws CloneNotSupportedException {
        StatsImpl newStats = new StatsImpl();
        newStats.count = count;
        newStats.total = total;
        newStats.minValue = minValue;
        newStats.maxValue = maxValue;
        newStats.sumOfSquares = sumOfSquares;
        return newStats;
    }

    public String toString() {
        return super.toString() + " [tot=" + total + ", min=" + minValue + ", maxV=" + maxValue + "]";
    }

    public void recordDataPoint(float value) {
        if ((Float.isNaN(value)) || (Float.isInfinite(value))) {
            throw new IllegalArgumentException("Data points must be numbers");
        }
        double sos = sumOfSquares + value * value;
        if (sos < sumOfSquares) {
            throw new IllegalArgumentException("Data value " + value + " caused sum of squares to roll over");
        }
        if (count > 0) {
            minValue = Math.min(value, minValue);
        } else {
            minValue = value;
        }
        count += 1;
        total += value;
        maxValue = Math.max(value, maxValue);
        sumOfSquares = sos;
    }

    public boolean hasData() {
        return (count > 0) || (total > 0.0F);
    }

    public void reset() {
        count = 0;
        total = (this.minValue = this.maxValue = 0.0F);
        sumOfSquares = 0.0D;
    }

    public float getTotal() {
        return total;
    }

    public float getTotalExclusiveTime() {
        return total;
    }

    public float getMinCallTime() {
        return minValue;
    }

    public float getMaxCallTime() {
        return maxValue;
    }

    public double getSumOfSquares() {
        return sumOfSquares;
    }

    public void merge(StatsBase statsObj) {
        if ((statsObj instanceof StatsImpl)) {
            StatsImpl stats = (StatsImpl) statsObj;
            if (stats.count > 0) {
                if (count > 0) {
                    minValue = Math.min(minValue, stats.minValue);
                } else {
                    minValue = stats.minValue;
                }
            }
            count += stats.count;
            total += stats.total;

            maxValue = Math.max(maxValue, stats.maxValue);
            sumOfSquares += stats.sumOfSquares;
        }
    }
}