package nars.term.obj;


import com.google.common.collect.BoundType;
import com.google.common.collect.DiscreteDomain;
import com.google.common.collect.Range;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.nar.Default;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;

public interface Termject<X> extends Atomic {

    /** the associated data value of this term */
    X val();

    int compareVal(X v);

    /** the native type of the value */
    Class<? super X> type();




    @Override
    default @NotNull Op op() {
        return Op.OBJECT;
    }

    @Override
    default int complexity() {
        return 1;
    }

    @Override
    default int varIndep() {
        return 0;
    }

    @Override
    default int varDep() {
        return 0;
    }

    @Override
    default int varQuery() {
        return 0;
    }

    @Override
    default int varPattern() {
        return 0;
    }

    /** when called, equality to this term has already been tested */
    boolean match(Term y, FindSubst f);

    abstract class PrimTermject<X> implements Termject<X> {

        final X val;
        private final int _hash;

        public PrimTermject(X val) {

            this.val = val;
            this._hash = val.hashCode();
        }

        @Override
        public X val() {
            return val;
        }

        @Override
        public String toString() {
            return '`' + val.toString() + '`'; //TODO escape any '`' which appear in the string
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            return (obj instanceof Termject) && val.equals(((Termject)obj).val());
        }

        @Override
        public final int hashCode() {
            //return val.hashCode();
            return _hash;
        }


    }

    class IntTerm extends PrimTermject<Integer> {

        public IntTerm(Integer val) {
            super(val);
        }


        @Override
        public int compareVal(Integer v) {
            return Integer.compare(val(), v);
        }

        @Override
        public Class type() {
            return Integer.class;
        }

        @Override
        public boolean match(Term y, FindSubst f) {
//            y = y.term(); //make sure to fully resolve
//
//            if (y instanceof Termject.IntPred) {
//                return ((IntPred)y).match(this, f, true); //reverse x,y necessary?
//            }
            return false;
        }
    }

    interface IntPred  {


//        default boolean match(IntPred x, Term y, FindSubst f) {
//            return match(x, y, f, false);
//        }
//
//        static boolean match(IntPred x, Term y, FindSubst f, boolean reverse) {
//            y = y.term(); //make sure to fully resolve
//
//            if (y instanceof Termject) {
//                if (y instanceof IntTerm) {
//                    int i = ((IntTerm) y).val;
//                    //return f.putXY($.negIf(this, !val.test(i)), y);
//                    //@Nullable Term yy = $.negIf(y, !val.test(i));
//                    if (!x.test(i)) {
//                        return false; //TODO use a negate method which perfectly inverts the boolean condition of the predicate, not using (--,..)
//                    }
//
//                    if (!reverse)
//                        return f.matchVarX(this, y);
//                    else
//                        return f.matchVarY(this, y);
//
//                } else if (y instanceof Termject.IntPred) {
//                    return match((IntPred) y, f);
//                }
//            }
//            return false;
//        }


        //abstract public boolean match(IntPred y, FindSubst f);


    }

    class IntInterval extends PrimTermject<Range<Integer>> {


        public IntInterval(int a, int b) {
            this(Range.closed(a,b).canonical( DiscreteDomain.integers() ));
        }

        public IntInterval(Range<Integer> span) {
            super(span.canonical( DiscreteDomain.integers() ));
        }

        @Override
        public int complexity() {
            return 0; //it's like a variable
        }

        @Override
        public int varQuery() {
            return 1; //pretend to be a query variable to allow unification
        }

        @Override
        public int compareVal(Range<Integer> v) {
            int l = val().lowerEndpoint();
            int vl = v.lowerEndpoint();
            int lc = Integer.compare(l, vl);
            if (lc == 0) {
                int u = val().upperEndpoint();
                int vu = v.upperEndpoint();
                return Integer.compare(u, vu);
            }
            return lc;
        }

        @Override
        public Class<? super Range<Integer>> type() {
            return Range.class;
        }

        @Override
        public String toString() {
            Range<Integer> r = val();
            int l = r.lowerEndpoint();
            int u = r.upperEndpoint();
            if (r.upperBoundType()== BoundType.OPEN) {
                u--;
            }
            if (r.lowerBoundType()== BoundType.OPEN) {
                l++;
            }
            return "`" + l + "<=?<=" + u + "`";
        }

        @Override
        public boolean match(Term y, FindSubst f) {
            if (y instanceof IntTerm) {
                int yi = ((IntTerm)y).val;
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
            return false;
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

        for (int i = 1 ;i < 10; i++)
            n.believe($.sim(new IntTerm(i-1), new IntTerm(i)), 1f, 0.9f);
        //n.believe($.sim(new IntTerm(4), $.the("x")), 1f, 0.9f);
        n.believe($.sim(new IntInterval(0,5), $.the("small")), 1f, 0.9f);
        n.believe($.sim(new IntInterval(5,10), $.the("large")), 1f, 0.9f);
        n.ask($.inh($.varDep(1), $.the("large")));
        n.ask($.inh($.varDep(1), $.the("small")));
        n.ask($.inh(new IntTerm(1), $.the("small")));
        n.ask($.inh(new IntTerm(1), $.the("large")));
        n.ask($.sim(new IntInterval(0,15), $.the("x")));
        n.run(1000);
    }
}
