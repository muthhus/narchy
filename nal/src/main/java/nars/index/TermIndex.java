package nars.index;

import com.gs.collections.api.list.primitive.ByteList;
import nars.$;
import nars.Narsese;
import nars.budget.policy.ConceptPolicy;
import nars.concept.Concept;
import nars.nal.TermBuilder;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.op.TermTransform;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.term.Termlike;
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

    @Nullable
    Termed get(@NotNull Termed key, boolean createIfMissing);


    /**
     * set whether absent or not
     */
    void set(@NotNull Termed src, Termed target);

    default void set(@NotNull Termed t) {
        set(t, t);
    }


    void clear();

    void forEach(Consumer<? super Termed> c);


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
        if (src.vars() + src.varPattern() == 0)
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


    @Nullable
    default Compound normalize(@NotNull Termed<Compound> t, boolean insert) {
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


            ((GenericCompound) t2).setNormalized();
            r = (Compound) t2;

        } else {
            r = t.term();
        }

        Compound s = (Compound) termOrNull(get(r, insert));

        return s == null ? r : s; //if a concept does not exist and was not created, return the key
    }


    @Nullable
    default Term build(@NotNull Compound src, @NotNull List<Term> newSubs) {
        return build(src, newSubs.toArray(new Term[newSubs.size()]));
    }


    @NotNull
    default Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        return src == null || !t.testSuperTerm(src) ? src : _transform(src, t);
    }


    @Nullable
    default Term transform(@NotNull Compound src, ByteList path, Term replacement) {
        return transform(src, path, 0, replacement);
    }

    @Nullable
    default Term transform(@NotNull Term src, ByteList path, int depth, Term replacement) {
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

    /**
     * returns how many subterms were modified, or -1 if failure (ex: results in invalid term)
     */
    @Nullable
    default Term _transform(@NotNull Compound src, @NotNull CompoundTransform trans) {


        int n = src.size();

        int modifications = 0;

        Term[] target = new Term[n];

        for (int i = 0; i < n; i++) {
            Term x = src.term(i);

            Term y = x;

            if (trans.test(x)) {
                y = trans.apply(src, x).term();
            } else if (x instanceof Compound) {
                y = transform((Compound) x, trans); //recurse
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

            if (term instanceof Variable)
                throw new InvalidConceptException(term, VARIABLES_ARE_NOT_CONCEPTUALIZABLE);

        } else {

            term = unneg(term);

            Termed prenormalized = term;

            if (!((term = normalize(term, createIfMissing)) instanceof Compound))
                throw new InvalidConceptException(prenormalized, "Failed normalization, becoming: " + term);

            Term aterm = atemporalize((Compound) term);
            if (!(aterm instanceof Compound))
                throw new InvalidConceptException(term, "Failed atemporalization, becoming: " + aterm);

            //optimization: atemporalization was unnecessary, normalization may have already provided the concept
            if ((aterm == term) && (term instanceof Concept)) {
                return (Concept) term;
            }

            //if (aterm.op() == NEG)
                //throw new InvalidConceptException(term, "Negation re-appeared");
            term = $.unneg(aterm);

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

    default void remove(Termed entry) {
        throw new UnsupportedOperationException();
    }

    default void policy(@NotNull Concept c, ConceptPolicy p) {

        synchronized (c) {
            if (c.policy() != p) {
                c.policy(p);

                c.tasklinks().commit();
                c.termlinks().commit();
            }
            set(c); //update in the cache (weight, etc.)
        }

    }

    default void activate(@NotNull Concept c, @NotNull ConceptPolicy warm) {

        if (c.policy() == warm)
            return;
        c.policy(warm);
        set(c); //update in the cache (weight, etc.)

        //clean out any deleted links since having been deactivated
        c.tasklinks().commit();
        c.termlinks().commit();
    }


    final class InvalidConceptException extends RuntimeException {

        @NotNull public final Termed term;
        @NotNull public final String reason;

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

    static boolean possiblyTemporal(Termlike x) {
        return (x instanceof Compound) && (!(x instanceof Concept)) && (x.hasTemporal());
    }

    @NotNull
    default Term atemporalize(@NotNull Compound c) {
        if (!possiblyTemporal(c))
            return c;
        return new CompoundAtemporalizer(this, c).result;
    }


    final class CompoundAtemporalizer implements CompoundTransform<Compound, Term> {

        private final TermIndex index;
        @NotNull
        private final Term result;

        public CompoundAtemporalizer(@NotNull TermIndex index, @NotNull Compound c) {
            this.index = index;
            this.result = _atemporalize(c);
        }

        @NotNull
        Term _atemporalize(@NotNull Compound c) {
            TermIndex i = index;

            Term x;
            if (c.op().temporal && c.dt() != DTERNAL) {
                Term xx = i.builder().build(c.op(), DTERNAL, c.subterms().terms());
                x = i.the(xx).term();
            } else {
                x = c;
            }

            if (x instanceof Compound)
                return i.transform((Compound) x, this);
            else
                return x;
        }

        @Override
        public boolean test(Term term) {
            return possiblyTemporal(term);
        }

        @Override
        public @Nullable Term apply(Compound parent, @NotNull Term subterm) {
            if (subterm instanceof Compound)
                return _atemporalize((Compound) subterm);
            else
                return subterm;
        }
    }
}
