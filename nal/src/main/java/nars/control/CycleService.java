package nars.control;

import nars.NAR;

import java.util.function.Consumer;

abstract public class CycleService extends NARService implements Consumer<NAR> {

    public CycleService(NAR nar) {
        super(nar);
    }

    @Override
    protected void start(NAR nar)  {
        super.start(nar);
        ons.add(nar.onCycle(this));
    }

}
