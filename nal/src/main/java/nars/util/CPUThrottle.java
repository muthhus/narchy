package nars.util;

import jcog.Util;
import jcog.data.FloatParam;

public class CPUThrottle implements Runnable {

    /**
     * cpu throttle: 100% = full speed, 0 = paused
     */
    public final FloatParam cpu = new FloatParam(0.5f, 0f, 1f);

    /**
     * fundamental frequency; context period
     */
    static int cycleMS = 50;
    private long nextCycle;

    public final Runnable looped;
    private boolean pause;

    public CPUThrottle(Runnable looped) {
        this.looped = looped;
    }


    public void pause(boolean b) {
        this.pause = b;
    }

    @Override
    public void run() {

        try {
            //long idleSince = ETERNAL;
            int idleCount = -1;
            while (true) {

                if (pause /* plan.isEmpty()*/) {
                    if (idleCount == -1) {
                        //idleSince = System.currentTimeMillis();
                        idleCount = 0;
                    } else {
                        idleCount++;
                    }
                    Util.pauseNext(idleCount);
                } else {
                    idleCount = -1;

                    int awakeTime = (int) (cycleMS * cpu.asFloat());
                    if (awakeTime > 0) {

                        nextCycle = System.currentTimeMillis() + awakeTime;

                        looped.run();

                        nextCycle = Long.MIN_VALUE; //TODO use AtomicLong?

                        //plan.commit().sample(this::exec);
                    }
                    int sleepTime = (int) (cycleMS * (1f - cpu.asFloat()));
                    if (sleepTime > 0) {
                        Util.stall(sleepTime);
                    }
                }


            }
        } catch (Exception stopped) {

        } finally {

        }
    }

    public boolean active() {
        long c = this.nextCycle;
        return c == Long.MIN_VALUE ? false : System.currentTimeMillis() < c;
    }

}
