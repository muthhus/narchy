package nars.term.index;

import nars.concept.DefaultConceptBuilder;
import nars.nal.meta.PatternCompound;
import nars.nal.meta.PremiseRule;
import nars.nar.AbstractNAR;
import nars.term.Compound;
import nars.term.Termed;
import nars.term.container.TermVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

/**
 * Index which specifically holds the components of a deriver ruleset
 */
public class PatternIndex extends AbstractNAR.DefaultTermIndex {

    public PatternIndex() {
        super(512);
    }


    @Override
    protected
    @Nullable
    Termed theCompound(@NotNull Compound t, boolean create) {

        /*if (!(x instanceof AbstractCompoundPattern)) {
            if (x instanceof Compound) {
                new VariableDependencies((Compound) x, Op.VAR_PATTERN);
            }

            //variable analysis
        }*/


        ///** only compile top-level terms, not their subterms */
        //if (!(x instanceof AbstractCompoundPattern)) {

        if (t instanceof PremiseRule) {
            //return new PremiseRule((Compound)x.term(0), (Compound)x.term(1));
            return t;
        }

//        if (!(x instanceof TermMetadata)) {
////            if (!Ellipsis.hasEllipsis(x)) {
////            if (!x.isCommutative()) {

        return PatternCompound.make(t,
            (TermVector) theSubterms(t.subterms())
        );

//                    return new LinearCompoundPattern(x, (TermVector) subs);
//                } else {
//                    return new CommutiveCompoundPattern(x, (TermVector) subs);
//            }
//            }
//        }
//        //}
//
//        return super.compileCompound(x);
    }

//    private static class PatternTermBuilder extends TermBuilder {
//        @Override
//        public
//        @Nullable
//        Termed make(Op op, int relation, TermContainer subterms, int dt) {
//            return null;
//        }
//    }

//    @Override
//    public
//    @Nullable
//    Termed the(Term x) {
//        //HACK - ellipsis contains additional metadata we want to save
//        return (x instanceof Ellipsis) ? x : super.the(x);
//    }


    //
//    /** non-commutive simple compound which can match subterms in any order, but this order is prearranged optimally */
//    static final class LinearCompoundPattern extends AbstractCompoundPattern {
//
//        private final int[] dependencyOrder;
//        //private final int[] heuristicOrder;
//        //private final int[] shuffleOrder;
//
//        public LinearCompoundPattern(Compound seed, TermVector subterms) {
//            super(seed, subterms);
//            dependencyOrder = getDependencyOrder(seed);
//            //heuristicOrder = getSubtermOrder(terms());
//            //shuffleOrder = heuristicOrder.clone();
//        }
//
//        private int[] getDependencyOrder(Compound seed) {
//            int ss = seed.size();
//
//
//            IntArrayList li = new IntArrayList();
//            List<Term> l = Global.newArrayList();
//            VariableDependencies d = new VariableDependencies(seed, Op.VAR_PATTERN);
//            Iterator ii = d.iterator();
//            while (ii.hasNext() && l.size() < ss) {
//                Term y = (Term) ii.next();
//                int yi = seed.indexOf(y);
//                if (yi!=-1) {
//                    l.add(y);
//                    li.add(yi);
//                }
//            }
//            if (li.size()!=ss) {
//                throw new RuntimeException("dependency fault");
//            }
//
//            //System.out.println(seed + " :: " + l + " " + li);
//            return li.toArray();
//        }
//
//        /** subterm match priority heuristic of
//         *  the amount of predicted effort or specificty
//         *  of matching a subterm
//         *  (lower is earlier) */
//        public static int subtermPriority(Term x) {
////            boolean isEllipsis = x instanceof Ellipsis;
////            boolean hasEllipsis =
////                    (x instanceof Compound) ?
////                        Ellipsis.containsEllipsis((Compound)x) : false;
//
//            int s = x.volume() + 1;
//
////            if (isEllipsis)
////                s += 200;
////            if (hasEllipsis)
////                s += 150;
//
//            if (x.isCommutative())
//                s *= s;
//
//            return s;
//        }
//
//        private static int[] getSubtermOrder(Term[] terms) {
//            Integer[] x = new Integer[terms.length];
//            for (int i = 0; i < terms.length; i++)
//                x[i] = i;
//            Arrays.sort(x,  (Integer a, Integer b) -> Integer.compare(
//                subtermPriority(terms[a]),
//                subtermPriority(terms[b])
//            ));
//            int[] y = new int[terms.length];
//            for (int i = 0; i < terms.length; i++) {
//                y[i] = x[i];
//            }
//            return y;
//        }
//
//        @Override
//        public boolean match(Compound y, FindSubst subst) {
//            if (!prematch(y)) return false;
//
//
//            if (!ellipsis) {
//                return matchLinear(y, subst);
//            } else {
//                return subst.matchCompoundWithEllipsis(this, y);
//            }
//
//        }
//
//
//        @Override
//        public boolean matchLinear(TermContainer y, FindSubst subst) {
//
//            //int[] o = this.heuristicOrder;
//            /*int[] o =
//                    //shuffle(shuffleOrder, subst.random);
//                    shuffle(shuffleOrder, subst.random);*/
//
//            //int[] o = dependencyOrder;
//
//            Term[] x = termsCached;
//            for (int i = 0; i < x.length; i++) {
//                //i = o[i]; //remap to the specific sequence
//                if (!subst.match(x[i], y.term(i)))
//                    return false;
//            }
//            return true;
//        }
//
//        static int[] shuffle(int[] shuffleOrder, Random random) {
//            nars.util.data.array.Arrays.shuffle(
//                shuffleOrder,
//                random
//            );
//            return shuffleOrder;
//        }
//
//    }
//
//    /** commutive simple compound which can match subterms in any order, but this order is prearranged optimally */
//    static final class CommutiveCompoundPattern extends AbstractCompoundPattern {
//
//        public CommutiveCompoundPattern(Compound seed, TermVector subterms) {
//            super(seed, subterms );
//        }
//
//        @Override
//        public boolean match(Compound y, FindSubst subst) {
//            if (!prematch(y))
//                return false;
//
//            if (!ellipsis) {
//                return subst.matchPermute(this, y);
//            } else {
//                return subst.matchCompoundWithEllipsis(this, y);
//            }
//
//        }
//
//    }
//

}
