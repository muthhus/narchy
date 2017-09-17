package nars.op.mental;

import nars.$;
import nars.NAR;
import nars.Task;
import nars.bag.leak.LeakOut;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Reflector extends LeakOut {

    final static Logger logger = LoggerFactory.getLogger(Reflector.class);

    final static float VOL_RATIO_MAX = 2.5f; //estimate
    private final NAR n;

    public Reflector(NAR n, int cap, float rate) {
        super(n, cap, rate);
        this.n = n;
    }

    @Override
    public boolean preFilter(Task t) {
        if (super.preFilter(t)) {
            Term tt = t.term();
            if (tt.size() > 1) {
                if (tt.volume() <= n.termVolumeMax.intValue() / VOL_RATIO_MAX)
                    return true;
            }
        }
        return false;
    }

    final static Atomic REFLECT_OP = Atomic.the("reflect");

    @Override
    protected float leak(Task out) {
        Term x = out.term().conceptual();
        Term reflectionSim = $.sim($.func(REFLECT_OP, x), x).eval(n);
        if (reflectionSim.size() > 0 && reflectionSim.volume() <= n.termVolumeMax.intValue()) {
            try {
                n.believe(reflectionSim);
            } catch (Throwable t) {
                return 0;
            }
            logger.info("reflect {}", reflectionSim);
            return 1;
        }
        return 0;
    }
}
