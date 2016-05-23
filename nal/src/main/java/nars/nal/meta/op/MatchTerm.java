package nars.nal.meta.op;

import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Global;
import nars.nal.meta.AtomicBoolCondition;
import nars.nal.meta.Fork;
import nars.nal.meta.ProcTerm;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.op.Derive;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.$.seteMap;
import static nars.nal.meta.op.MatchTaskBelief.compile;

/**
 * Establishes conditions for the Term match
 *
 * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
 */
abstract public class MatchTerm extends AtomicBoolCondition {

    @Nullable
    public final MatchConstraint constraints;


    private final Term pid;

    @NotNull
    private Term id;

    /** derivation handlers; use the array form for fast iteration */
    //private final Set<Derive> derive = Global.newHashSet(1);
    private final Set<Derive> derive = Global.newHashSet(1);
    public final Term x;

    @Nullable
    public ProcTerm eachMatch;

    public MatchTerm(@NotNull Term id, Term pattern, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        this.id = this.pid = id;

        this.x = pattern;
        this.constraints = constraints!=null ? compile(constraints) : null;
    }


    @Nullable
    public static Term id(@NotNull Term pattern, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        return (constraints == null) ?
                //no constraints
                pattern :
                //constraints stored in atomic string
                (Compound) ($.esect(pattern, seteMap(constraints.castToMap(), $.ToStringToTerm)));
    }


//    public static final class MatchTaskBeliefPair extends MatchTerm {
//
//        public MatchTaskBeliefPair(@NotNull TaskBeliefPair x, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
//            super(id(x, constraints), x, constraints);
//        }
//
//        @Override
//        @Deprecated public final boolean booleanValueOf(@NotNull PremiseEval p) {
//            p.matchAll(x, p.term.get() /* current term */, this, constraints);
//            return true;
//        }
//    }


    @NotNull
    @Override
    public final String toString() {
        return id.toString();
    }

    /** add a derivation handler to be applied after a rule match */
    public void derive(Derive x) {
        derive.add(x);
    }

    public final @NotNull ProcTerm build() {
        if (this.eachMatch == null) {


            ProcTerm om;

            switch (derive.size()) {
                case 0:
                    om = null;
                    break;
                case 1:
                    om = derive.iterator().next();
                    break;
                default:
                    om = Fork.compile(derive.toArray(new Derive[derive.size()]));
                    break;
            }

            this.id = $.the("MatchTerm(" + pid +
                    ((om!=null) ? ",  " + om  : "") + ")");


            this.eachMatch = om;
        }
        return this.eachMatch;
    }

//    @Override
//    public final void accept(PremiseEval p) {
//        throw new RuntimeException("n/a");
//    }

}
