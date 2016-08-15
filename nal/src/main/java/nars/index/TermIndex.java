package nars.index;

import nars.NAR;
import nars.Param;
import org.eclipse.collections.api.list.primitive.ByteList;
import org.eclipse.collections.impl.factory.Maps;
import nars.$;
import nars.Narsese;
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
import nars.term.container.TermContainer;
import nars.term.subst.MapSubst;
import nars.term.subst.Subst;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.var.Variable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nars.$.unneg;
import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;
import static nars.term.Termed.termOrNull;
import static nars.term.Terms.compoundOrNull;

/**
 *
 */
public interface TermIndex {


    String VARIABLES_ARE_NOT_CONCEPTUALIZABLE = "Variables are not conceptualizable";

    /**
     * getOrAdd the term
     */
    default @Nullable Termed the(@NotNull Termed t) {
        return get(t, true);
    }

    /**
     * get if not absent
     */
    @Nullable
    default Termed get(@NotNull Termed t) {
        return get(t, false);
    }

    //use concept(key, craeteIfMissing) instead
    @Nullable Termed get(@NotNull Termed key, boolean createIfMissing);


    /**
     * set whether absent or not
     */
    void set(@NotNull Termed src, Termed target);

    default void set(@NotNull Termed t) {
        set(t, t);
    }


    void clear();

    void forEach(Consumer<? super Termed> c);


    default public void start(NAR nar) { }

    /**
     * # of contained terms
     */
    int size();

    /**
     * implications, equivalences, and interval
     */
    int InvalidEquivalenceTerm = or(IMPL, EQUI);
    /**
     * equivalences and intervals (not implications, they are allowed
     */
    int InvalidImplicationSubject = or(EQUI, IMPL);
    int InvalidImplicationPredicate = or(EQUI);


    @NotNull TermBuilder builder();

    @Nullable Concept.ConceptBuilder conceptBuilder();


    @Nullable
    TermContainer theSubterms(TermContainer s);

//    @Nullable
//    default TermContainer normalize(TermContainer s) {
//        if (s instanceof TermVector)
//            return normalize((TermVector) s);
//
//
//        //TODO implement normalization for any non-container types
//        throw new UnsupportedOperationException();
//    }

//    /**
//     * should be called after a new entry needed to be created for the novel termcontainer
//     */
//    @Nullable
//    default TermContainer normalize(@NotNull TermVector s) {
//        Term[] x = s.terms();
//        int l = x.length;
//        for (int i = 0; i < l; i++) {
//            Term a = x[i];
//            Termed b = the(a);
//            if (b == null)
//                return null;
//            if (a != b) {
//                //different instance but still equal so replace it in the origin array, otherwise leave as-is
//                x[i] = b.term();
//            }
//        }
//        return s;
//    }


    int subtermsCount();

//    default TermContainer internSubterms(Term[] t) {
//        return new TermVector<>(t, this::the);
//    }

    @Nullable
    default Term build(@NotNull Compound csrc, @NotNull Term[] newSubs) {
        return builder().build(csrc.op(), csrc.dt(), newSubs);
    }


