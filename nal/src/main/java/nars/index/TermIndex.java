package nars.index;

import nars.$;
import nars.Global;
import nars.Narsese;
import nars.Op;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.op.TermTransform;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.*;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.subst.MapSubst;
import nars.term.subst.Subst;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.variable.Variable;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;
import static nars.term.Termed.termOrNull;

/**
 *
 */
public interface TermIndex {


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
    @Nullable
    void set(@NotNull Termed src, Termed target);

    @Nullable
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


    TermBuilder builder();

    @Nullable Concept.@Nullable ConceptBuilder conceptBuilder();


    @NotNull
    static Task spawn(@NotNull Task parent, @NotNull Compound content, char punctuation, Truth truth, long occ, @NotNull Budget budget) {
        return spawn(parent, content, punctuation, truth, occ, budget.pri(), budget.dur(), budget.qua());
    }

    @NotNull
    static Task spawn(@NotNull Task parent, @NotNull Compound content, char punctuation, Truth truth, long occ, float p, float d, float q) {
        return new MutableTask(content, punctuation, truth)
                .budget(p, d, q)
                .parent(parent)
                .occurr(occ);
    }


    @Nullable
    TermContainer theSubterms(TermContainer s);

    @Nullable
    default TermContainer normalize(TermContainer s) {
        if (s instanceof TermVector)
            return normalize((TermVector) s);


        //TODO implement normalization for any non-container types
        throw new UnsupportedOperationException();
    }

    /**
     * should be called after a new entry needed to be created for the novel termcontainer
     */
    @Nullable
    default TermContainer normalize(@NotNull TermVector s) {
        Term[] x = s.terms();
        int l = x.length;
        for (int i = 0; i < l; i++) {
            Term a = x[i];
            Termed b = the(a);
            if (b == null)
                return null;
            if (a != b) {
                //different instance but still equal so replace it in the origin array, otherwise leave as-is
                x[i] = b.term();
            }
        }
        return s;
    }


    int subtermsCount();

//    default TermContainer internSubterms(Term[] t) {
//        return new TermVector<>(t, this::the);
//    }

    @Nullable
    default Term build(@NotNull Compound csrc, Term[] newSubs) {
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

        boolean strict = f instanceof PremiseEval;



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

        List<Term> sub = Global.newArrayList(len /* estimate */);


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


    default Termed<Compound> normalize(@NotNull Termed<Compound> t, boolean insert) {
        if (/*t instanceof Compound &&*/ !t.isNormalized()) {
            Compound ct = (Compound) t;
            int numVars = ct.vars();
            t = transform(ct,
                    (numVars == 1 && ct.varPattern() == 0) ?
                            VariableNormalization.singleVariableNormalization :
                            new VariableNormalization(numVars)
            );

//            if (!(t instanceof Compound)) //includes null test
//                return null;

            ((GenericCompound) t).setNormalized();

        }

        // TODO also eligible for fast concept resolution is if it is temporal but has no temporal relations
        if (t instanceof Compound && ((Compound)t).hasTemporal()) {
            return t;
        } else {
            //fast resolve to concept if not temporal:
            Termed shared = insert ? the(t) : get(t);
            return shared != null ? shared : t;
        }
    }




    @Nullable
    default Term build(@NotNull Compound src, @NotNull List<Term> newSubs) {
        return build(src, newSubs.toArray(new Term[newSubs.size()]) );
    }


    @Nullable
    default Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        return !t.testSuperTerm(src) ? src : _transform(src, t);
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
                y = termOrNull(trans.apply(src, x));
            } else if (x instanceof Compound) {
                y = transform((Compound) x, trans); //recurse
            }

            if (y == null)
                return null;

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

    /**
     * return null if nothing matched
     */
    @Nullable
    default <T extends Termed> T get(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) get(fromString(termToParse));
    }

    @NotNull
    default Term fromString(@NotNull String termToParse) throws Narsese.NarseseException {
        return Narsese.the().term(termToParse, this, true);
    }
    @NotNull
    default Term fromStringRaw(@NotNull String termToParse) throws Narsese.NarseseException {
        return Narsese.the().term(termToParse, this, false);
    }

    @Nullable
    default <T extends Termed> T the(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) /*the*/(fromString(termToParse));
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
    default Concept concept(@NotNull Termed term, boolean createIfMissing) {


        if (term.op() == NEG) {
            //unwrap negation
            term = ((Compound)term).term(0);
            if (term instanceof Atomic) {
                //negations of non-DepVar atomics are invalid
                if (term.op() != Op.VAR_DEP) {
                    throw new InvalidConceptTerm(term);
                }
            }

        }


        if (term instanceof Atomic) {

            if (term instanceof Variable)
                throw new InvalidConceptTerm(term);


        } else {

            Termed prenormalized = term;
            if ((term = normalize(term, createIfMissing)) == null)
                throw new InvalidTerm(prenormalized);

            if ((term = atemporalize((Compound)term)) == null)
                //throw new InvalidTerm(prenormalizetd);
                return null; //probably unforseeable
        }

//        if (tt == null)
//            return null;
//
//        Termed c = createIfMissing ? index.the(tt) : index.get(tt);
//        if (c == null)
//            return null;
//        if (!(c instanceof Concept)) {
//            //throw new RuntimeException("not a concept: " + c + " while resolving: " + t + " create=" + createIfMissing);
//            return null;
//        }
//        return (Concept) c;

        @Nullable Termed c = get(term, createIfMissing);
        if (c == null)
            return null;
        if (!(c instanceof Concept)) {
            throw new RuntimeException("not a concept: " + c + " (" + c.getClass() + ')');
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

    default Term remap(@NotNull Term src, Map<Term, Term> m) {
        return termOrNull(resolve(src, new MapSubst(m)));
    }

    default Termed remove(Termed entry) {
        throw new UnsupportedOperationException();
    }


    final class InvalidConceptTerm extends RuntimeException {

        public final Termed term;

        public InvalidConceptTerm(Termed term) {
            this.term = term;
        }

        @NotNull
        @Override
        public String toString() {
            return "InvalidConceptTerm: " + term + " (" + term.getClass() + ")";
        }
    }

    final class InvalidTaskTerm extends RuntimeException {

        public final Termed term;

        public InvalidTaskTerm(Termed term) {
            this(term, "InvalidTaskTerm");
        }

        public InvalidTaskTerm(Termed term, String message) {
            super(message);
            this.term = term;
        }

        @NotNull
        @Override
        public String toString() {
            return getMessage() + ": " + term;
        }
    }


    @Nullable
    default Compound atemporalize(@NotNull Compound c) {
        return (Compound) transform(
                (c.op().temporal) ? c.dt(DTERNAL) : c,
                CompoundAtemporalizer);
    }

    @Nullable CompoundTransform<Compound,Term> CompoundAtemporalizer = new CompoundTransform<>() {

        @Override
        public boolean test(Term term) {
            return true; // term.hasTemporal();
        }

        @NotNull
        @Override
        public Termed apply(Compound parent, @NotNull Term subterm) {
            if (subterm instanceof Compound) {
                Compound csub = (Compound) subterm;
                if (csub.hasTemporal()) {
                    return /*the*/($.terms.atemporalize(csub));
                }
            }
            return /*the*/(subterm);
        }
    };


}
