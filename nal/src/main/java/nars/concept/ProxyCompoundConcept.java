package nars.concept;

import nars.NAR;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptBudgeting;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
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
public class ProxyCompoundConcept<T extends Term> implements Concept {
    private final CompoundConcept target;
    private final T alias;

    public ProxyCompoundConcept(T alias, CompoundConcept target) {
        this.alias = alias;
        this.target = target;
    }

    @Override
    public boolean contains(Task t) {
        return target.tableFor(t.punc()).get(t) != null;
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
        return null;
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

    @Override
    public int compareTo(Object o) {
        return alias.compareTo(o);
    }

    @NotNull
    @Override
    public Term term() {
        return alias;
    }

}