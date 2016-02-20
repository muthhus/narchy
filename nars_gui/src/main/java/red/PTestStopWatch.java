//    RED - A Java Editor Library
//    Copyright (C) 2003  Robert Lichtenberger
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
package red;



/** A simple stopwatch class for performance tests 
  * @author rli@chello.at
  * @tier test
  */
public class PTestStopWatch {
	public PTestStopWatch() {
		fStart = fEnd = fSum = 0;
		fStops = 0;
	}
	
	/** start stopwatch timer */
	public void start() {
		fStart = System.currentTimeMillis();
	}
	
	/** stop stopwatch timer and printout time elapsed since last call of start or stop
	 * @param measurement String to printout before the time
	 */
	public void stop(String measurement) {
		System.out.println(measurement + ": " + stop() + " msec.");
	}
	
	/** stop stopwatch timer and return time elapsed since last call of start or stop in millisec.
	  * @return The time elapsed in msec.
	  */
	public long stop() {
		fEnd = System.currentTimeMillis();
		fStops++;
		fSum += (fEnd - fStart);
		long retVal = fEnd - fStart;
		fStart = System.currentTimeMillis();
		return retVal;
	}
	
	public void sum(String measurement) {
		System.out.println(fStops + " times " + measurement + ": " + fSum + " msec.");
		System.out.println("mean time: " + fSum / fStops);
	}
	
	private long fStart;
	private long fEnd;
	private long fSum;
	private int fStops;
}
