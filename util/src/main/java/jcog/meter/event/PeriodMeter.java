/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jcog.meter.event;

import jcog.meter.FunctionMeter;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

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
    final DescriptiveStatistics stat;
    private double prev = Double.NaN;
    private final boolean frequency;
    
    public PeriodMeter(String id, boolean nanoOrMilli, int window, boolean asFrequency) {
        super(id, true, ".min", ".max", ".mean", ".stddev");

        this.stat = new DescriptiveStatistics(window);
        this.nanoOrMilli = nanoOrMilli;
        frequency = asFrequency;
        reset();
    }
    
     public static double now(boolean nanoSeconds /* TODO use a Resolution enum */) {
         return nanoSeconds ? System.nanoTime() : System.currentTimeMillis() * 1.0E6;
    }


    

    public void reset() {
        stat.clear();
    }

    public void hit() {
        hit(1);
    }

    public void hit(int n) {
        double now = now(nanoOrMilli);
        if (prev == prev) {
            double dt = now - prev;
            for (int i = 0; i < n; i++)
                stat.addValue(dt);
        }
        prev = now;
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

}
