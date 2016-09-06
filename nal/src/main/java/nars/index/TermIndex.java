package nars.index;

import nars.*;
import nars.concept.Concept;
import nars.nal.TermBuilder;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.op.TermTransform;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.atom.Operator;
import nars.term.compound.GenericCompound;
import nars.term.compound.ProtoCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.MapSubst;
import nars.term.subst.Subst;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.var.Variable;
import nars.util.data.map.nbhm.HijacKache;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.factory.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import static nars.Op.INH;
import static nars.Op.VAR_PATTERN;
import static nars.term.Term.False;
import static nars.term.Termed.termOrNull;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.DTERNAL;

/**
 *
 */
public abstract class TermIndex extends TermBuilder {


    public static final Logger logger = LoggerFactory.getLogger(TermIndex.class);

    /**
     * get if not absent
     */
    @Nullable
    public Termed get(@NotNull Termed t) {
        return get(t, false);
    }

    //use concept(key, craeteIfMissing) instead
    @Nullable
    public abstract Termed get(@NotNull Termed key, boolean createIfMissing);

    @NotNull public Compound conceptualize(@NotNull Compound x) {

        if (!x.isNormalized())
            throw new InvalidConceptException(x, "not normalized");

        Term xx = $.unneg(Terms.atemporalize(x)).term();

        if (xx.op().var)
            throw new InvalidConceptException(x, "variables can not be conceptualized");

        return (Compound)xx;
    }


    /**
     * set whether absent or not
     */
    public abstract void set(@NotNull Termed src, Termed target);

    public void set(@NotNull Termed t) {
        set(t, t);
    }


    public abstract void clear();

    abstract public void forEach(Consumer<? super Termed> c);


    public void start(NAR nar) { }

    /**
     * # of contained terms
     */
    public abstract int size();

    @Nullable abstract public Concept.ConceptBuilder conceptBuilder();



    public abstract int subtermsCount();

    public final HijacKache<TermContainer,TermContainer> normalizations =
            new HijacKache<>(Param.NORMALIZATION_CACHE_SIZE, 3 );
    public final HijacKache<ProtoCompound,Term> terms =
            new HijacKache<>(Param.TERM_CACHE_SIZE, 3 );

//    final ThreadLocal<Map<Compound,Compound>> normalizations =
//            ThreadLocal.withInitial( () ->
//                new CapacityLinkedHashMap(Param.NORMALIZATION_CACHE_SIZE_PER_THREAD)
//            );
//            //Collections.synchronizedMap( new CapacityLinkedHashMap(16*1024) );
    //final Cache<Compound,Compound> normalizations = Caffeine.newBuilder().maximumSize(Param.NORMALIZATION_CACHE_SIZE).build();


    final Function<? super ProtoCompound, ? extends Term> termizer = pc -> {

        try {
            return super.the(pc.op(), pc.dt(), pc.terms());
        } catch (InvalidTermException x) {
            if (Param.DEBUG_EXTRA)
                logger.info("the {} {}", pc, x);
            return False; //place a False placeholder so that a repeat call will not have to discover this manually
        } catch (Throwable e) {
            logger.error("the {} {}", pc ,e);
            return False;
        }
    };

    @NotNull public final Term cached(@NotNull Op op, int dt, @NotNull Term[] u) throws InvalidTermException {

        if (cacheable(op, u)) {

            ProtoCompound p = ProtoCompound.the(op, dt, u);



            Term t = terms.computeIfAbsent(p, termizer);

//            if (failure[0] != null) {
//                Throwable f = failure[0];
//                if (f instanceof InvalidTermException)
//                    throw ((InvalidTermException) f);
//                throw new RuntimeException(failure[0]);
//            }

//            //SANITY TEST:
//            @NotNull Term retry = super.the(p.op(), p.dt(), p.terms());
//            if (!t.equals(retry)) {
//                terms.computeIfAbsent(p, termizer);
//                throw new RuntimeException("cache fault");
//            }

            return t;

        } else {
            return super.the(op,dt,u);
        }
    }

