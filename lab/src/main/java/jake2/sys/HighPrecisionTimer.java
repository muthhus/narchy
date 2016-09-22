///*
// * HighPrecisionTimer.java
// * Copyright (C) 2005
// *
// * $Id: HighPrecisionTimer.java,v 1.1 2005-07-01 14:11:00 hzi Exp $
// */
//package jake2.sys;
//
////import sun.misc.Perf;
//
//
//
//
//class HighPrecisionTimer extends Timer {
//
//	//private Perf perf = Perf.getPerf();
//	private double f = 1E-6;
//	private long base;
//
//	HighPrecisionTimer() {
//		base = System.nanoTime();
//	}
//
//	public long currentTimeMillis() {
//		long time = System.nanoTime();
//		long delta = time - base;
//		if (delta < 0) {
//			delta += Long.MAX_VALUE + 1;
//		}
//		return (long)(delta * f);
//	}
//}
