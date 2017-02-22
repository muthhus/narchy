/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.meter.event;

import jcog.Util;
import jcog.meter.FunctionMeter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummary;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

/**
 * Measures the period between hit() calls to determine frequency-related
 * statistics of hit intervals.
 * millisecond resolution performs better than nanoseconds but both are
 * converted to a double value (nanosecond unixtime) for calculation.
 * 
 * the results can be returned either as period duration (sec) or frequency (1/period).
 */
public class PeriodMeter extends FunctionMeter<Double> {
    
    private final boolean nanoOrMilli;
    final StatisticalSummary stat;
    private final String id;
    private double prev = Double.NaN;
    private final boolean frequency;
    
    public PeriodMeter(String id, boolean nanoOrMilli, int window, boolean asFrequency) {
        super(id, true, ".min", ".max", ".mean", ".stddev");

        this.id = id;
        this.stat = window > 0 ? new DescriptiveStatistics(window) : new SummaryStatistics();
        this.nanoOrMilli = nanoOrMilli;
        frequency = asFrequency;
        clear();
    }
    
     public static double now(boolean nanoSeconds /* TODO use a Resolution enum */) {
         return nanoSeconds ? System.nanoTime() : System.currentTimeMillis() * 1.0E6;
    }


    



    public void hit() {
        hit(1);
    }

    public void hit(int n) {
        double now = now(nanoOrMilli);
        if (prev == prev) {
            double dt = now - prev;
            for (int i = 0; i < n; i++)
                hit(dt);
        }
        prev = now;
    }

    /** record a specific duration */
    public void hit(double dt) {
        if (stat instanceof DescriptiveStatistics)
            ((DescriptiveStatistics)stat).addValue(dt);
        else
            ((SummaryStatistics)stat).addValue(dt);
    }
    public void clear() {
        if (stat instanceof DescriptiveStatistics)
            ((DescriptiveStatistics)stat).clear();
        else
            ((SummaryStatistics)stat).clear();
    }

    @Override
    public Double getValue(Object key, int index) {
        if (stat.getN() == 0) return null;
        switch (index) {
            case 0: return f(stat.getMin());
            case 1: return f(stat.getMax());
            case 2: return f(stat.getMean());
            case 4: return stat.getStandardDeviation();
        }
        return null;
    }
    
    protected double f(double period) {
        if (frequency) {
            if (period == 0) return Double.POSITIVE_INFINITY;
            return 1.0/period;
        }            
        return period;
    }


    public double mean() {
        return this.stat.getMean();
    }
    public double sum() {
        return this.stat.getSum();
    }

    @Override
    public String toString() {

        return id + "{" + Util.secondStr(stat.getMean()) + "AVG x " + stat.getN() + "= " + Util.secondStr(stat.getSum()) + "}";
    }

}
