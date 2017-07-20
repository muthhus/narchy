package nars.concept;

import jcog.bag.Bag;
import jcog.pri.PriReference;
import nars.Task;
import nars.conceptualize.state.ConceptState;
import nars.index.term.TermContext;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.container.TermContainer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static nars.conceptualize.state.ConceptState.Deleted;


public class AtomConcept extends Atom implements Concept {

    private final Bag<Term,PriReference<Term>> termLinks;
    private final Bag<Task,PriReference<Task>> taskLinks;

    @NotNull private transient ConceptState state = Deleted;

    @Nullable
    private Map meta;

    public AtomConcept(@NotNull Atom term, Bag... bags) {
        this(term.toString(), bags[0], bags[1]);
    }

    protected AtomConcept(@NotNull String term,  Bag<Term,PriReference<Term>> termLinks, Bag<Task,PriReference<Task>> taskLinks) {
        super(term);

        this.termLinks = termLinks;
        this.taskLinks = taskLinks;

        this.state = Deleted;
    }

    @Override
    public TermContainer templates() {
        return null;
    }

    @Override
    public final Term eval(TermContext index) {
        //safe to return itself because it's probably what is being resolved anyway
        return this;
    }

    @Override
    public ConceptState state() {
        return state;
    }

    @Override
    public ConceptState state(@NotNull ConceptState p) {
        ConceptState current = this.state;
        if (current!=p) {
            if ((this.state = p)!=Deleted) { //dont bother shrinking to zero capacity on delete. potentially supports fast immediate recovery
                termlinks().setCapacity(p.linkCap(this, true));
                tasklinks().setCapacity(p.linkCap(this, false));
            }
        }
        return p;
    }


    @Override
    public @NotNull Bag<Task,PriReference<Task>> tasklinks() {
        return taskLinks;
    }

    @NotNull
    @Override
    public Bag<Term,PriReference<Term>> termlinks() {
        return termLinks;
    }

    @Override
    public @Nullable Map meta() {
        return meta;
    }

    @Override
    public void setMeta(@NotNull Map newMeta) {
        this.meta = newMeta;
    }



//    @Override
//    public void delete(@NotNull NAR nar) {
//        Concept.delete(this, nar);
//        meta = null;
//    }

}
