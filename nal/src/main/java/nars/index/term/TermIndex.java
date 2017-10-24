package nars.index.term;

import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.builder.ConceptBuilder;
import nars.concept.builder.DefaultConceptBuilder;
import nars.concept.state.ConceptState;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.transform.CompoundTransform;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.Null;

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
    @Nullable
    public abstract Termed get(/*@NotNull*/ Term key, boolean createIfMissing);

    @Override
    public final Termed apply(Term term) {
        return get(term, false);
    }

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
        stream().forEach(out::println);
        out.println();
    }

    abstract public Stream<? extends Termed> stream();

    /**
     * default impl
     */
    public void forEach(Consumer<? super Termed> c) {
        stream().forEach(c);
    }

    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     * term should be conceptualizable prior to calling this
     */
    @Nullable
    public final Concept concept(Termed x, boolean createIfMissing) {

        Term xt;
        if (x instanceof Concept) {
            Concept ct = (Concept) x;
            if (!ct.isDeleted())
                return ct; //assumes an existing Concept index isnt a different copy than what is being passed as an argument
            //otherwise if it is deleted, continue
            xt = ct.term();
        } else {
            xt = x.term();
        }

        Term y = xt.conceptual();
        if (y == Null)
            return null;

        @Nullable Termed c = get(y, createIfMissing);
        if (!(c instanceof Concept)) {
//            if (createIfMissing) {
//                throw new Concept.InvalidConceptException(term, "Failed to builder concept");
//            }
            return null;
        }

        Concept cc = (Concept) c;
        ConceptState s = cc.state();

        if (s == ConceptState.Deleted) {
            //probably just a race condition fluke when this occurrs
            //and without looping again (and again) this should just give up

//            if (createIfMissing)
//                throw new RuntimeException("TermIndex impl should not return a deleted concept without trying to create a new one");
//            else
                return null;
        }

        if (s == ConceptState.New) {
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

    /** accesses the interning termcontext which recursively replaces subterms with what this index resolves them to */
    public final TermContext intern() {
        return intern;
    }

    private final TermContext intern = new InterningContext();

    private class InterningContext implements CompoundTransform {

        @Override
        public Termed apply(Term x) {
            if (!x.op().conceptualizable || x instanceof PermanentConcept)
                return x; //skip the nonsense
            else
                return TermIndex.this.applyIfPossible(x);
        }

        @Override
        public @Nullable Term transform(Compound x, Op op, int dt) {
            //TODO recurse
            return TermIndex.this.applyTermIfPossible(x);
        }

//        @Override
//        public Term applyTermIfPossible(Term x) {
//            return x.op().conceptualizable ? //skip the nonsense if it's constant
//                    CompoundTransform.super.applyTermIfPossible(x) : x;
//        }

        @Override
        public Term intern(Term x) {
//            if (x instanceof Compound)
//                return x.transform(this);
            if (x.op().conceptualizable) return TermIndex.this.applyTermIfPossible(x);
            else return x;
        }
    }
}
