package jcog.meter;

import jcog.meter.resource.MemoryUsageMeter;
import jcog.meter.resource.ThreadCPUTimeTracker;

/**
 * Awareness of available and consumed resources, such as: real-time,
 * computation time, memory, energy, I/O, etc..
 */
public class ResourceMeter {

	public final MemoryUsageMeter CYCLE_RAM_USED = new MemoryUsageMeter(
			"ram.used");

	/** the cpu time of each cycle */
	public final ThreadCPUTimeTracker CYCLE_CPU_TIME = new ThreadCPUTimeTracker(
			"cpu.time");


}
