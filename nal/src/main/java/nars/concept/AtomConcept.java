package nars.concept;

import nars.Op;
import nars.Task;
import nars.bag.Bag;
import nars.budget.policy.ConceptPolicy;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicStringConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;


public class AtomConcept extends AtomicStringConstant implements AbstractConcept {

    private final Bag<Term> termLinks;
    private final Bag<Task> taskLinks;
    @Nullable
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
    public void policy(@NotNull  ConceptPolicy p, long now, List<Task> removed) {
        ConceptPolicy current = this.policy;
        if (current!=p) {
            this.policy = p;
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
    public final QuestionTable questions() {
        return QuestionTable.EMPTY;
    }

    @NotNull
    @Override
    public final QuestionTable quests() {
        return QuestionTable.EMPTY;
    }





}
