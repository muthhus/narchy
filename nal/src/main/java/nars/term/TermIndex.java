package nars.term;

import nars.$;
import nars.Global;
import nars.Narsese;
import nars.Op;
import nars.budget.Budget;
import nars.nal.meta.PremiseAware;
import nars.nal.meta.PremiseEval;
import nars.nal.meta.match.EllipsisMatch;
import nars.nal.op.ImmediateTermTransform;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.compound.GenericCompound;
import nars.term.container.TermContainer;
import nars.term.container.TermVector;
import nars.term.transform.CompoundTransform;
import nars.term.transform.VariableNormalization;
import nars.term.transform.subst.Subst;
import nars.term.variable.GenericVariable;
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
import static nars.nal.Tense.ITERNAL;

/**
 *
 */
public interface TermIndex  {


    /** getOrAdd the term */
    @Nullable Termed the(@NotNull Termed t);

    /** get if not absent */
    @Nullable Termed get(@NotNull Termed t);


        //DEFAULT IMPL to be moved to a concrete class: BUILDS ON THE HEAP:
        //return builder().make(op, relation, subterms, dt);
    //}

//    @Nullable
//    default Termed newCompound(Op op, int relation, TermContainer subterms) {
//        //DEFAULT IMPL to be moved to a concrete class: BUILDS ON THE HEAP:
//        return newCompound(op, relation, subterms, ITERNAL);
//    }
    @Nullable
    default Termed the(Op op, TermContainer subterms) {
        //DEFAULT IMPL to be moved to a concrete class: BUILDS ON THE HEAP:
        return builder().the(op, -1, ITERNAL, subterms);
    }
    @Nullable
    default Termed the(Op op, Collection<Term> subterms) {
        //DEFAULT IMPL to be moved to a concrete class: BUILDS ON THE HEAP:
        return the(op, TermContainer.the(op, subterms));
    }



    void clear();

    void forEach(Consumer<? super Termed> c);



    /** # of contained terms */
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



    @NotNull
    static Task spawn(@NotNull Task parent, @NotNull Compound content, char punctuation, Truth truth, long occ, @NotNull Budget budget) {
        return spawn(parent, content, punctuation, truth, occ, budget.pri(), budget.dur(), budget.qua());
    }

    @NotNull
    static Task spawn(@NotNull Task parent, @NotNull Compound content, char punctuation, Truth truth, long occ, float p, float d, float q) {
        return new MutableTask(content, punctuation)
                .truth(truth)
                .budget(p, d, q)
                .parent(parent)
                .occurr(occ);
    }


    @NotNull
    TermContainer theSubterms(TermContainer s);

    @Nullable default TermContainer normalize(TermContainer s) {
        if (s instanceof TermVector)
            return normalize((TermVector)s);
        else if (s instanceof Terms.EmptyTermContainer)
            return s;

        //TODO implement normalization for any non-container types
        throw new UnsupportedOperationException();
    }

