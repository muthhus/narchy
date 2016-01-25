package nars.nal.meta.op;

import com.google.common.collect.ListMultimap;
import com.gs.collections.api.map.ImmutableMap;
import nars.$;
import nars.Global;
import nars.nal.meta.*;
import nars.nal.meta.constraint.AndConstraint;
import nars.nal.meta.constraint.MatchConstraint;
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
public final class MatchTerm extends AtomicBooleanCondition<PremiseMatch> implements ProcTerm<PremiseMatch> {

    public final TaskBeliefPair x;
    @Nullable
    public final ImmutableMap<Term, MatchConstraint> constraints;

    @NotNull
    private final Compound id;

    /** derivation handlers; use the array form for fast iteration */
    private final Set<Derive> derive = Global.newHashSet(1);
    @Nullable
    private PremiseMatchFork onMatch = null;

    private MatchTerm(TaskBeliefPair x, @Nullable ImmutableMap<Term, MatchConstraint> constraints) {
        this.id = (constraints == null) ?
                //no constraints
                x :
                //constraints stored in atomic string
                (Compound) ($.sect(x, seteMap(constraints.castToMap(), $.ToStringToTerm)));

        this.x = x;
        this.constraints = constraints;
    }

    @NotNull
    public static MatchTerm get(TaskBeliefPair x, @Nullable ListMultimap<Term, MatchConstraint> c) {

        ImmutableMap<Term, MatchConstraint> constraints =
                ((c == null) || c.isEmpty()) ?
                        null :
                        immutable.ofAll(initConstraints(c));

        return new MatchTerm(x, constraints);
    }

    @NotNull
    public static Map<Term, MatchConstraint> initConstraints(@NotNull ListMultimap<Term, MatchConstraint> c) {
        Map<Term, MatchConstraint> con = Global.newHashMap();
        c.asMap().forEach((t, cc) -> {
            switch (cc.size()) {
                case 0:
                    return;
                case 1:
                    con.put(t, cc.iterator().next());
                    break;
                default:
                    con.put(t, new AndConstraint(cc));
                    break;
            }
        });
        return con;
    }


    @Override
    public final void accept(PremiseMatch p) {
        throw new RuntimeException("n/a");
    }

    @Override
    @Deprecated public final boolean booleanValueOf(@NotNull PremiseMatch p) {
        p.match(this, constraints);
        return true;
    }

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
        if (Global.DEBUG && derive.isEmpty())
            throw new RuntimeException("invalid MatchTerm with no derivation handlers:" + this);

        //TODO HACK dont lazily instantiate this but do it after the TrieDeriver has finished building the rule trie by iterating all known MatchTerm's (in the LinkGraph)
        if (onMatch == null) {
            onMatch = new PremiseMatchFork(derive.toArray(new Derive[derive.size()]));
        }

        onMatch.accept(m);
        return true;
    }
}
