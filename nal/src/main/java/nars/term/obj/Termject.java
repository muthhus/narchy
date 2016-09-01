package nars.term.obj;


import com.google.common.collect.BoundType;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.nar.Default;
import nars.term.Term;
import nars.term.atom.AtomicString;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import static nars.Op.INT;

public interface Termject<X> extends Term {

    /**
     * the associated data value of this term
     */
    X val();

    int compareVal(X v);

    /**
     * the native type of the value
     */
    Class<? super X> type();

    @Override
    default int complexity() {
        return 1;
    }

//    @Override
//    default int varIndep() {
//        return 0;
//    }
//
//    @Override
//    default int varDep() {
//        return 0;
//    }
//
//    @Override
//    default int varQuery() {
//        return 0;
//    }
//
//    @Override
//    default int varPattern() {
//        return 0;
//    }


    abstract class PrimTermject<X> extends AtomicString implements Termject<X> {

        @NotNull
        public final X val;

        public PrimTermject(@NotNull X val) {
            this.val = val;
        }

        @NotNull
        @Override
        public X val() {
            return val;
        }


        @NotNull
        @Override
        public String toString() {
            return '`' + val.toString() + '`'; //TODO escape any '`' which appear in the string
        }


        @Override
        public int varIndep() {
            return 0;
        }

        @Override
        public int varDep() {
            return 0;
        }

        @Override
        public int varQuery() {
            return 0;
        }

        @Override
        public int varPattern() {
            return 0;
        }
    }

    //    interface IntPred  {
//
//
////        default boolean match(IntPred x, Term y, FindSubst f) {
////            return match(x, y, f, false);
////        }
////
////        static boolean match(IntPred x, Term y, FindSubst f, boolean reverse) {
////            y = y.term(); //make sure to fully resolve
////
////            if (y instanceof Termject) {
////                if (y instanceof IntTerm) {
////                    int i = ((IntTerm) y).val;
////                    //return f.putXY($.negIf(this, !val.test(i)), y);
////                    //@Nullable Term yy = $.negIf(y, !val.test(i));
////                    if (!x.test(i)) {
////                        return false; //TODO use a negate method which perfectly inverts the boolean condition of the predicate, not using (--,..)
////                    }
////
////                    if (!reverse)
////                        return f.matchVarX(this, y);
////                    else
////                        return f.matchVarY(this, y);
////
////                } else if (y instanceof Termject.IntPred) {
////                    return match((IntPred) y, f);
////                }
////            }
////            return false;
////        }
//
//
//        //abstract public boolean match(IntPred y, FindSubst f);
//
//
//    }

    class IntInterval extends PrimTermject<Range<Integer>> {

        //extends PrimTermject<Range<Integer>> {


        public IntInterval(int a, int b) {
            super(Range.closed(
                    a, b
            ).canonical(DiscreteDomain.integers()));
            //this(Range.closed(a,b).canonical( DiscreteDomain.integers() ));
            //super(INTRANGE, TermVector.the(new IntTerm(a), new IntTerm(b-a)));

            //int a = ((IntTerm) (term(0))).val;
            //int ba = ((IntTerm) (term(1))).val;
        }


        //        public IntInterval(@NotNull Range<Integer> span) {
//
//            //super(span.canonical( DiscreteDomain.integers() ));
//        }


        @Override
        public @NotNull Op op() {
            return INT;
        }


//        @Override
//        public int complexity() {
//            return 0; //it's like a variable
//        }
//
//        @Override
//        public int varQuery() {
//            return 1; //pretend to be a query variable to allow unification
//        }

        @Override
        public int compareVal(@NotNull Range<Integer> v) {
            int l = val.lowerEndpoint();
            int vl = v.lowerEndpoint();
            int lc = Integer.compare(l, vl);
            if (lc == 0) {
                int u = val.upperEndpoint();
                int vu = v.upperEndpoint();
                return Integer.compare(u, vu);
            }
            return lc;
        }

        @NotNull
        @Override
        public Class<? super Range<Integer>> type() {
            return Range.class;
        }


        @Override
        public void append(@NotNull Appendable p) throws IOException {
            p.append(toString());
        }

        @NotNull
        @Override
        public String toString() {
            int u = max();
            int l = min();
            return "`" + l + "<=?<=" + u + '`';
        }

        public int min() {
            int l = val.lowerEndpoint();
            if (val.lowerBoundType() == BoundType.OPEN) {
                l++;
            }
            return l;
        }

        public int max() {
            int u = val.upperEndpoint();
            if (val.upperBoundType() == BoundType.OPEN) {
                u--;
            }
            return u;
        }


        @Override
        public boolean unify(Term y, FindSubst f) {

            //if (y.op() == INT) {
                if (y instanceof IntTerm) {
                    int yi = ((IntTerm) y).val;
                    if (val.contains(yi)) {
                        //return f.matchVarX(this, y);
                        return true;
                    }
                } else if (y instanceof IntInterval) {
                    Range<Integer> yr = ((IntInterval) y).val;
                    //if (val.isConnected(yr)) {
//                    Range<Integer> combinedRange = val.span(yr);
//                    IntInterval combined;
//                    if (!combinedRange.equals(val)) {
//                        combined = new IntInterval(combinedRange);
//                    } else {
//                        combined = this;
//                    }
//                    return f.putBidi(this, y, combined);
                    return (val.encloses(yr) || yr.encloses(val));
                    //}

                }
            //}
            return super.unify(y, f);
        }


//            @Override
//            public boolean match(IntPred y, FindSubst f) {
//
////                if (y instanceof IntInterval) {
////                    //union the intervals, create a new IntPred (ie, common variable)
////                    IntInterval ii = (IntInterval)y;
////
////                    if (range.isConnected(ii.range)) {
////                        Range<Integer> span = range.span(ii.range);
////                        return f.putBidi(this, y, new IntInterval(span));
////                    }
////
////                }
//                return false;
//            }

    }

    static void main(String[] args) {
        NAR n = new Default();
        n.log();

//        Atom x = $.the("x");
//        Atom y = $.the("y");
//        n.believe($.conj($.inh(x, new IntTerm(2)), new IntInterval(0,4)), 1f, 0.9f);

        for (int i = 1; i < 10; i++)
            n.believe($.sim(new IntTerm(i - 1), new IntTerm(i)), 1f, 0.9f);
        //n.believe($.sim(new IntTerm(4), $.the("x")), 1f, 0.9f);
        n.believe($.sim(new IntInterval(0, 5), $.the("small")), 1f, 0.9f);
        n.believe($.sim(new IntInterval(5, 10), $.the("large")), 1f, 0.9f);
        n.ask($.inh($.varDep(1), $.the("large")));
        n.ask($.inh($.varDep(1), $.the("small")));
        n.ask($.inh(new IntTerm(1), $.the("small")));
        n.ask($.inh(new IntTerm(1), $.the("large")));
        n.ask($.sim(new IntInterval(0, 15), $.the("x")));
        n.run(1000);
    }
}