    /** should be called after a new entry needed to be created for the novel termcontainer */
    @Nullable default TermContainer normalize(TermVector s) {
        Term[] x = s.terms();
        int l = x.length;
        for (int i = 0; i < l; i++) {
            Term a = x[i];
            Termed b = the(a);
            if ((a!=b) && b!=null/* && a.equals(b)*/) {
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
    default Term transform(@Nullable Term trc, @NotNull Subst f) {

        if (trc == null)
            return null; //pass-through

        Term y = f.term(trc);
        if (y != null)
            return y;

        int len = trc.size();
        if (len == 0)
            return trc; //atomic, proceed no further

        Compound src = (Compound)trc;
        Op sop = src.op();
        final int maxArity = sop.maxSize;

        List<Term> sub = Global.newArrayList(len /* estimate */);
        for (int i = 0; i < len; i++) {
            Term t = src.term(i);
            Term u = apply(f, t);

            if (u instanceof EllipsisMatch) {

                EllipsisMatch m = (EllipsisMatch) u;

                if (maxArity != -1 && m.size() + sub.size() > maxArity) {
                    return src; //invalid transformation, violates arity constraint
                }

                Collections.addAll(sub, m.term);

            } else {

                if (maxArity != -1 && 1 + sub.size() > maxArity) {
                    return src; //invalid transformation, violates arity constraint
                }

                sub.add(u != null ? u : t);
            }
        }

        final int minArity = sop.minSize;
        int resultSize = sub.size();

        //early filter for invalid image
        if (sop.isImage()) {
            if (sub.isEmpty() || (resultSize==1 && sub.get(0).equals( Imdex)) )
                return src;
        }

        if ((minArity!=-1) && (resultSize < minArity)) {
            //?
        } else if (resultSize == 0) {
            switch (sop) {
                case PRODUCT: return Terms.ZeroProduct;
                default:
                    return src;
            }
        }


        Term result = this.builder().newCompound(src, TermContainer.the(sop, sub));


        //apply any known immediate transform operators
        //TODO decide if this is evaluated incorrectly somehow in reverse
        if (result != null) {
            if (isOperation(result)) {
                ImmediateTermTransform tf = f.getTransform(Operator.operator((Compound) result));
                if (tf != null) {
                    result = applyImmediateTransform(f, result, tf);
                }
            }
        } else {
            result = src;
        }

        return result;
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
    default Term apply(@NotNull Subst f, @NotNull Term src) {


        if (src instanceof Compound) {
            //if f is empty there will be no changes to apply anyway
            return f.isEmpty() ? src : transform(src, f);

        } else if (src instanceof Variable) {
            Term x = f.term(src);
            if (x != null)
                return x;
        }

        return src;

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
    }


    @Nullable
    default Term normalized(@NotNull Term t) {
        if (t instanceof Compound && !t.isNormalized()) {
            Compound ct = (Compound)t;
            t = transform(ct,
                    (ct.vars() == 1) ?
                        VariableNormalization.singleVariableNormalization :
                        new VariableNormalization()
            );

            if (t != null) {
                ((GenericCompound) t).setNormalized();
            }
        }
        return t;
    }


    CompoundTransform CompoundAnonymizer = new CompoundTransform<Compound,Term>() {

        @Override public boolean test(Term term) {
            return true;
        }

        @Override public Term apply(Compound parent, @NotNull Term subterm, int depth) {
            return subterm.anonymous();
        }
    };


    @Nullable
    default Term transform(@NotNull Compound src, @NotNull CompoundTransform t) {
        if (!t.testSuperTerm(src)) {
            return src; //nothing changed
        }

        Term[] newSubterms = new Term[src.size()];

        int mods = transform(src, t, newSubterms, 0);

        if (mods == -1) {
            return null;
        } else if ((mods > 0)) {
            return builder().newCompound(src, TermContainer.the(src.op(), newSubterms));
        }
        return src; //nothing changed
    }


    /**
     * returns how many subterms were modified, or -1 if failure (ex: results in invalid term)
     */
    default <T extends Term> int transform(@NotNull Compound src, @NotNull CompoundTransform<Compound<T>, T> trans, Term[] target, int level) {
        int n = src.size();

        int modifications = 0;

        for (int i = 0; i < n; i++) {
            Term x = src.term(i);
            if (x == null)
                throw new InvalidTerm();

            if (trans.test(x)) {

                Term x2 = trans.apply((Compound<T>) src, (T) x, level);
                if (x2 == null)
                    return -1;

                if (x!=x2) { //REFERENCE EQUALTY
                    modifications++;
                    x = x2;
                }

            } else if (x instanceof Compound) {
                //recurse
                Compound cx = (Compound) x;
                if (trans.testSuperTerm(cx)) {

                    Term[] yy = new Term[cx.size()];
                    int submods = transform(cx, trans, yy, level + 1);

                    if (submods == -1) return -1;
                    if (submods > 0) {

                        //method 1: using termindex
//                        x = newTerm(cx.op(), cx.relation(), cx.t(),
//                            TermContainer.the(cx.op(), yy)
//                        );

                        //method 2: on heap
                        Op op = cx.op();
                        int dt = cx.dt();

                        //this constructs the new term on the heap, not in this index.
                        //TODO make this configurable
                        x = $.the(op, cx.relation(), dt,
                                TermContainer.the(op, yy)
                        );

                        if (x == null)
                            return -1;
                        modifications += (x!=cx) ? 1 : 0; //REFERENCE EQUALTY
                    }
                }
            }
            target[i] = x;
        }

        return modifications;
    }

    /** return null if nothing matched */
    @Nullable default <T extends Termed> T get(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T)get(parse(termToParse));
    }

    @Nullable
    default Termed parse(@NotNull String termToParse) {
        return Narsese.the().term(termToParse, this);
    }

    @Nullable default <T extends Termed> T the(@NotNull String termToParse) throws Narsese.NarseseException {
        return (T)the(parse(termToParse));
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
}
