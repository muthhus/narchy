package nars.concept;

import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptBudgeting;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.task.Task;
import nars.term.*;
import nars.term.proxy.ProxyCompound;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.BiFunction;

/** aliases one concept with another.
 *
 *  in abbreviation/compression cases the target will likely be of higher
 *  volume than the alias which need only be a term or a product-wrapped singleton compound.
 */
public class ProxyCompoundConcept implements Concept, ProxyCompound<Compound<Term>> {

    private final CompoundConcept target;

    private final Compound alias;

    public ProxyCompoundConcept(Compound alias, CompoundConcept target, NAR n) {
        this.alias = alias;

        this.target = (CompoundConcept) n.index.remove(target);
        n.index.set(target, this);
        n.index.set(alias, this);
    }

    @Override
    public int hashCode() {
        return alias.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || alias.equals(obj);
    }

    @Override
    public String toString() {
        return target.toString();
    }

    @Override
    public final Compound target() {
        return alias;
    }

    @NotNull
    @Override
    public Compound term() {
        return this;
    }

    @Override
    public boolean contains(Task t) {
        return target.contains(t);
    }

    @Override
    public void linkTask(@NotNull Task t, float scale) {
        target.linkTask(t, scale);
    }

    /**
     * Pending Quests to be answered by new desire values
     */
    @Nullable
    @Override
    public final QuestionTable quests() {
        return (target.quests());
    }

    @Override
    public @Nullable Task process(@NotNull Task task, @NotNull NAR nar) {
        return target.process(task, nar);
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

    @Override
    public @NotNull Bag<Termed> termlinks() {
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
    public <C> C meta(Object key, BiFunction value) {
        return target.meta(key, value);
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
    public boolean link(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {
        return target.link(b, scale, minScale, nar, conceptOverflow);
    }

    @Override
    public void capacity(ConceptBudgeting c) {
        target.capacity(c);
    }


    public String toStringActual() {
        return getClass().getSimpleName() + "(" + alias + " ===> " + target + ")";
    }

}