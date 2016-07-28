package nars.term.obj;


import nars.$;
import nars.NAR;
import nars.Op;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Term;
import nars.term.Termlike;
import nars.term.Terms;
import nars.term.atom.Atomic;
import nars.term.subst.FindSubst;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public interface Termject<X> extends Atomic {

    /** the associated data value of this term */
    X val();

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
    abstract boolean match(Term y, FindSubst f);

    abstract class PrimTermject<X> implements Termject<X> {

        final X val;

        public PrimTermject(X val) {
            this.val = val;
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
            return (obj instanceof Termject) && val().equals(((PrimTermject)obj).val());
        }

        @Override
        public int hashCode() {
            return val().hashCode();
        }


    }

    public static class IntTerm extends PrimTermject<Integer> {

        public IntTerm(Integer val) {
            super(val);
        }



        @Override
        public Class type() {
            return Integer.class;
        }

        @Override
        public boolean match(Term y, FindSubst f) {
            y = y.term(); //make sure to fully resolve

            if (y instanceof Termject.IntPred) {
                return ((IntPred)y).match(this, f, true); //reverse x,y necessary?
            }
            return false;
        }
    }

    abstract static class IntPred extends PrimTermject<Predicate<Integer>> {

        public IntPred(Predicate<Integer> val) {
            super(val);
        }

        @Override
        public Class type() {
            return Predicate.class;
        }

        @Override
        public boolean match(Term y, FindSubst f) {
            return match(y, f, false);
        }

        public boolean match(Term y, FindSubst f, boolean reverse) {
            y = y.term(); //make sure to fully resolve

            if (y instanceof Termject) {
                if (y instanceof IntTerm) {
                    int i = ((IntTerm) y).val;
                    //return f.putXY($.negIf(this, !val.test(i)), y);
                    //@Nullable Term yy = $.negIf(y, !val.test(i));
                    if (!val.test(i)) {
                        return false; //TODO use a negate method which perfectly inverts the boolean condition of the predicate, not using (--,..)
                    }

                    if (!reverse)
                        return f.matchVarX(this, y);
                    else
                        return f.matchVarY(this, y);

                } else if (y instanceof Termject.IntPred) {
                    return match((IntPred) y, f);
                }
            }
            return false;
        }


        abstract public boolean match(IntPred y, FindSubst f);


        /** inclusive */
        public static class IntInterval extends IntPred {

            private final int a;
            private final int b;

            public IntInterval(int a, int b) {
                super((i) -> (i >=a && i <= b));
                this.a = a;
                this.b = b;
            }

            @Override
            public boolean equals(Object obj) {
                return super.equals(obj);
            }

            @Override
            public String toString() {
                return "`" + a + "<=?<="+ b + "`";
            }

            @Override
            public boolean match(IntPred y, FindSubst f) {


                if (y instanceof IntInterval) {
                    //union the intervals, create a new IntPred (ie, common variable)
                    IntInterval ii = (IntInterval)y;
                    int ca = Math.min(a, ii.a);
                    int cb = Math.max(b, ii.b);

                    IntPred union;
                    if ((ca == a) && (cb == b)) {
                        union = this;
                    } else  {
                        union = new IntInterval(ca, cb);
                    }
                    return f.putBidi(this, y, union);
                }
                return false;
            }
        }
    }

    public static void main(String[] args) {
        NAR n = new Default();
        n.log();
        for (int i = 1 ;i < 10; i++)
            n.believe($.sim(new IntTerm(i-1), new IntTerm(i)), 1f, 0.9f);
        //n.believe($.sim(new IntTerm(4), $.the("x")), 1f, 0.9f);
        n.believe($.inh(new IntPred.IntInterval(0,5), $.the("small")), 1f, 0.9f);
        n.believe($.inh(new IntPred.IntInterval(5,10), $.the("large")), 1f, 0.9f);
        n.ask($.inh($.varDep(1), $.the("large")));
        n.ask($.inh(new IntTerm(1), $.the("small")));
        n.ask($.inh(new IntTerm(1), $.the("large")));
        //n.ask($.sim(new IntPred.IntInterval(0,15), $.the("x")));
        n.run(1000);
    }
}
