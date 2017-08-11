package nars.control;

import jcog.Services;
import jcog.event.Ons;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

public class NARService extends Services.AbstractService<NAR> implements Termed {


    protected Ons ons;

    protected NARService(NAR nar) {
        nar.on(this);
    }

    protected void start(NAR nar) {
        ons = new Ons();
        ons.add(nar.eventClear.on(n -> clear()));
    }

    public void clear() {
        //default: nothing
    }


    @Override
    protected void stop(NAR nar) {
        ons.off();
        ons = null;
    }

    @Override
    public @NotNull Term term() {
        return $.p($.quote(getClass().getName()), $.the(System.identityHashCode(this)) );
    }

}
