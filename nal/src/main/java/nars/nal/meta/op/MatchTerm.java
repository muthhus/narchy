package nars.nal.meta.op;

import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Global;
import nars.nal.meta.*;
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

    @NotNull
    private Term id;

    /** derivation handlers; use the array form for fast iteration */
    //private final Set<Derive> derive = Global.newHashSet(1);
    private final Set<Derive> derive = Global.newHashSet(1);
    public final Term x;

    private @Nullable ProcTerm onMatch;

    public MatchTerm(@NotNull Term id, Term pattern, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        this.id = id;

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

    /** delegates a partial or complete match to each of the known derivation handlers */
    public void onMatch(@NotNull PremiseEval m) {
//        if (Global.DEBUG && derive.isEmpty())
//            throw new RuntimeException("invalid MatchTerm with no derivation handlers:" + this);

        //TODO HACK dont lazily instantiate this but do it after the TrieDeriver has finished building the rule trie by iterating all known MatchTerm's (in the LinkGraph)
        ProcTerm o = this.onMatch;


        o.accept(m);
    }

    public final @NotNull ProcTerm build() {
        if (this.onMatch == null) {
            this.id = $.the("MatchTerm(" + id + ",{" + derive + "})");


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

            this.onMatch = om;
        }
        return this.onMatch;
    }

//    @Override
//    public final void accept(PremiseEval p) {
//        throw new RuntimeException("n/a");
//    }

}
