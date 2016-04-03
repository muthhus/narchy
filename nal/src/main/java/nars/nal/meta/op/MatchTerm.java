package nars.nal.meta.op;

import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Global;
import nars.Op;
import nars.nal.meta.AtomicBooleanCondition;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.PremiseFork;
import nars.nal.meta.ProcTerm;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.op.Derive;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

import static nars.$.seteMap;

/**
 * Establishes conditions for the Term match
 *
 * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
 */
abstract public class MatchTerm extends AtomicBooleanCondition<PremiseEval>  {

    @Nullable
    public final ImmutableMap<Term, MatchConstraint> constraints;

    @NotNull
    private final Term id;

    /** derivation handlers; use the array form for fast iteration */
    private final Set<Derive> derive = Global.newHashSet(1);
    public final Term x;

    private @Nullable ProcTerm onMatch;

    public MatchTerm(@NotNull Term id, Term pattern, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        this.id = id;

        this.x = pattern;
        this.constraints = constraints;
    }


    @Nullable static private Term id(@NotNull Term pattern, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        return (constraints == null) ?
                //no constraints
                pattern :
                //constraints stored in atomic string
                (Compound) ($.sect(pattern, seteMap(constraints.castToMap(), $.ToStringToTerm)));
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

    public static final class MatchOneSubterm extends MatchTerm {

        /** either 0 (task) or 1 (belief) */
        private final int subterm;

        @Nullable
        private final MatchOneSubterm callback;

        public MatchOneSubterm(@NotNull Term x, @Nullable ImmutableMap<Term, MatchConstraint> constraints, int subterm, boolean finish) {
            super(
                (subterm == 0 ?
                        $.p(id(x,constraints), Op.Imdex) :
                        $.p(Op.Imdex, id(x,constraints))),
                x
                , constraints);
            this.subterm = subterm;
            this.callback = finish ? this : null;
        }

        @Override
        @Deprecated public final boolean booleanValueOf(@NotNull PremiseEval p) {
            p.matchAll(x, p.term.term(subterm) /* current term */, callback, constraints);
            return true;
        }
    }


    @NotNull
    @Override
    public final String toString() {
        return id.toString();
    }

    /** add a derivation handler to be applied after a rule match */
    public void derive(Derive x) {
        derive.add(x);
    }

    /** delegates a partial or complete match to each of the known derivation handlers */
    public boolean onMatch(@NotNull PremiseEval m) {
//        if (Global.DEBUG && derive.isEmpty())
//            throw new RuntimeException("invalid MatchTerm with no derivation handlers:" + this);

        //TODO HACK dont lazily instantiate this but do it after the TrieDeriver has finished building the rule trie by iterating all known MatchTerm's (in the LinkGraph)
        ProcTerm o = this.onMatch;
        if (o == null) {
            o = this.onMatch = init();
        }

        o.accept(m);
        return true;
    }

    private final @NotNull ProcTerm init() {
        switch (derive.size()) {
            case 0: throw new RuntimeException("empty result procedure");
            case 1: return derive.iterator().next();
            default:
                return new PremiseFork(derive.toArray(new Derive[derive.size()]));
        }
    }

//    @Override
//    public final void accept(PremiseEval p) {
//        throw new RuntimeException("n/a");
//    }

}
