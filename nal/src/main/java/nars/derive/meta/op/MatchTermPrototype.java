package nars.derive.meta.op;

import nars.$;
import nars.Op;
import nars.derive.meta.AtomicBoolCondition;
import nars.derive.meta.BoolCondition;
import nars.derive.meta.Conclude;
import nars.derive.meta.Fork;
import nars.derive.meta.constraint.MatchConstraint;
import nars.index.term.PatternTermIndex;
import nars.premise.Derivation;
import nars.term.Compound;
import nars.term.Term;
import org.eclipse.collections.api.map.ImmutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.$.seteMap;
import static nars.derive.meta.op.MatchTaskBelief.compile;

/**
 * Establishes conditions for the Term match
 *
 * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
 */
abstract public class MatchTermPrototype extends AtomicBoolCondition {

    @Nullable
    private BoolCondition eachMatch;

    @Nullable
    public final MatchConstraint constraints;


    @NotNull
    protected final Term pid;

    @NotNull
    protected Term id;

    public final Term pattern;

    /** derivation handlers; use the array form for fast iteration */
    //private final Set<Derive> derive = Global.newHashSet(1);
    private final Set<Conclude> conclude = $.newHashSet(1);




    public MatchTermPrototype(@NotNull Term id, Term pattern, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        this.id = this.pid = id;

        this.pattern = pattern;
        this.constraints = constraints!=null ? compile(constraints) : null;
    }


    @Nullable
    public static Term id(@NotNull PatternTermIndex index, @NotNull Term pattern, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        return (constraints == null) ?
                //no constraints
                pattern :
                //constraints stored in atomic string
                (Compound) (index.the(Op.SECTe, pattern, seteMap(constraints.castToMap(), $.ToStringToTerm)));
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
    public void derive(Conclude x) {
        conclude.add(x);
    }

    public final @NotNull BoolCondition build() {
        if (this.eachMatch == null) {


            BoolCondition om;

            switch (conclude.size()) {
                case 0:
                    om = null;
                    break;
                case 1:
                    om = conclude.iterator().next();
                    break;
                default:
                    om = Fork.compile(conclude.toArray(new Conclude[conclude.size()]));
                    break;
            }


            this.id = $.the("MatchTerm(" + pid +
                    ((om!=null) ? ",  " + om  : "") + ')');


            this.eachMatch = om;
        }

        return build(this.eachMatch);
    }

    @NotNull
    abstract protected BoolCondition build(BoolCondition eachMatch);

    @Override
    public boolean run(Derivation derivation, int now) {
        throw new UnsupportedOperationException("Use the instance that this builds, not this. it should not result in the deriver");
    }

}
