package spacegraph.web;

import jcog.Util;
import jcog.data.MutableInteger;

/**
 *
 */
public abstract class PeriodicWebsocketService extends WebsocketService implements Runnable {

    public final MutableInteger updatePeriodMS;
    private Thread thread;

    public PeriodicWebsocketService(int updatePeriodMS) {
        super();

        this.updatePeriodMS = new MutableInteger(updatePeriodMS);
    }


    @Override
    public void run() {
        while (thread!=null) {
            update();
            Util.sleep(updatePeriodMS.intValue());
        }
    }

    abstract protected void update();

    @Override
    public void onStart() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void onStop() {
        thread.interrupt();
        thread = null;
    }
}