    /**
     * returns the resolved term according to the substitution
     */
    @Nullable
    public Term resolve(@NotNull Term src, @NotNull Subst f) {


        Term y = f.xy(src);
        if (y != null)
            return y; //an assigned substitution, whether a variable or other type of term


        if (src instanceof Variable) {

            if (src.op() == VAR_PATTERN)
                return null; //unassigned pattern variable
            else
                return src; //unassigned but literal non-pattern var

        } else if (src instanceof Atomic) {
            return src;
        }

        //no variables that could be substituted, so return this constant
        if (f instanceof PremiseEval && src.vars() + src.varPattern() == 0) //shortcut for premise evaluation matching
            return src;

        int len = src.size();
        List<Term> sub = $.newArrayList(len /* estimate */);

        boolean strict = f instanceof PremiseEval;

        boolean changed = false;
        Compound crc = (Compound) src;
        Term[] cct = crc.terms();
        for (int i = 0; i < len; i++) {
            Term t = cct[i];
            Term u = resolve(t, f);


            if (u instanceof EllipsisMatch) {

                //                if (maxArity != -1 && m.size() + sub.size() > maxArity) {
//                    return src; //invalid transformation, violated arity constraint
//                }

                Collections.addAll(sub, ((EllipsisMatch) u).term);
                changed = true;

            } else {

                //                if (maxArity != -1 && 1 + sub.size() > maxArity) {
                //                    return src; //invalid transformation, violates arity constraint
                //                }

                if (u == null) {

                    if (strict)
                        return null;

                    sub.add(t); //keep value

                } else {
                    sub.add(u);
                    changed |= (u != t);
                }

            }
        }

        return changed ? the(crc, sub.toArray(new Term[sub.size()])) : crc;
    }

    @NotNull
    public final Term the(@NotNull Compound csrc, @NotNull TermContainer newSubs) {
        if (csrc.subterms().equals(newSubs)) {
            return csrc;
        } else {
            return the(csrc, newSubs.terms());
        }
    }

    @NotNull
    public final Term the(@NotNull Compound csrc, @NotNull Term[] args) {
        return the(csrc.op(), csrc.dt(), args);
    }

    @Override
    public final @NotNull Term the(@NotNull Op op, int dt, @NotNull Term[] args) throws InvalidTermException {
        return cached(op, dt, args);
    }

    @Deprecated @Override public final @NotNull Term the(@NotNull Op op, @NotNull Term... tt) {
        return the(op, DTERNAL, tt); //call this implementation's, not super class's
    }

    private static boolean cacheable(Op op, Term[] u) {
        if (u.length < 2) {
            return false; //probably not worth caching small compounds
        }
        if (op == INH) {
            if (u[1] instanceof TermTransform) //skip any immediate transforms as these must be dynamically computed
                return false;
        }
        return true;
    }

//    public Term immediates(@NotNull Subst f, Term result) {
//        Atomic op = Operator.operator(result);
//        if (op!=null) {
//            ImmediateTermTransform tf = f.getTransform(op);
//            if (tf != null) {
//                result = transform(f, (Compound) result, tf);
//            }
//        }
//        return result;
//    }


