package nars.index.term;

import nars.Builtin;
import nars.NAR;
import nars.Narsese;
import nars.Op;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.conceptualize.ConceptBuilder;
import nars.index.TermBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.subst.MapSubst;
import nars.term.subst.MapSubst1;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.NEG;
import static nars.time.Tense.DTERNAL;
import static nars.time.Tense.XTERNAL;

/**
 *
 */
public abstract class TermIndex extends TermBuilder implements TermContext {


    private static final Logger logger = LoggerFactory.getLogger(TermIndex.class);
    public NAR nar;
    protected ConceptBuilder conceptBuilder;


    /**
     * internal get procedure
     */
    @Nullable
    public abstract Termed get(@NotNull Term key, boolean createIfMissing);

//    @Override
//    protected int dt(int dt) {
//        NAR n = this.nar;
//        if (n == null)
//            return dt;
//
//        switch (dt) {
//            case DTERNAL:
//            case XTERNAL:
//            case 0:
//                return dt; //no-change
//        }
//
//        return Math.abs(dt) < n.dur() ? 0 : dt;
//    }

    /**
     * sets or replaces the existing value, unless the existing value is a PermanentConcept it must not
     * be replaced with a non-Permanent concept
     */
    public abstract void set(@NotNull Term src, Termed target);

    public final void set(@NotNull Termed t) {
        set(t instanceof Term ? (Term) t : t.term(), t);
    }


    abstract public void clear();

    abstract public void forEach(Consumer<? super Termed> c);


    /**
     * called when a concept has been modified, ie. to trigger persistence
     */
    public void commit(Concept c) {
        //by default does nothing
    }

    public void start(NAR nar) {

        this.nar = nar;
        this.conceptBuilder = nar.conceptBuilder;

        for (Concept t : Builtin.statik)
            set(t);

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


//    public final HijacKache<Compound, Term> normalizations =
//            new HijacKache<>(Param.NORMALIZATION_CACHE_SIZE, 4);
//    public final HijacKache<ProtoCompound, Term> terms =
//            new HijacKache<>(Param.TERM_CACHE_SIZE, 4);

//    final Function<? super ProtoCompound, ? extends Term> termizer = pc -> {
//
//        return theSafe(pc.op(), pc.dt(), pc.terms() );
//    };
//
//    private int volumeMax(Op op) {
//        if (nar!=null) {
//            return nar.termVolumeMax.intValue();
//        } else {
//            return Param.COMPOUND_VOLUME_MAX;
//        }
//    }

//    @NotNull
//    private final Term theSafe(@NotNull Op o, int dt, @NotNull Term[] u) {
//        try {
//            return super.the(o, dt, u);
//            //return t == null ? False : t;
//        } catch (@NotNull InvalidTermException | InvalidTaskException x) {
//            if (Param.DEBUG_EXTRA) {
//                logger.warn("{x} : {} {} {}", x, o, dt, u);
//            }
//        } catch (Throwable e) {
//            logger.error("{x} : {} {} {}", e, o, dt, u);
//        }
//        return False; //place a False placeholder so that a repeat call will not have to discover this manually
//    }

//    @NotNull
//    public final Term the(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
//        if (csrc.subterms().equals(newSubs)) {
//            return csrc;
//        } else {
//            return the(csrc.op(), csrc.dt(), newSubs.terms());
//        }
//    }

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


//    private boolean cacheNormalization(@NotNull Compound src) {
//        return false;
//    }


//    @Nullable
//    public Term the(@NotNull Compound src, @NotNull List<Term> newSubs) {
//        if (src.size() == newSubs.size() && src.equalTerms(newSubs) )
//            return src;
//        else
//            return the(src.op(), src.dt(), newSubs.toArray(new Term[newSubs.size()]));
//    }

    @Nullable
    public Compound normalize(@NotNull Compound x) {

        if (x.isNormalized())
            return x; //TODO try not to let this happen

        Term y;

        int vars = x.vars();
        int pVars = x.varPattern();
        int totalVars = vars + pVars;

        Compound result;
        if (totalVars > 0) {
            y = transform(x,
                    ((vars == 1) && (pVars == 0)) ?
                            VariableNormalization.singleVariableNormalization //special case for efficiency
                            :
                            new VariableNormalization(totalVars /* estimate */)
            );

            if (y instanceof Compound) {
                if (y != x) {
                    Compound cy = (Compound) y;
                    result = cy;
                } else {
                    result = x;
                }
            } else {
                result = null;
            }

        } else {
            result = x;
        }

        if (result != null)
            result.setNormalized();

        return result;
    }


    @Nullable
    public Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        if (!t.testSuperTerm(src)) {
            return src;
        } else {
            return transform(src.op(), src.dt(), src, t);
        }
    }

    @Nullable
    public Term transform(@NotNull Compound src, int newDT, @NotNull CompoundTransform t) {
        if (src.dt() == newDT)
            return transform(src, t); //no dt change, use non-DT changing method that has early fail
        else {
            return transform(src.op(), newDT, src, t);
        }
    }

