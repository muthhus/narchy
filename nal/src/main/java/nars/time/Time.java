package nars.time;

import java.io.Serializable;

/**
 * Time state
 */
public interface Time extends Serializable {

	/** called when memory reset */
	void clear();

	/** returns the current time, as measured in units determined by this clock */
	long time();

	/** returns a new stamp evidence id */
	long nextStamp();

		/** called each cycle */
	void tick();

	default void tick(int ticks) {
		for (int i = 0; i < ticks; i++)
			tick();
	}

	long elapsed();

    float duration();
}
