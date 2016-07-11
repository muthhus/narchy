package nars.concept;

import nars.NAR;
import nars.Op;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.Atom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class AtomConcept extends Atom implements AbstractConcept  {

    private final Bag<Term> termLinks;
    private final Bag<Task> taskLinks;

    @NotNull
    private final Op op;

    private Map meta;

    public AtomConcept(@NotNull Atom atom, Bag<Term> termLinks, Bag<Task> taskLinks) {
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

    @Nullable
    @Override
    public final BeliefTable beliefs() {
        return BeliefTable.EMPTY;
    }

    @Nullable
    @Override
    public final BeliefTable goals() {
        return BeliefTable.EMPTY;
    }

    @Override
    public final @Nullable QuestionTable questions() {
        return QuestionTable.EMPTY;
    }

    @Nullable
    @Override
    public final QuestionTable quests() {
        return QuestionTable.EMPTY;
    }


    @Nullable
    @Override
    public Task process(@NotNull Task task, @NotNull NAR nar) {
        throw new UnsupportedOperationException();
    }



    @Override
    public final boolean contains(Task t) {
        return false;
    }

    @Override
    public void linkAny(Budgeted b, float scale, float minScale, @NotNull NAR nar, @Nullable MutableFloat conceptOverflow) {
        //nothing
    }
}
