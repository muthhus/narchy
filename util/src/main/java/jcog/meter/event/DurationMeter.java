///*
// * To change this license header, choose License Headers in Project Properties.
// * To change this template file, choose Tools | Templates
// * and open the template in the editor.
// */
//package jcog.meter.event;
//
//import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
//
///**
// * Measures the period between start() and end() calls as a ValueMeter value.
// */
//public class DurationMeter extends DoubleMeter {
//
//	private final DescriptiveStatistics stat;
//	private double startTime = Double.NaN;
//	private final boolean nanoSeconds;
//	// DescriptiveStatistics stat;
//	//private final double window;
//	// private double prev;
//	private final boolean frequency;
//	private final boolean strict = false;
//
//	public DurationMeter(String id, boolean nanoSeconds, int window, boolean asFrequency) {
//		super(id);
//
//		//window = windowSec * 1.0E9;
//		this.stat = new DescriptiveStatistics(window);
//		// this.stat = new DescriptiveStatistics();
//		this.nanoSeconds = nanoSeconds;
//		frequency = asFrequency;
//		reset();
//	}
//
//	public boolean isStarted() {
//		return !Double.isNaN(startTime);
//	}
//
//	/** returns the stored start time of the event */
//	public double start() {
//		if (strict && isStarted()) {
//			startTime = Double.NaN;
//			throw new RuntimeException(this + " already started");
//		}
//		startTime = PeriodMeter.now(nanoSeconds);
//		return startTime;
//	}
//
//	/** returns the value which it stores (duration time, or frequency) */
//	public double stop() {
//		if (strict && !isStarted())
//			throw new RuntimeException(this + " not previously started");
//		double duration = sinceStart();
//		double v = frequency ? (1.0 / duration) : duration;
//		set(v);
//		startTime = Double.NaN;
//		return v;
//	}
//
//	public synchronized double sinceStart() {
//		double resolutionTime = nanoSeconds ? 1.0E9 : 1.0E3;
//		return (PeriodMeter.now(nanoSeconds) - startTime) / resolutionTime;
//	}
//
//}
