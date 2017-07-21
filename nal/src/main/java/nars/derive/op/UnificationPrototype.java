package nars.derive.op;

import nars.control.premise.Derivation;
import nars.derive.AbstractPred;
import nars.derive.Conclusion;
import nars.derive.Fork;
import nars.derive.PrediTerm;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TreeSet;
import java.util.function.Function;

/**
 * Establishes conditions for the Term unification
 * <p>
 * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
 */
abstract public class UnificationPrototype extends AbstractPred<Derivation> {


    @NotNull
    protected Compound id;

    @NotNull
    protected final Compound pid;

    public final Term pattern;

    /**
     * derivation handlers; use the array form for fast iteration
     */
    public final TreeSet<Conclusion> conclude = new TreeSet();

    public UnificationPrototype(@NotNull Compound id, Term pattern) {
        super(id);
        this.id = this.pid = id;

        this.pattern = pattern;
    }

    abstract protected PrediTerm build(@Nullable PrediTerm eachMatch);

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




    public final @Nullable PrediTerm<Derivation> buildEachMatch() {

        PrediTerm om;

        int cs = conclude.size();
        switch (cs) {
            case 0:
                om = null;
                break;
            case 1:
                om = conclude.first();
                break;
            default:
//                om = Fork.fork(
//                    conclude.toArray(new Conclusion[cs])
//                );
                om = new Fork(conclude.toArray(new Conclusion[cs])) {
                    @Override
                    public boolean test(@NotNull Derivation m) {
                        return super.test(m);
                    }
                };
                break;
        }


        return om;
//
//                    $.func("unify", pid,
//                            this.eachMatch = each.apply(om)  ) : //final part of match
//
//                    $.func( "unify", pid)
//            ); //first part of match

    }



    @Override
    public boolean test(Derivation derivation) {
        throw new UnsupportedOperationException("Use the instance that this builds, not this. it should not result in the deriver");
    }

}
