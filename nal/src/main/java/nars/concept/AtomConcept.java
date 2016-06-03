package nars.concept;

import nars.NAR;
import nars.Op;
import nars.bag.Bag;
import nars.budget.Budgeted;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.task.Task;
import nars.term.Termed;
import nars.term.Termlike;
import nars.term.atom.Atom;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicString;
import nars.term.atom.AtomicStringConstant;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * Created by me on 9/2/15.
 */
public class AtomConcept extends Atom implements AbstractConcept  {

    private final Bag<Termed> termLinks;
    private final Bag<Task> taskLinks;
    private final Op op;
    private Map meta;

    public AtomConcept(@NotNull Atom atom, Bag<Termed> termLinks, Bag<Task> taskLinks) {
        //super(atom, termLinks, taskLinks);
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

    /** the atom in this case is the concept itself, exposing this and not the internal 'term' field */
    @Override @NotNull public final AtomConcept term() {
        return this;
    }



    @Override
    public @NotNull Bag<Task> tasklinks() {
        return taskLinks;
    }

    @Override
    public @NotNull Bag<Termed> termlinks() {
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
