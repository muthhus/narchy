package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PLink;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.conceptualize.state.ConceptState;
import nars.index.term.TermIndex;
import nars.table.BeliefTable;
import nars.table.QuestionTable;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.atom.AtomicStringConstant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;


public class AtomConcept extends AtomicStringConstant implements Concept {

    private final Bag<Term,PLink<Term>> termLinks;
    private final Bag<Task,PLink<Task>> taskLinks;

    @Nullable private transient ConceptState state = ConceptState.Inactive;

    @NotNull
    private final Op op;

    @Nullable
    private Map meta;

    public AtomConcept(@NotNull Atomic atom, Bag<Term,PLink<Term>> termLinks, Bag<Task,PLink<Task>> taskLinks) {
        this(atom.toString(), atom.op(), termLinks, taskLinks);
    }

    protected AtomConcept(@NotNull String term, @NotNull Op op, Bag<Term,PLink<Term>> termLinks, Bag<Task,PLink<Task>> taskLinks) {
        super(term);

        this.op = op;

        this.termLinks = termLinks;
        this.taskLinks = taskLinks;

        this.state = ConceptState.Inactive;
    }

    @NotNull
    @Override
    public Op op() {
        return op;
    }

    @Override
    public final Term eval(TermIndex index) {
        //safe to return itself because it's probably what is being resolved anyway
        return this;
    }

    @Override
    public ConceptState state() {
        return state;
    }

    @Override
    public ConceptState state(@NotNull ConceptState p, NAR nar) {
        ConceptState current = this.state;
        if (current!=p) {
            this.state = p;
            linkCapacity(p.linkCap(this, true),p.linkCap(this, false));
        }
        return current;
    }


    @Override
    public @NotNull Bag<Task,PLink<Task>> tasklinks() {
        return taskLinks;
    }

    @NotNull
    @Override
    public Bag<Term,PLink<Term>> termlinks() {
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
    public BeliefTable beliefs() {
        return BeliefTable.EMPTY;
    }

    @NotNull
    @Override
    public BeliefTable goals() {
        return BeliefTable.EMPTY;
    }

    @Override
    @NotNull
    public QuestionTable questions() {
        return QuestionTable.EMPTY;
    }

    @NotNull
    @Override
    public QuestionTable quests() {
        return QuestionTable.EMPTY;
    }


    @Override
    public void delete(@NotNull NAR nar) {
        Concept.delete(this, nar);
        meta = null;
    }



}
