package nars.concept;

import nars.NAR;
import nars.Op;
import nars.bag.Bag;
import nars.concept.table.BeliefTable;
import nars.concept.table.QuestionTable;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 9/2/15.
 */
public class AtomConcept<E extends Atomic> extends AbstractConcept<E> implements Atomic {

    public AtomConcept(@NotNull E atom, Bag<Termed> termLinks, Bag<Task> taskLinks) {
        super(atom, termLinks, taskLinks);
    }

    /** the atom in this case is the concept itself, exposing this and not the internal 'term' field */
    @Override @NotNull public final AtomConcept term() {
        return this;
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
    public final @Nullable Op op() {
        return term.op();
    }

    @Override
    public int complexity() {
        return term.complexity();
    }


    @Override
    public int varIndep() {
        return 0;
    }

    @Override
    public int varDep() {
        return 0;
    }

    @Override
    public int varQuery() {
        return 0;
    }

    @Override
    public int varPattern() {
        return 0;
    }

    @Override
    public int vars() {
        return 0;
    }


    @Override
    public boolean contains(Task t) {
        return false;
    }
}
