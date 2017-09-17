package nars.index.term;

import nars.NAR;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.builder.ConceptBuilder;
import nars.concept.builder.DefaultConceptBuilder;
import nars.concept.state.ConceptState;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 */
public abstract class TermIndex implements TermContext {


    private static final Logger logger = LoggerFactory.getLogger(TermIndex.class);
    public final ConceptBuilder conceptBuilder = new DefaultConceptBuilder();
    public NAR nar;

    /**
     * internal get procedure
     */
    @Override
    @Nullable
    public abstract Termed get(@NotNull Term key, boolean createIfMissing);

    /**
     * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
     * be replaced with a non-Permanent concept
     */
    public abstract void set(@NotNull Term src, Termed target);

    public final void set(@NotNull Termed t) {
        set(t.term(), t);
    }

    abstract public void clear();

//    /**
//     * called when a concept has been modified, ie. to trigger persistence
//     */
//    public void commit(Concept c) {
//        //by default does nothing
//    }

    public void start(NAR nar) {
        this.nar = nar;

        conceptBuilder.start(nar);

    }

    /**
     * # of contained terms
     */
    public abstract int size();

    /**
     * a string containing statistics of the index's current state
     */
    @NotNull
    public abstract String summary();

    public abstract void remove(@NotNull Term entry);

    public void print(@NotNull PrintStream out) {
        forEach(out::println);
        out.println();
    }

    abstract public Stream<Termed> stream();

    /** default impl */
    public void forEach(@NotNull Consumer<? super Termed> c) {
        stream().forEach(c);
    }

    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     * term should be conceptualizable prior to calling this
     */
    @Nullable
    public final Concept concept(@NotNull Term term, boolean createIfMissing) {

        assert(term.op().conceptualizable); //term = term.unneg();

        @Nullable Termed c = get(term, createIfMissing);
        if (!(c instanceof Concept)) {
//            if (createIfMissing) {
//                throw new Concept.InvalidConceptException(term, "Failed to builder concept");
//            }
            return null;
        }

        Concept cc = (Concept) c;

        @NotNull ConceptState s = cc.state();

        if (s == ConceptState.Deleted) {
            if (createIfMissing)
                throw new RuntimeException("TermIndex impl should not return a deleted concept without trying to create a new one");
            else
                return null;
        }

        if (s == ConceptState.New) {
            cc.activate(0, nar);
            cc.state(conceptBuilder.init());
        }

        return cc;
    }

    protected final void onRemove(Termed value) {
        if (value instanceof Concept) {
            if (value instanceof PermanentConcept) {
                //refuse deletion
                set(value);
            } else {

                Concept c = (Concept) value;
                onBeforeRemove(c);
                c.delete(nar);
            }
        }
    }

    protected void onBeforeRemove(Concept c) {

    }

}