    /**
     * returns the resolved term according to the substitution
     */
    @Nullable
    default Term resolve(@NotNull Term src, @NotNull Subst f) {


        Term y = f.term(src);
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

        Compound crc = (Compound) src;

        List<Term> sub = $.newArrayList(len /* estimate */);

        boolean strict = f instanceof PremiseEval;

        boolean changed = false;
        for (int i = 0; i < len; i++) {
            Term t = crc.term(i);
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

        return changed ? build(crc, sub.toArray(new Term[sub.size()])) : crc;
    }

//    default Term immediates(@NotNull Subst f, Term result) {
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
    default Term transform(Subst f, @NotNull Compound result, TermTransform tf) {

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

//    /** default memory-based (Guava) cache */
//    @NotNull
//    static TermIndex memory(int capacity) {
////        CacheBuilder builder = CacheBuilder.newBuilder()
////            .maximumSize(capacity);
//        return new MapIndex2(
//            new HashMap(capacity*2),
//                //new UnifriedMap()
//                new DefaultConceptBuilder());
////        return new MapIndex2(
////                new HashMap(capacity)
////                //new UnifriedMap()
////        );
//    }
//    /** default memory-based (Guava) cache */
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

    default void print(@NotNull PrintStream out) {
        forEach(out::println);
        out.println();
    }


    @NotNull
    default Compound normalize(@NotNull Termed<Compound> t) {
        Compound r;
        if (!t.isNormalized()) {
            Compound ct = (Compound) t;
            int numVars = ct.vars();

            Term t2;
            if (numVars == 1 && ct.varPattern() == 0) {
                t2 = transform(ct, VariableNormalization.singleVariableNormalization);
            } else {
                t2 = transform(ct, new VariableNormalization(numVars));
            }

            /*if (!(t2 instanceof Compound)) { //includes null test
                if (Global.DEBUG)
                    System.err.println(t + " TermIndex.normalize() produced null");
                return null;
            }*/

            if (!(t2 instanceof Compound)) {
                throw new InvalidTermException(ct.op(), ct.dt(), ct.terms(),
                    Param.DEBUG ? "normalized to non-compound: " + t2 : "normalized to non-compound"
                );
            }

            ((GenericCompound) t2).setNormalized();
            r = (Compound) t2;

        } else {
            r = compoundOrNull(t);
            if (r == null)
                throw new InvalidTermException(t.op(), DTERNAL, new Term[]{},
                    Param.DEBUG ? "normalizing non-compound: " + t : "normalizing non-compound"
                );
        }

//        if (insert) {
//            Compound s = (Compound) termOrNull(get(r, false));
//            return s == null ? r : s; //if a concept does not exist, do not create one yet and just return the key
//        } else {
            return r;
//        }

    }


    @Nullable
    default Term build(@NotNull Compound src, @NotNull List<Term> newSubs) {
        return build(src, newSubs.toArray(new Term[newSubs.size()]));
    }


    @NotNull
    default Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        if (src == null || !t.testSuperTerm(src))
            return src;

        int n = src.size();

        int modifications = 0;

        Term[] target = new Term[n];

        for (int i = 0; i < n; i++) {
            Term x = src.term(i);

            Term y = x;

            if (t.test(x)) {
                y = t.apply(src, x).term();
            } else if (x instanceof Compound) {
                y = transform((Compound) x, t); //recurse
            }


            if (x != y) { //must be refernce equality test for some variable normalization cases
                modifications++;
                x = y;
            }

            target[i] = x;
        }

        return modifications > 0 ?
                builder().build(src.op(), src.dt(), target) : //must not allow subterms to be tested for equality, for variable normalization purpose the variables will seem equivalent but they are not
                src;

    }


    @Nullable
    default Term transform(@NotNull Compound src, @NotNull ByteList path, Term replacement) {
        return transform(src, path, 0, replacement);
    }

    @Nullable
    default Term transform(@NotNull Term src, @NotNull ByteList path, int depth, Term replacement) {
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

        return builder().build(csrc.op(), csrc.dt(), target);
    }


    @NotNull
    default Term parseRaw(@NotNull String termToParse) throws Narsese.NarseseException {
        return Narsese.the().term(termToParse, this, false);
    }

    @Nullable
    default <T extends Termed> T parse(@NotNull String termToParse) throws Narsese.NarseseException {
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
    default Concept concept(@NotNull Termed term, boolean createIfMissing) throws InvalidConceptException {

        if (term instanceof Atomic) {

            if (term instanceof Variable) {
                //if (createIfMissing)
                throw new InvalidConceptException(term, VARIABLES_ARE_NOT_CONCEPTUALIZABLE);
                //return null;
            }

        } else {

            term = unneg(term);

            Termed prenormalized = term;

            if (!((term = normalize(term)) instanceof Compound))
                throw new InvalidConceptException(prenormalized, "Failed normalization");

            Term aterm = Terms.atemporalize((Compound) term);
            if (!(aterm instanceof Compound))
                throw new InvalidConceptException(term, "Failed atemporalization");

            if (aterm instanceof Concept) {
                return (Concept) aterm;
            }

            term = unneg(aterm);
//            term = aterm;

            //if (aterm.op() == NEG)
            //throw new InvalidConceptException(term, "Negation re-appeared");

        }


        @Nullable Termed c = get(term, createIfMissing);
        if (!(c instanceof Concept)) {
            if (createIfMissing) {
                throw new InvalidConceptException(term, "Failed to build concept");
            }
            return null;
        }


        return (Concept) c;
    }

    /**
     * a string containing statistics of the index's current state
     */
    @NotNull
    default String summary() {
        return "";
    }

    @Nullable
    default Term remap(@NotNull Term src, Map<Term, Term> m) {
        return termOrNull(resolve(src, new MapSubst(m)));
    }

    @Nullable
    default Term remap(@NotNull Term src, Term from, Term to) {
        return remap(src, Maps.mutable.of(from, to));
    }

    default void remove(Termed entry) {
        throw new UnsupportedOperationException();
    }



    final class InvalidConceptException extends RuntimeException {

        @NotNull
        public final Termed term;
        @NotNull
        public final String reason;

        public InvalidConceptException(@NotNull Termed term, @NotNull String reason) {
            this.term = term;
            this.reason = reason;
        }

        @NotNull
        @Override
        public String toString() {
            return "InvalidConceptTerm: " + term + " (" + term.getClass() + "): " + reason;
        }
    }


    default void loadBuiltins() {
        for (TransformConcept t : TransformConcept.BuiltIn) {
            set(t);
        }
    }


    //    static boolean possiblyTemporal(Termlike x) {
//        return (x instanceof Compound) && (!(x instanceof Concept)) && (x.hasTemporal());
//    }
//
//    @NotNull
//    default Term atemporalize2(@NotNull Compound c) {
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
