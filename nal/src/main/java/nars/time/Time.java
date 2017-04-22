package nars.time;

import java.io.Serializable;

/**
 * Time state
 */
public interface Time extends Serializable {

	//Timer real = new Timer("Realtime");

	/** called when memory reset */
	void clear();

	/** returns the current time, as measured in units determined by this clock */
	long time();

	/** returns a new stamp evidence id */
	long nextStamp();

		/** called each cycle */
	void cycle();

	default void cycle(int ticks) {
		for (int i = 0; i < ticks; i++)
			cycle();
	}

	long elapsed();

	/** the default duration applied to input tasks that do not specify one */
	int dur();

	/** set the duration, return this
	 * @param d*/
    Time dur(int d);
}
