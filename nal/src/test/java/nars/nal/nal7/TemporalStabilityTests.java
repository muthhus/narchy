package nars.nal.nal7;

import nars.NAR;
import nars.Param;
import nars.nar.Default;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;


/**
 * N independent events
 */
public class TemporalStabilityTests {

    static {
        Param.DEBUG = true;
    }

    static class T1 extends TemporalStabilityTest {

        @NotNull
        private final IntHashSet whens;
        private final IntToObjectFunction<String> eventer;
        private final int minT, maxT;
        final int tolerance = 1;

        public T1(IntToObjectFunction<String> eventer, int... whens) {
            this.whens = new IntHashSet(whens);
            minT = this.whens.min();
            maxT = this.whens.max();
            this.eventer = eventer;
        }

        @Override
        public boolean validOccurrence(long o) {
            //return whens.contains((int)o);
            return (o >= minT-tolerance) && (o <= maxT+tolerance);
        }

        @Override
        public void input(@NotNull NAR n) {
            int j = 0;
            for (int i : whens.toSortedArray()) {
                n.inputAt(i, eventer.valueOf(j++) + ". :|:");
            }
        }

    }



//    @Override
//    public void input(NAR n) {
//
//        n
//        .inputAt(1, "a:b. :|:")
//        .inputAt(2, "b:c. :|:")
//        .inputAt(5, "c:d. :|:");
//    }

    static final IntToObjectFunction<String> inheritencer = (j) -> {
        char c = (char) ('a' + j);
        return c + ":" + c + "" + c;
    };
    static final IntToObjectFunction<String> implicator = (j) -> {
        char c = (char) ('a' + j);
        return "(" + (c + "==>" + (c + "" + c)) + ")";
    };
    static final IntToObjectFunction<String> productor = (j) -> {
        char c = (char) ('a' + j);
        return "(" + c + ")";
    };
    static final IntToObjectFunction<String> biproductor = (j) -> {
        char c = (char) ('a' + j);
        return "(" + c + "," + (c + "" + c) + ")";
    };
    static final IntToObjectFunction<String> linkedproductor = (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j+1)); //next
        return "(" + c + "," + d + ")";
    };
    static final IntToObjectFunction<String> linkedinh= (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j+1)); //next
        return "(" + c + "-->" + d + ")";
    };
    static final IntToObjectFunction<String> linkedimpl= (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j+1)); //next
        return "(" + c + "==>" + d + ")";
    };
    static final IntToObjectFunction<String> linkedTempConj = (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j+1)); //next
        return "(" + c + " &&+5" + ")";
    };

    @Test public void testTemporalStabilityInh3() {
        new T1(inheritencer, 1, 2, 5).test(300, new Default(1024, 8, 4, 3));
    }

    @Test public void testTemporalStabilityImpl() {
        new T1(implicator, 1, 2, 5).test(300, new Default(1024, 8, 4, 3));
    }
    @Test public void testTemporalStabilityProd() {
        new T1(productor, 1, 2, 5).test(300, new Default(1024, 8, 4, 3));
    }
    @Test public void testTemporalStabilityBiProd() {
        new T1(biproductor, 1, 2, 5).test(300, new Default(1024, 8, 4, 3));
    }
    @Test public void testTemporalStabilityLinkedProd() {
        new T1(linkedproductor, 1, 2, 5).test(300, new Default(1024, 8, 4, 3));
    }

    @Test public void testTemporalStabilityLinkedInh() {
        new T1(linkedinh, 1, 2, 5).test(400, new Default(1024, 8, 4, 3) );
    }
    @Test public void testTemporalStabilityLinkedImpl() {
        new T1(linkedimpl, 1, 2, 5).test(400, new Default(1024, 12, 4, 3));
    }
    @Test public void testTemporalStabilityLinkedTemporalConj() {
        new T1(linkedTempConj, 1, 2, 5).test(400, new Default(1024, 12, 4, 3));
    }
    @Test public void testTemporalStabilityLinkedImplExt() {
        new T1(linkedimpl, 1, 2, 5).test(400, new Default(1024, 12, 4, 3));
    }
    @Test public void testTemporalStabilityLinkedImplExt2() {
        @NotNull NAR n = new Default(1024, 32, 4, 3);
        int time = 400;
        T1 a = new T1(linkedimpl, 1, 2, 5, 10);
        T1 b = new T1(linkedinh, 1, 2, 5, 10);

        a.test(-1, n);
        b.test(-1, n);

        n.run(time);

        a.evaluate(n);
        b.evaluate(n);

    }

}

