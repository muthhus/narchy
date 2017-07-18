package nars.derive.meta.op;

import com.google.common.collect.Lists;
import nars.$;
import nars.control.premise.Derivation;
import nars.derive.meta.*;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Establishes conditions for the Term match
 *
 * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
 */
abstract public class MatchTermPrototype extends AbstractPred<Derivation> {

    @Nullable
    private BoolPred eachMatch;

    @NotNull
    protected Compound id;

    @NotNull
    protected final Compound pid;

    public final Term pattern;

    /** derivation handlers; use the array form for fast iteration */
    private final Set<Conclude> conclude = $.newHashSet(1);

    public MatchTermPrototype(@NotNull Compound id, Term pattern) {
        super(id);
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



    /** add a derivation handler to be applied after a rule match */
    public void derive(Conclude x) {
        conclude.add(x);
    }

    public final @NotNull BoolPred build() {
        if (this.eachMatch == null) {


            BoolPred om;

            switch (conclude.size()) {
                case 0:
                    om = null;
                    break;
                case 1:
                    om = cause(conclude.iterator().next());
                    break;
                default:
                    om = Fork.compile(
                        conclude.stream().map(this::cause).toArray(BoolPred[]::new)
                        //conclude.toArray(new Conclude[conclude.size()])
                    );
                    break;
            }


            this.ref = this.id = om!=null ? $.func("MatchTerm", pid, om) :
                                 $.func( "MatchTerm", pid);

            this.eachMatch = om;
        }

        return build(this.eachMatch);
    }

    private BoolPred cause(Conclude c) {
        return AndCondition.the( Lists.newArrayList(
            new Caused(),
            c
        ) );
    }

    @NotNull
    abstract protected BoolPred build(BoolPred eachMatch);

    @Override
    public boolean test(Derivation derivation) {
        throw new UnsupportedOperationException("Use the instance that this builds, not this. it should not result in the deriver");
    }

}
