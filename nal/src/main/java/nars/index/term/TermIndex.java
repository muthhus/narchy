package nars.index.term;

import nars.Builtin;
import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.builder.ConceptBuilder;
import nars.concept.builder.DefaultConceptBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.transform.Retemporalize;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;

/**
 *
 */
public abstract class TermIndex implements TermContext {


    private static final Logger logger = LoggerFactory.getLogger(TermIndex.class);
    public NAR nar;

    public final ConceptBuilder conceptBuilder = new DefaultConceptBuilder();


    /**
     * internal get procedure
     */
    @Nullable
    public abstract Termed get(@NotNull Term key, boolean createIfMissing);


    /**
     * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
     * be replaced with a non-Permanent concept
     */
    public abstract void set(@NotNull Term src, Termed target);

    public final void set(@NotNull Termed t) {
        set(t instanceof Term ? (Term) t : t.term(), t);
    }


    abstract public void clear();




    /**
     * called when a concept has been modified, ie. to trigger persistence
     */
    public void commit(Concept c) {
        //by default does nothing
    }

    public void start(NAR nar) {
        this.nar = nar;

        conceptBuilder.start(nar);

        Builtin.load(nar);
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


    @NotNull
    public final Term the(@NotNull Compound csrc, @NotNull Term... args) {
        return csrc.equalTerms(args) ? csrc : csrc.op().the(csrc.dt(), args);
    }

    @NotNull
    public final Term the(@NotNull Compound csrc, int newDT) {
        return csrc.dt() == newDT ? csrc : csrc.op().the(newDT, csrc.toArray());
    }

//    @Override
//    public final @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] args) throws InvalidTermException {
//
////        int totalVolume = 0;
////        for (Term x : u)
////            totalVolume += x.volume();
//
////        if (totalVolume > volumeMax(op))
////            throw new InvalidTermException(op, dt, u, "Too voluminous");
//
//        boolean cacheable =
//                //(totalVolume > 2)
//                        //&&
//                (op !=INH) || !(args[0].op() == PROD && args[1].op()==ATOM && get(args[1]) instanceof Functor) //prevents caching for potential transforming terms
//                ;
//
//        if (cacheable) {
//
//            return terms.computeIfAbsent(new ProtoCompound.RawProtoCompound(op, dt, args), termizer);
//
//        } else {
//            return super.the(op, dt, args);
//        }
//    }

//    @Deprecated
//    public final @NotNull Term the(@NotNull Op op, @NotNull Term... tt) {
//        return the(op, DTERNAL, tt); //call this implementation's, not super class's
//    }


    public void print(@NotNull PrintStream out) {
        forEach(out::println);
        out.println();
    }


    abstract public Stream<Termed> stream();

    /** default impl */
    public void forEach(@NotNull Consumer<? super Termed> c) {
        stream().forEach(c);
    }

    /** un-normalized */
    @NotNull public <T extends Term> T termRaw(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) Narsese.term(termToParse, false);
    }

    /** normalized */
    @NotNull public <T extends Term> T term(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) (Narsese.term(termToParse, true));
    }


    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     * term should be conceptualizable prior to calling this
     */
    @Nullable
    public final Concept concept(@NotNull Term term, boolean createIfMissing) {

        assert (term.op() != NEG); //term = term.unneg();

        @Nullable Termed c = get(term, createIfMissing);
        if (!(c instanceof Concept)) {
//            if (createIfMissing) {
//                throw new Concept.InvalidConceptException(term, "Failed to builder concept");
//            }
            return null;
        }

        Concept cc = (Concept) c;
        if (cc.isDeleted()) {
            cc.state(conceptBuilder.init());
        }
        commit(cc);
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



    /* TODO move to Term instance method */
    @Nullable public static Term retemporalize(@NotNull Term x, Retemporalize r) {

//        if (x.size() == 0)
//            return x;

        Term y = x.transform(r.dt(x), r);
        if (y == null || y.size() == 0) {
            return y;
        } else {



            //            int ydt = yy.dt();
//            if (ydt ==XTERNAL|| ydt ==DTERNAL) {
//                int zdt = r.dt(x);
//                if (ydt!=zdt)
//                    //yy = compoundOrNull(transform(yy, zdt, CompoundTransform.Identity));
//                    yy = compoundOrNull(yy.op().the(zdt, yy.toArray()));
//            }
//            if (yy == null)
//                return null;
            return y.normalize();
        }

    }


    public static final Retemporalize retemporalizeAllToDTERNAL = new Retemporalize.RetemporalizeAll(DTERNAL);
    public static final Retemporalize retemporalizeXTERNALToDTERNAL = new Retemporalize.RetemporalizeNonXternal(DTERNAL);
    public static final Retemporalize retemporalizeXTERNALToZero = new Retemporalize.RetemporalizeNonXternal(0);




}