    @Nullable
    public Term transform(Subst f, @NotNull Compound result, TermTransform tf) {

        //Compound args = (Compound) Operator.opArgs((Compound) result).apply(f);
        Compound args = Operator.opArgs(result);

        return ((tf instanceof PremiseAware) && (f instanceof PremiseEval)) ?
                ((PremiseAware) tf).function(args, (PremiseEval) f) :
                tf.function(args);
    }


//    class ImmediateTermIndex implements TermIndex {
//
//        @Override
//        public Termed get(Object key) {
//            return (Termed)key;
//        }
//
//        @Override
//        public TermContainer getIfAbsentIntern(TermContainer s) {
//            return s;
//        }
//
//        @Override
//        public Termed internAtomic(Term t) {
//            return t;
//        }
//
//        @Override
//        public void forEach(Consumer<? super Termed> c) {
//
//        }
//
//        @Override
//        public Termed getTermIfPresent(Termed t) {
//            return t;
//        }
//
//        @Override
//        public Termed intern(Term tt) {
//            return tt;
//        }
//
//        @Override
//        public int subtermsCount() {
//            return 0;
//        }
//
//
//        @Override
//        public void clear() {
//
//        }
//
//
//        @Override
//        public void putTerm(Termed termed) {
//
//        }
//
//        @Override
//        public int size() {
//            return 0;
//        }
//
//
//    }

//    class GuavaIndex extends GuavaCacheBag<Term,Termed> implements TermIndex {
//
//
//        public GuavaIndex(CacheBuilder<Term, Termed> data) {
//            super(data);
//        }
//
//        @Override
//        public void forEachTerm(Consumer<Termed> c) {
//            data.asMap().forEach((k,v) -> {
//                c.accept(v);
//            });
//        }
//
//
//
//    }

//    /** public memory-based (Guava) cache */
//    @NotNull
//    static TermIndex memory(int capacity) {
////        CacheBuilder builder = CacheBuilder.newBuilder()
////            .maximumSize(capacity);
//        return new MapIndex2(
//            new HashMap(capacity*2),
//                //new UnifriedMap()
//                new publicConceptBuilder());
////        return new MapIndex2(
////                new HashMap(capacity)
////                //new UnifriedMap()
////        );
//    }
//    /** public memory-based (Guava) cache */
//    @NotNull
//    static TermIndex softMemory(int capacity) {
////        CacheBuilder builder = CacheBuilder.newBuilder()
////            .maximumSize(capacity);
//        return new MapIndex2(
//                new SoftValueHashMap(capacity*2),
//                //new WeakHashMap()
//                conceptBuilder);
////        return new MapIndex2(
////                new HashMap(capacity)
////                //new UnifriedMap()
////        );
//    }
////    static GuavaIndex memoryGuava(Clock c, int expirationCycles) {
////        return new GuavaIndex(c, expirationCycles);
//////        return new MapIndex(
//////
//////                new WeakValueHashMap(capacity),
//////                new WeakValueHashMap(capacity*2)
//////        );
////    }

    public void print(@NotNull PrintStream out) {
        forEach(out::println);
        out.println();
    }



    final Function<? super TermContainer, ? extends TermContainer> normalizer = u -> {
        //bmiss[0] = true;

        TermContainer result;

        try {
            int numVars = u.vars();

            @NotNull Term[] src = u.terms();

            Term[] tgt =
                transform(src, null,
                    (numVars == 1 && u.varPattern() == 0) ?
                            VariableNormalization.singleVariableNormalization :
                            new VariableNormalization(numVars /* estimate */)
                );

            result = tgt != src ? TermVector.the(tgt) : u;

        } catch (Throwable e) {

            if (Param.DEBUG_EXTRA)
                logger.warn("normalize {} : {}", u, e);

            result = InvalidSubterms;
        }

        return result;
    };

    @Nullable public final Compound normalize(@NotNull Compound t) {

//        if (t.op() == NEG)
//            throw new RuntimeException("should not be neg");

        if (t.isNormalized())
            return t;

        TermContainer src = t.subterms();
        TermContainer tgt = normalize(src);
        if (src == tgt) {
            //no change
            ((GenericCompound)t).setNormalized();
            return t;
        }

        if (tgt != InvalidSubterms) {
            Compound c = compoundOrNull(the(t, tgt));
            if (c!=null) {
                ((GenericCompound)c).setNormalized();

                return (Compound) $.unneg((Compound)c).term(); //some cases it may normalize to neg
            }
        }

        return null;


        //        if (v == null)
        //            throw new InvalidTermException(t.op(), t.dt(), new Term[]{}, "unnormalizable");
        //
        //
        ////        if (insert) {
        ////            Compound s = (Compound) termOrNull(get(r, false));
        ////            return s == null ? r : s; //if a concept does not exist, do not create one yet and just return the key
        ////        } else {
        ////        }
        //
        //
        //
        //
        ////Compound v = normalizations.get(t, normalizer); //caffeine
        //
        //        /*if (Math.random() < 0.01)
        //            System.err.println(normalizations.summary());*/
        //
        //        return v == InvalidSubterms ? null : v;
        //
        //    //return _normalize(t);
        //

    }

