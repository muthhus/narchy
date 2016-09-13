package nars.concept;

import nars.IO;
import nars.NAR;
import nars.Task;
import nars.bag.Bag;
import nars.budget.Activation;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptPolicy;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.term.Compound;
import nars.term.Term;
import nars.term.proxy.ProxyCompound;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/** aliases one concept with another.
 *
 *  in abbreviation/compression cases the target will likely be of higher
 *  volume than the alias which need only be a term or a product-wrapped singleton compound.
 */
public class ProxyCompoundConcept implements Concept, ProxyCompound<Compound> {

    @NotNull
    private final CompoundConcept<?> target;

    @NotNull
    private final Compound alias;

    public ProxyCompoundConcept(@NotNull Compound alias, @NotNull CompoundConcept target, @NotNull NAR n) {
        this.alias = alias;

        //this.target = (CompoundConcept) n.index.remove(target);
        this.target = target;

        n.index.set(alias, this);
        n.index.set(target, this);

    }


    @Override
    public int hashCode() {
        return alias.hashCode();
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == this) return true;

        return alias.equals(obj);
//        Object ref;
//
//        if (obj instanceof ProxyCompoundConcept) {
//            ref = (((ProxyCompoundConcept)obj).target);
//        } else {
//            ref = obj;
//        }
//        return alias.equals(ref);
    }

    @Override
    public final ConceptPolicy policy() {
        return target.policy();
    }

//    @Override
//    public int compareTo(@NotNull Termlike o) {
//        if (equals(o)) return 0;
//        return alias.compareTo(o);
//    }

    @Override
    public void delete(NAR nar) {
        //?
    }

    @NotNull
    @Override
    public String toString() {
        return IO.Printer.stringify(target.term()).toString();
    }

    @Override
    public void append(@NotNull Appendable p) throws IOException {
        IO.Printer.append(target.term(), p);
    }

    @NotNull
    @Override
    public final Compound proxy() {
        return alias;
    }


    /**
     * Pending Quests to be answered by new desire values
     */
    @Nullable
    @Override
    public final QuestionTable quests() {
        return (target.quests());
    }

    @NotNull
    @Override
    public final QuestionTable questions() {
        return (target.questions());
    }

    @Override
    public @NotNull Bag<Task> tasklinks() {
        return target.tasklinks();
    }

    @NotNull
    @Override
    public Bag<Term> termlinks() {
        return target.termlinks();
    }

    @Override
    public @Nullable Map<Object, Object> meta() {
        return target.meta();
    }

    @Override
    public @Nullable Object put(@NotNull Object key, @Nullable Object value) {
        return target.put(key, value);
    }

    @NotNull
    @Override
    public Object meta(@NotNull Object key, @NotNull BiFunction value) {
        return target.meta(key,value);
    }


    /**
     * Judgments directly made about the term Use ArrayList because of access
     * and insertion in the middle
     */
    @NotNull
    @Override
    public BeliefTable beliefs() {
        return (target.beliefs());
    }

    /**
     * Desire values on the term, similar to the above one
     */
    @NotNull
    @Override
    public BeliefTable goals() {
        return (target.goals());
    }




    @Override
    public void policy(@NotNull ConceptPolicy c, long now, @NotNull List<Task> removed) {
        target.policy(c, now, removed);
    }


    @NotNull
    public String toStringActual() {
        return getClass().getSimpleName() + '(' + alias + " ===> " + target + ")";
    }

}