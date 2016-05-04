package nars.term;

import nars.Global;
import nars.Narsese;
import nars.Op;
import nars.budget.Budget;
import nars.concept.ConceptBuilder;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.op.ImmediateTermTransform;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.transform.subst.Subst;
import nars.term.variable.Variable;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
    @Nullable
    default Termed the(@NotNull Termed t) {
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
     * set if not absent
     */
    @Nullable
    Termed set(@NotNull Termed t);

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
        Term b = builder().the(op, -1, DTERNAL, subterms);
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

    @Nullable
    ConceptBuilder conceptBuilder();


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


    /**
     * returns the resolved term according to the substitution
     */
    @Nullable
    default Termed transform(@Nullable Compound input, @NotNull Subst f) {

        if (input == null)
            return null; //pass-through

        //if f is empty there will be no changes to apply anyway
        if (f.isEmpty())
            return input;

        Term y = f.term(input);
        if (y != null)
            return y;

        int len = input.size();
        if (len == 0)
            return input; //atomic or zero size ( ex: () ), proceed no further

        Compound src = input;

        List<Term> sub = Global.newArrayList(len /* estimate */);
        for (int i = 0; i < len; i++) {
            Term t = src.term(i);
            Term u = Termed.termOrNull(apply(f, t));

            if (u instanceof EllipsisMatch) {

                EllipsisMatch m = (EllipsisMatch) u;

//                if (maxArity != -1 && m.size() + sub.size() > maxArity) {
//                    return src; //invalid transformation, violated arity constraint
//                }

                Collections.addAll(sub, m.term);

            } else {

//                if (maxArity != -1 && 1 + sub.size() > maxArity) {
//                    return src; //invalid transformation, violates arity constraint
//                }

                sub.add(u != null ? u : t);
            }
        }



        //Prefilters
        Op sop = src.op();

        if (sop.isStatement() && sub.size()!=2)
            return null;

        TermContainer transformedSubterms = TermContainer.the(sop, sub);

        Termed result;
        if (transformedSubterms.equals(input.subterms())) {
            result = input;
        } else {

            result = transformedCompound(src, transformedSubterms);

            //Filtering of transformed result:
            if (result == null)
                return null;

            //apply any known immediate transform operators
            //TODO decide if this is evaluated incorrectly somehow in reverse

            if (sop.isImage()) {
                int resultSize = sub.size();
                if (sub.isEmpty() || (resultSize == 1 && sub.get(0).equals(Imdex)))
                    return null;
            }

            //        if ((minArity!=-1) && (resultSize < minArity)) {
            //            //?
            //        }

        }


        //Post-Processnig of result:

        if (isOperation(result)) {
            ImmediateTermTransform tf = f.getTransform(Operator.operator((Compound) result));
            if (tf != null) {
                result = applyImmediateTransform(f, result.term(), tf);
            }
        }

        return result!=null ? result.term() : null;
    }

    @Nullable
    default Termed transformedCompound(@NotNull Compound src, @NotNull TermContainer transformedSubterms) {
        return /*Termed t = */builder().theTransformed(src, transformedSubterms);
        //return t != null ? the(t) : null;
    }




    @Nullable
    default Term applyImmediateTransform(Subst f, Term result, ImmediateTermTransform tf) {

        //Compound args = (Compound) Operator.opArgs((Compound) result).apply(f);
        Compound args = Operator.opArgs((Compound) result);

        return ((tf instanceof PremiseAware) && (f instanceof PremiseEval)) ?
                ((PremiseAware) tf).function(args, (PremiseEval) f) :
                tf.function(args, this);
    }


    @Nullable
    default Termed apply(@NotNull Subst f, @NotNull Term src) {

        if (src instanceof Compound) {
            return transform((Compound) src, f);
        } else if (src instanceof Variable) {
            return f.term(src);
        } else {
            return src;
        }
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
    default Termed normalized(@NotNull Termed t) {
        if (/*t instanceof Compound &&*/ !t.isNormalized()) {
            Compound ct = (Compound) t;
            t = transform(ct,
                    (ct.vars() == 1) ?
                            VariableNormalization.singleVariableNormalization :
                            new VariableNormalization()
            );

            if (t != null) {
                ((GenericCompound) t).setNormalized();
            }
        } else {

            //fast access to concept:
            if (!t.op().isTemporal()) {
                Termed existing = get(t);
                if (existing != null)
                    return existing;
            }
        }
        return t;
    }


    CompoundTransform CompoundAnonymizer = new CompoundTransform<Compound, Term>() {

        @Override
        public boolean test(Term term) {
            return true;
        }

        @NotNull @Override
        public Termed apply(Compound parent, @NotNull Term subterm) {
            return subterm.anonymous();
        }
    };



    @Nullable
    default Term theTransformed(@NotNull Compound src, @Nullable Term[] newSubs) {
        return ((newSubs == null) || (newSubs == src.terms())) ?
                src :
                builder().theTransformed(src, TermContainer.the(src.op(), newSubs));
    }


    @Nullable
    default Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        if (!t.testSuperTerm(src)) {
            return src; //nothing changed
        } else {
            return theTransformed(src, _transform(src, t));
        }
    }


    /**
     * returns how many subterms were modified, or -1 if failure (ex: results in invalid term)
     */
    @NotNull
    default Term[] _transform(@NotNull Compound src, @NotNull CompoundTransform<Compound, Term> trans) {

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

            if (cx == null)
                return null;

            if (x!=cx) { //REFERENCE EQUALTY
                modifications++;
                x = cx;
            }

            target[i] = x;
        }

        return modifications > 0 ? target : src.terms();
    }

    /**
     * return null if nothing matched
     */
    @Nullable
    default <T extends Termed> T get(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) get(parse(termToParse));
    }

    @NotNull
    default Term parse(@NotNull String termToParse) throws Narsese.NarseseException {
        return Narsese.the().term(termToParse, this);
    }

    @Nullable
    default <T extends Termed> T the(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T) the(parse(termToParse));
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
            Compound tc = (Compound)term.term();

            if (tc.op() == NEGATE && tc.term(0) instanceof Atomic) {
                //negations of atoms are invalid
                throw new InvalidConceptTerm(term);
            }

            //NORMALIZATION
            Compound prenormalized = tc;
            if ((term = normalized(term)) == null)
                throw new InvalidTerm(prenormalized);

            //ANONYMIZATION
            //TODO ? put the unnormalized term for cached future normalizations?
            Termed anonymizedTerm = term.anonymous();
            if (anonymizedTerm != term) {
                //complete anonymization process
                if (null == (anonymizedTerm = transform((Compound) anonymizedTerm, CompoundAnonymizer))) {
                    if (Global.DEBUG)
                        throw new InvalidTerm((Compound) term);
                    else
                        return null;
                }

                term = anonymizedTerm;
            }

            return term;
        }

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

}
