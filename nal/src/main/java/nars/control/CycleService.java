package nars.control;

import nars.NAR;

abstract public class CycleService extends NARService implements Runnable {

    public CycleService(NAR nar) {
        super(nar);
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();
        ons.add(nar.onCycle(this));
    }



}
