package nars.concept;

import nars.NAR;
import nars.Op;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.budget.policy.ConceptPolicy;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicStringConstant;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class AtomConcept extends AtomicStringConstant implements AbstractConcept {

    private final Bag<Term> termLinks;
    private final Bag<Task> taskLinks;
    private ConceptPolicy policy;

    @NotNull
    private final Op op;

    private Map meta;

    public AtomConcept(@NotNull Atomic atom, Bag<Term> termLinks, Bag<Task> taskLinks) {
        super(atom.toString());

        this.op = atom.op();

        this.termLinks = termLinks;
        this.taskLinks = taskLinks;
    }

    @NotNull
    @Override
    public Op op() {
        return op;
    }


    @Override
    public ConceptPolicy policy() {
        return policy;
    }
    @Override
    public void policy(@Nullable ConceptPolicy p, long now) {
        this.policy = p;
        if (p!=null) {
            linkCapacity(p);
        }
    }


    @Override
    public @NotNull Bag<Task> tasklinks() {
        return taskLinks;
    }

    @NotNull
    @Override
    public Bag<Term> termlinks() {
        return termLinks;
    }

    @Override
    public @Nullable Map<Object, Object> meta() {
        return meta;
    }

    @Override
    public void setMeta(@NotNull Map newMeta) {
        this.meta = newMeta;
    }

    @NotNull
    @Override
    public final BeliefTable beliefs() {
        return BeliefTable.EMPTY;
    }

    @NotNull
    @Override
    public final BeliefTable goals() {
        return BeliefTable.EMPTY;
    }

    @Override
    @NotNull
    public final @Nullable QuestionTable questions() {
        return QuestionTable.EMPTY;
    }

    @NotNull
    @Override
    public final QuestionTable quests() {
        return QuestionTable.EMPTY;
    }


    @Override
    public Task process(@NotNull Task task, @NotNull NAR nar) {
        throw new UnsupportedOperationException();
    }

//    @Override
//    public final boolean contains(Task t) {
//        return false;
//    }

    @Override
    public void linkAny(@NotNull Budgeted b, float scale, float minScale, @NotNull NAR nar, NAR.Activation activation) {
        //nothing
    }
}
