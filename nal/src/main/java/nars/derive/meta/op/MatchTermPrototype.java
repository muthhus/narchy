package nars.derive.meta.op;

import nars.$;
import nars.derive.meta.AtomicPredicate;
import nars.derive.meta.BoolPredicate;
import nars.derive.meta.Conclude;
import nars.derive.meta.Fork;
import nars.premise.Derivation;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Establishes conditions for the Term match
 *
 * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
 */
abstract public class MatchTermPrototype extends AtomicPredicate<Derivation> {

    @Nullable
    private BoolPredicate eachMatch;


    @NotNull
    protected final Term pid;

    @NotNull
    protected Term id;

    public final Term pattern;

    /** derivation handlers; use the array form for fast iteration */
    //private final Set<Derive> derive = Global.newHashSet(1);
    private final Set<Conclude> conclude = $.newHashSet(1);




    public MatchTermPrototype(@NotNull Term id, Term pattern) {
        this.id = this.pid = id;

        this.pattern = pattern;
    }


    @Nullable
    public static Term id(@NotNull Term pattern) {
        return pattern;
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

    public final @NotNull BoolPredicate build() {
        if (this.eachMatch == null) {


            BoolPredicate om;

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
    abstract protected BoolPredicate build(BoolPredicate eachMatch);

    @Override
    public boolean test(Derivation derivation) {
        throw new UnsupportedOperationException("Use the instance that this builds, not this. it should not result in the deriver");
    }

}