    @Nullable public final TermContainer normalize(@NotNull TermContainer t) {
        return normalizations.computeIfAbsent(t, normalizer);
    }


    @Nullable
    public Term the(@NotNull Compound src, @NotNull List<Term> newSubs) {
        return the(src, newSubs.toArray(new Term[newSubs.size()]));
    }


    @NotNull
    public Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        if (src == null || !t.testSuperTerm(src))
            return src;

        Term[] srcSubs = src.terms();
        Term[] tgtSubs = transform(srcSubs, src, t);

        return tgtSubs!=srcSubs ?
                the(src.op(), src.dt(), tgtSubs) : //must not allow subterms to be tested for equality, for variable normalization purpose the variables will seem equivalent but they are not
                src;

    }

    @NotNull
    public Term[] transform(@NotNull Term[] src, Compound superterm, @NotNull CompoundTransform t) {

        int modifications = 0;

        Term[] target = src.clone();

        for (int i = 0, n = src.length; i < n; i++) {

            Term x = src[i], y;

            if (t.test(x)) {
                y = t.apply(superterm, x);
            } else if (x instanceof Compound) {
                y = transform((Compound) x, t); //recurse
            } else {
                continue;
            }

            if (x != y) { //must be refernce equality test for some variable normalization cases
                modifications++;
                target[i] = y;
            }

        }

        return modifications > 0 ? target : src;
    }


    @Nullable
    public Term transform(@NotNull Compound src, @NotNull ByteList path, Term replacement) {
        return transform(src, path, 0, replacement);
    }

    @Nullable
    public Term transform(@NotNull Term src, @NotNull ByteList path, int depth, Term replacement) {
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

        for (int i = 0; i < n; i++) {
            Term x = csrc.term(i);
            if (path.get(depth) != i)
                //unchanged subtree
                target[i] = x;
            else {
                //replacement is in this subtree
                target[i] = transform(x, path, depth + 1, replacement);
            }

        }

        return the(csrc.op(), csrc.dt(), target);
    }


    @NotNull
    public Term parseRaw(@NotNull String termToParse) throws Narsese.NarseseException {
        return Narsese.the().term(termToParse, this, false);
    }

    @Nullable
    public <T extends Termed> T parse(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) /*the*/(Narsese.the().term(termToParse, this, true));
    }


