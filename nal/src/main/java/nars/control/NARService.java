package nars.control;

import com.google.common.util.concurrent.AbstractIdleService;
import jcog.event.Ons;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;

public class NARService extends AbstractIdleService implements Termed {

    public final NAR nar;
    protected Ons ons;

    protected NARService(NAR nar) {
        this.nar = nar;
        nar.runLater(()-> nar.add(term(), this));
    }

    @Override
    protected void startUp() throws Exception {
        ons = new Ons();
        ons.add(nar.eventClear.on(n -> clear()));
    }

    public void clear() {
        //default: nothing
    }

    @Override
    protected void shutDown() throws Exception {
        ons.off();
        ons = null;
    }

    @Override
    public @NotNull Term term() {
        return $.quote(getClass() + "@" + System.identityHashCode(this));
    }

}
