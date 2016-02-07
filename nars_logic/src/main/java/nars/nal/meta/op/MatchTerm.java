package nars.nal.meta.op;

import com.google.common.collect.ListMultimap;
import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Global;
import nars.nal.meta.*;
import nars.nal.meta.constraint.AndConstraint;
import nars.nal.meta.constraint.MatchConstraint;
import nars.nal.op.Derive;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;

import static com.gs.collections.impl.factory.Maps.immutable;
import static nars.$.seteMap;

/**
 * Establishes conditions for the Term match
 *
 * < (|, match [, constraints]) ==> (&|, derivation1, ... derivationN)>
 */
abstract public class MatchTerm extends AtomicBooleanCondition<PremiseMatch> implements ProcTerm {

    @Nullable
    public final ImmutableMap<Term, MatchConstraint> constraints;

    @NotNull
    private final Term id;

    /** derivation handlers; use the array form for fast iteration */
    private final Set<Derive> derive = Global.newHashSet(1);
    public final Term x;

    @Nullable
    private PremiseMatchFork onMatch = null;

    public MatchTerm(Term pattern, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        this.id = (constraints == null) ?
                //no constraints
                pattern :
                //constraints stored in atomic string
                (Compound) ($.sect(pattern, seteMap(constraints.castToMap(), $.ToStringToTerm)));

        this.x = pattern;
        this.constraints = constraints;
    }



    public static final class MatchTaskBeliefPair extends MatchTerm {

        public MatchTaskBeliefPair(TaskBeliefPair x, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
            super(x, constraints);
        }

        /** the entire TaskBeliefPair tuple */
        @Override protected Term target(PremiseMatch p) {
            return p.term.get();
        }
    }

    public static final class MatchOneSubterm extends MatchTerm {

        /** either 0 (task) or 1 (belief) */
        private final int subterm;

        public MatchOneSubterm(Term x, @Nullable ImmutableMap<Term, MatchConstraint> constraints, int subterm) {
            super(x, constraints);
            this.subterm = subterm;
        }

        /** the entire TaskBeliefPair tuple */
        @Override protected Term target(PremiseMatch p) {
            return ((Compound)p.term.get()).term(subterm);
        }
    }


    @Override
    @Deprecated public final boolean booleanValueOf(@NotNull PremiseMatch p) {
        p.matchAll(x, target(p) /* current term */, this, constraints);
        return true;
    }

    /** returns the target term that will be compared against; */
    protected abstract Term target(PremiseMatch p);

    @Override
    public final String toString() {
        return id.toString();
    }

    /** add a derivation handler to be applied after a rule match */
    public void derive(Derive x) {
        derive.add(x);
    }

    /** delegates a partial or complete match to each of the known derivation handlers */
    public boolean onMatch(@NotNull PremiseMatch m) {
//        if (Global.DEBUG && derive.isEmpty())
//            throw new RuntimeException("invalid MatchTerm with no derivation handlers:" + this);

        //TODO HACK dont lazily instantiate this but do it after the TrieDeriver has finished building the rule trie by iterating all known MatchTerm's (in the LinkGraph)
        PremiseMatchFork o = this.onMatch;
        if (o == null) {
            o = this.onMatch = init();
        }

        o.accept(m);
        return true;
    }

    @NotNull
    private PremiseMatchFork init() {
        return new PremiseMatchFork(derive.toArray(new Derive[derive.size()]));
    }

    @Override
    public final void accept(PremiseMatch p) {
        throw new RuntimeException("n/a");
    }

}