    @Nullable
    private Term transform(Op op, int dt, @NotNull Compound src, @NotNull CompoundTransform t) {

        //int modifications = 0;

        if (!t.testSuperTerm(src))
            return src;

        boolean filterTrueFalse = disallowTrueOrFalse(op);

        int s = src.size(), subtermMods = 0;
        AppendProtoCompound target = new AppendProtoCompound(op, s);
        for (int i = 0; i < s; i++) {

            Term x = src.sub(i), y;

            y = t.apply(src, x);

            if (y == null)
                return null;

            if (y instanceof Compound) {
                y = transform((Compound) y, t); //recurse
            }

            if (y == null)
                return null;

            if (y != x) {
                if (Term.filterBool(y, filterTrueFalse))
                    return null;

                //            if (y != null)
                //                y = y.eval(this);

                //if (x != y) { //must be refernce equality test for some variable normalization cases
                //if (!x.equals(y)) { //must be refernce equality test for some variable normalization cases
                subtermMods++;

            }

            target.add(y);
        }

        //TODO does it need to recreate the container if the dt has changed because it may need to be commuted ... && (superterm.dt()==dt) but more specific for the case: (XTERNAL -> 0 or DTERNAL)

        //        if (subtermMods == 0 && !opMod && dtMod && (op.image || (op.temporal && concurrent(dt)==concurrent(src.dt()))) ) {
//            //same concurrency, just change dt, keep subterms
//            return src.dt(dt);
//        }
        if (subtermMods > 0 || op != src.op()/* || dt != src.dt()*/)
            return the(op, dt, target);
        else if (dt != src.dt())
            return src.dt(dt);
        else
            return src;
    }

    public static boolean disallowTrueOrFalse(Op superOp) {

        switch (superOp) {
            case EQUI:
            case IMPL:
            case CONJ:
                return false; //allow for these because reductions may apply if they are present
            default:
                return true;
        }
    }


    @Nullable
    public Term transform(@NotNull Compound src, @NotNull ByteList path, @NotNull Term replacement) {
        return transform(src, path, 0, replacement);
    }

    @Nullable
    private Term transform(@NotNull Term src, @NotNull ByteList path, int depth, @NotNull Term replacement) {
        int ps = path.size();
        if (ps == depth)
            return replacement;
        if (ps < depth)
            throw new RuntimeException("path overflow");

        if (!(src instanceof Compound))
            return src; //path wont continue inside an atom

        int n = src.size();
        Compound csrc = (Compound) src;

        Term[] target = new Term[n];


        boolean changed = false;
        for (int i = 0; i < n; ) {
            Term x = csrc.sub(i);
            Term y;
            if (path.get(depth) != i) {
                //unchanged subtree
                y = x;
            } else {
                //replacement is in this subtree
                y = transform(x, path, depth + 1, replacement);
                changed = true;
            }

            target[i++] = y;
        }

        if (!changed)
            return csrc;

        return csrc.op().the(csrc.dt(), target);
    }


    /** un-normalized */
    @NotNull public <T extends Term> T termRaw(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) Narsese.term(termToParse, this, false);
    }

    /** normalized */
    @NotNull public <T extends Term> T term(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) (Narsese.term(termToParse, this, true));
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
//                throw new Concept.InvalidConceptException(term, "Failed to build concept");
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


    @Nullable
    public final Term replace(@NotNull Term src, @NotNull Map<Term, Term> m) {
        return new MapSubst(m).transform(src);
    }

    @Nullable
    public final Term replace(@NotNull Term src, @NotNull Term from, @NotNull Term to) {
        return new MapSubst1(from, to).transform(src);
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



    @Nullable
    public Compound retemporalize(@NotNull Compound x, Retemporalization r) {

        Term y = transform(x, r.dt(x), r);
        if (!(y instanceof Compound)) {
            return null;
        } else {
            Compound yy = (Compound)y;
//            int ydt = yy.dt();
//            if (ydt ==XTERNAL|| ydt ==DTERNAL) {
//                int zdt = r.dt(x);
//                if (ydt!=zdt)
//                    //yy = compoundOrNull(transform(yy, zdt, CompoundTransform.Identity));
//                    yy = compoundOrNull(yy.op().the(zdt, yy.toArray()));
//            }
//            if (yy == null)
//                return null;
            return normalize(yy);
        }

    }

    @Nullable
    public Term queryToDepVar(@NotNull Compound term) {
        return transform(term, CompoundTransform.queryToDepVar);
    }


    public final Retemporalization retemporalizationDTERNAL = new Retemporalization(DTERNAL);
    public final Retemporalization retemporalizationZero = new Retemporalization(0);


    public class Retemporalization implements CompoundTransform {

        final int dtIfXternal;

        private Retemporalization(int dtIfXternal) {
            this.dtIfXternal = dtIfXternal;
        }

        @Override
        public boolean testSuperTerm(@NotNull Compound c) {
            return (c.hasAny(Op.TemporalBits));
        }

        @Nullable
        @Override
        public Term apply(@Nullable Compound parent, @NotNull Term term) {
            if (term instanceof Compound && term.hasAny(Op.TemporalBits)) {
                Compound x = (Compound) term;
                return transform(x, dt(x), this);
            }
            return term;
        }

        public int dt(@NotNull Compound x) {
            int dt = x.dt();
            return (dt==DTERNAL||dt==XTERNAL) ? dtIfXternal : dt;
        }
    }
}