//    /** for long-running processes, this uses
//     * a weak-value policy */
//    static TermIndex memoryAdaptive(int capacity) {
//        CacheBuilder builder = CacheBuilder.newBuilder()
//            .maximumSize(capacity)
//            .recordStats()
//            .weakValues();
//        return new GuavaIndex(builder);
//    }

    /**
     * applies normalization and anonymization to resolve the term of the concept the input term maps t
     */
    @Nullable
    public Concept concept(@NotNull Termed term, boolean createIfMissing) throws InvalidConceptException {

//        if (term instanceof Atomic) {
//
//            if (term instanceof Variable) {
//                //if (createIfMissing)
//                throw new InvalidConceptException(term, "Variables are not conceptualizable");
//                //return null;
//            }
//
//        } else {
//
//            term = unneg(term);
//
//            Compound prenormalized = (Compound) term.term();
//
//            Compound cterm;
//            if ((cterm = normalize(prenormalized)) == null)
//                throw new InvalidConceptException(prenormalized, "Failed normalization");
//
//            Compound aterm = Terms.atemporalize(cterm);
//            if (!(aterm instanceof Compound))
//                throw new InvalidConceptException(term, "Failed atemporalization");
//
//            term = unneg(aterm);
////            term = aterm;
//
//            //if (aterm.op() == NEG)
//            //throw new InvalidConceptException(term, "Negation re-appeared");
//
//        }


        @Nullable Termed c = get(term, createIfMissing);
        if (!(c instanceof Concept)) {
            if (createIfMissing) {
                throw new InvalidConceptException(term, "Failed to build concept");
            }
            return null;
        }

        Concept cc = (Concept)c;
        if (cc.policy() == null) {
            conceptBuilder().init(cc);
        }

        return cc;
    }

    /**
     * a string containing statistics of the index's current state
     */
    @NotNull
    public abstract String summary();

    public abstract void remove(@NotNull Termed entry);

    @Nullable
    public Term replace(@NotNull Term src, Map<Term, Term> m) {
        return termOrNull(resolve(src, new MapSubst(m)));
    }

    @Nullable
    public Term replace(@NotNull Term src, Term from, Term to) {
        return replace(src, Maps.mutable.of(from, to));
    }

    public static final class InvalidConceptException extends RuntimeException {

        @NotNull
        public final Termed term;
        @NotNull
        public final String reason;

        public InvalidConceptException(@NotNull Termed term, @NotNull String reason) {
            this.term = term;
            this.reason = reason;
        }

        @Override
        public String getMessage() {
            return "InvalidConceptTerm: " + term + " (" + term.getClass() + "): " + reason;
        }

    }


    public void loadBuiltins() {
        for (TransformConcept t : TransformConcept.BuiltIn) {
            set(t);
        }
    }


    //    static boolean possiblyTemporal(Termlike x) {
//        return (x instanceof Compound) && (!(x instanceof Concept)) && (x.hasTemporal());
//    }
//
//    @NotNull
//    public Term atemporalize2(@NotNull Compound c) {
//        if (!possiblyTemporal(c))
//            return c;
//        return new CompoundAtemporalizer(this, c).result;
//    }
//
//
//    final class CompoundAtemporalizer implements CompoundTransform<Compound, Term> {
//
//        private final TermIndex index;
//        @NotNull
//        private final Term result;
//
//        public CompoundAtemporalizer(@NotNull TermIndex index, @NotNull Compound c) {
//            this.index = index;
//            this.result = apply(null, c);
//        }
//
//
//        @Override
//        public boolean test(Term subterm) {
//            return possiblyTemporal(subterm);
//        }
//
//        @Override
//        public @Nullable Term apply(Compound parent, @NotNull Term subterm) {
//
//            Compound c = (Compound)subterm;
//            TermIndex i = index;
//
//            Term x = c;
//            int dt = c.dt();
//            if (dt!=DTERNAL) {
//                Op o = c.op();
//                if (o.temporal) {
//                    //int edt; //for non-commutative conjunctions, use XTERNAL as a placeholder to prevent flattening
//                    //if (o == CONJ && dt != 0 && csubs.hasAny(CONJ.bit)) {
//                    //edt = XTERNAL;
//                    //} else {
//                    //edt = DTERNAL;
//                    //}
//                    //Term xx = i.builder().build(o, edt, csubs.terms());
//
//                    GenericCompound xx = new GenericCompound(o, DTERNAL, c.subterms());
//                    if (c.isNormalized())
//                        xx.setNormalized();
//
//                    Termed exxist = i.get(xx, false); //early exit: atemporalized to a concept already, so return
//                    if (exxist!=null)
//                        return exxist.term();
//
//                    //x = i.the(xx).term();
//                    x = xx;
//                }
//            }
//
//            //if (x instanceof Compound) {
//                return i.transform((Compound) x, this);
//            //}
//            //else
//                //return x;
//
//
//        }
//    }
}
