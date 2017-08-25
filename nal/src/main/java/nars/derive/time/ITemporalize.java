package nars.derive.time;

import nars.control.Derivation;
import nars.term.Term;
import nars.term.subst.Subst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

import static nars.time.Tense.ETERNAL;

public interface ITemporalize {



    /**
     * warning: for external use only; all internal calls should use solve(target, trail) to prevent stack overflow
     */
    default Event solve(Term target) {
        return solve(target, new HashMap<>(target.volume()));
    }

    void knowDerivedTerm(Subst d, Term term, long start, long end);

    Event solve(Term rel, Map<Term, Time> trail);



    static String timeStr(long when) {
        return when != ETERNAL ? Long.toString(when) : "ETE";
    }




}
