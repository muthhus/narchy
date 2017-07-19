package nars.derive.meta.op;

import nars.control.premise.Derivation;
import nars.derive.meta.AbstractPred;
import nars.derive.meta.Conclude;
import nars.derive.meta.Fork;
import nars.derive.meta.PrediTerm;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.TreeSet;
import java.util.function.Function;

/**
 * Establishes conditions for the Term unification
 *
 * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
 */
abstract public class UnificationPrototype extends AbstractPred<Derivation> {

    @Nullable
    private PrediTerm eachMatch;

    @NotNull
    protected Compound id;

    @NotNull
    protected final Compound pid;

    public final Term pattern;

    /** derivation handlers; use the array form for fast iteration */
    public final TreeSet<Conclude> conclude = new TreeSet();

    public UnificationPrototype(@NotNull Compound id, Term pattern) {
        super(id);
        this.id = this.pid = id;

        this.pattern = pattern;
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

    public final @NotNull PrediTerm build(Function<PrediTerm<Derivation>, PrediTerm<Derivation>> each) {


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
                    om = Fork.fork(
                        conclude.toArray(new Conclude[cs])
                    );
                    break;
            }



            return build( om!=null ? each.apply(om) : null );
//
//                    $.func("unify", pid,
//                            this.eachMatch = each.apply(om)  ) : //final part of match
//
//                    $.func( "unify", pid)
//            ); //first part of match

    }


    @NotNull
    abstract protected PrediTerm build(PrediTerm eachMatch);

    @Override
    public boolean test(Derivation derivation) {
        throw new UnsupportedOperationException("Use the instance that this builds, not this. it should not result in the deriver");
    }

}
