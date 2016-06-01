package nars.index;

import nars.Global;
import nars.Narsese;
import nars.Op;
import nars.budget.Budget;
import nars.concept.Concept;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.op.ImmediateTermTransform;
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import static nars.Op.*;
import static nars.nal.Tense.DTERNAL;

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


        //DEFAULT IMPL to be moved to a concrete class: BUILDS ON THE HEAP:
    //return builder().make(op, relation, subterms, dt);
    //}

    //    @Nullable
//    default Termed newCompound(Op op, int relation, TermContainer subterms) {
//        //DEFAULT IMPL to be moved to a concrete class: BUILDS ON THE HEAP:
//        return newCompound(op, relation, subterms, ITERNAL);
//    }
    @Nullable
    default Termed the(@NotNull Op op, @NotNull TermContainer subterms) {
        //DEFAULT IMPL to be moved to a concrete class: BUILDS ON THE HEAP:
        Term b = builder().build(op, -1, DTERNAL, subterms);
        return b == null ? null : the(b);
    }

    @Nullable
    default Termed the(@NotNull Op op, @NotNull Collection<Term> subterms) {
        //DEFAULT IMPL to be moved to a concrete class: BUILDS ON THE HEAP:
        return the(op, TermContainer.the(op, subterms));
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
    int InvalidEquivalenceTerm = or(IMPLICATION, EQUIV);
    /**
     * equivalences and intervals (not implications, they are allowed
     */
    int InvalidImplicationPredicate = or(EQUIV);


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
    default Term theTransformed(@NotNull Compound csrc, @NotNull TermContainer subs) {
        return builder().buildTransformed(csrc, subs);
    }


    /**
     * returns the resolved term according to the substitution
     */
    @Nullable
    default Term resolve(@NotNull Term src, @NotNull Subst f) {

        if (src == null)
            throw new NullPointerException();
        //return null; //pass-through

        //constant atom or zero-length compound, ex: ()
        int len = src.size();
        boolean variable;
        if (len == 0) {
            if (!(variable = (src instanceof Variable)))
                return src; //constant term, of which none should be mapped in the subst
        } else {
            variable = false;
        }


        Term y = f.term(src);
        if (y != null)
            return y; //an assigned substitution, whether a variable or other type of term
        else if (variable)
            return null; //unassigned variable

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

                Term[] ee = ((EllipsisMatch) u).term;
                Collections.addAll(sub, ee);
                changed = true; //just assume it was changed

            } else {

                //                if (maxArity != -1 && 1 + sub.size() > maxArity) {
                //                    return src; //invalid transformation, violates arity constraint
                //                }

                if (t!=u) {
                    if (u == null) {
                        u = t; //keep value
                    } else {//!changed && !t.equals(u)) {
                        changed = true; //check for any changes
                    }
                }

                sub.add(u);
            }
        }

        if (!changed)
            return src;

        //Prefilters
        Op sop = src.op();

        //Prefilters?
        //        if ((minArity!=-1) && (resultSize < minArity)) {
        //            //?
        //        }

        if (sop.isStatement() && (sub.size() != 2 || sub.get(0).equals(sub.get(1))))
            return null; //transformed to degenerate statement

        Term result = theTransformed(crc, TermContainer.the(sop, sub));

        if (result != null) {

            //post-process: apply any known immediate transform operators
            if (isOperation(result)) {
                Compound cres = (Compound)result;
                ImmediateTermTransform tf = f.getTransform(Operator.operator(cres));
                if (tf != null) {
                    result = applyImmediateTransform(f, cres, tf);
                }
            }
        } /*else {
//            //why was it null?
//            if (!sop.isStatement())
//                System.err.println(crc + " " + TermContainer.the(sop, sub));


            if (Global.DEBUG_PARANOID) {

                //these should not happen

                if (sop.isStatement() && sub.size() != 2) {
                    throw new RuntimeException("transformed to degenerate statement");
                } else if (sop.isImage()) {
                    int resultSize = sub.size();
                    if (resultSize == 0 || (resultSize == 1 && sub.get(0).equals(Imdex)))
                        throw new RuntimeException("transformed to degenerate image");
                }
            }
        }*/

        return result;
    }


    @Nullable
    default Term applyImmediateTransform(Subst f, @NotNull Compound result, ImmediateTermTransform tf) {

        //Compound args = (Compound) Operator.opArgs((Compound) result).apply(f);
        Compound args = Operator.opArgs(result);

        return ((tf instanceof PremiseAware) && (f instanceof PremiseEval)) ?
                ((PremiseAware) tf).function(args, (PremiseEval) f) :
                tf.function(args, this);
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
    default Termed<Compound> normalized(@NotNull Termed<Compound> t) {
        if (/*t instanceof Compound &&*/ !t.isNormalized()) {
            Compound ct = (Compound) t;
            int numVars = ct.vars();
            t = transform(ct,
                    (ct.vars() == 1 && ct.varPattern() == 0) ?
                            VariableNormalization.singleVariableNormalization :
                            new VariableNormalization(numVars)
            );

            if (!(t instanceof Compound)) //includes null test
                return null;

            ((GenericCompound) t).setNormalized();

        }



//        // TODO also eligible for fast concept resolution is if it is temporal but has no temporal relations
//        if (t instanceof Compound && ((Compound)t).hasTemporal())
//            return t;

        return t;

        //fast resolve to concept if not temporal:
        //Termed existing = the(t);
//        if (existing != null)
//            return existing;
//        else
//            return t;
    }




    @Nullable
    default Term theTransformed(@NotNull Compound src, @NotNull Term[] newSubs) {
        return theTransformed(src,
                //theSubterms(
                        TermContainer.the(src.op(), newSubs)
                //)
        );
    }


    @Nullable
    default Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        return !t.testSuperTerm(src) ? src : _transform(src, t);
    }


    /**
     * returns how many subterms were modified, or -1 if failure (ex: results in invalid term)
     */
    @Nullable
    default Term _transform(@NotNull Compound src, @NotNull CompoundTransform<Compound, Term> trans) {

        int n = src.size();

        int modifications = 0;

        Term[] target = new Term[n];

        for (int i = 0; i < n; i++) {
            Term x = src.term(i);

            Term cx = x;

            if (trans.test(x)) {
                cx = trans.apply(src, x).term();
            } else if (x instanceof Compound) {
                cx = transform((Compound) x, trans); //recurse
            }

            if (x!= cx) { //must be refernce equality test for some variable normalization cases
                if (cx == null)
                    return null;

                modifications++;
                x = cx;
            }

            target[i] = x;
        }

        return modifications > 0 ? theTransformed(src, target) : src;
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
        return (T) the(fromString(termToParse));
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
    default Termed validConceptTerm(@NotNull Termed term) {

        if (term instanceof Atomic) {

            if (term instanceof Variable)
                throw new InvalidConceptTerm(term);

            return term;

        } else {
            //COMPOUND -------
            Compound tc = (Compound) term.term();

            if (tc.op() == NEGATE) {
//                Term t0 = tc.term(0);
//                if (t0 instanceof Atomic) {
//                    //negations of non-DepVar atomics are invalid
//                    if (t0.op() != Op.VAR_DEP) {
//                        if (Global.DEBUG)
//                            throw new InvalidConceptTerm(term);
//                        else
//                            return null;
//                    }
//                }
                return null;
            }

            //NORMALIZATION
            Compound prenormalized = tc;
            if ((term = normalized(term)) == null)
                throw new InvalidTerm(prenormalized);

            return atemporalize((Compound)term);

//            //ANONYMIZATION
//            //TODO ? put the unnormalized term for cached future normalizations?
//            Termed anonymizedTerm = term.anonymous();
//            if (anonymizedTerm != term) {
//                //complete anonymization process
//                if (null == (anonymizedTerm = transform((Compound) anonymizedTerm, CompoundAnonymizer))) {
//                    if (Global.DEBUG)
//                        throw new InvalidTerm((Compound) term);
//                    else
//                        return null;
//                }
//
//                term = anonymizedTerm;
//            }
//
//            if (term.term().isTemporal())
//                throw new RuntimeException("anonymization failure");
//
//            return term;
        }

    }

    /**
     * a string containing statistics of the index's current state
     */
    @NotNull
    default String summary() {
        return "";
    }

    @NotNull
    default Term remap(Map<Term, Term> m, @NotNull Term src) {
        return resolve(src, new MapSubst(m)).term();
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
            return "InvalidConceptTerm: " + term;
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
        return (Compound) transform(c.dt(DTERNAL), CompoundAtemporalizer);
    }

    @Nullable CompoundTransform CompoundAtemporalizer = new CompoundTransform<>() {

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
                    return /*the*/(Terms.terms.atemporalize(csub));
                }
            }
            return /*the*/(subterm);
        }
    };


}
