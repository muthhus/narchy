package nars.time;

import java.io.Serializable;

/**
 * Time state
 */
public interface Time extends Serializable {

    //Timer real = new Timer("Realtime");

    /**
     * called when memory reset
     */
    void clear();

    /**
     * returns the current time, as measured in units determined by this clock
     */
    long time();

    /**
     * returns a new stamp evidence id
     */
    long nextStamp();

    /**
     * called each cycle
     */
    void cycle();


    long elapsed();

    /**
     * the default duration applied to input tasks that do not specify one
     * >0
     */
    int dur();

    /**
     * set the duration, return this
     *
     * @param d, d>0
     */
    Time dur(int d);

    default long[] nextInputStamp() {
        return new long[]{nextStamp()};
    }


}
