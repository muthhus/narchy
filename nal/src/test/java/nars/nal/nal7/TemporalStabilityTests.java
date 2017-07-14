package nars.nal.nal7;

import nars.NAR;
import nars.Narsese;
import nars.Param;
import nars.nar.NARS;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;


/**
 * N independent events
 */
public class TemporalStabilityTests {

    static {
        Param.DEBUG = false;
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
        public void input(@NotNull NAR n) throws Narsese.NarseseException {
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
        return "(" + c + " &&+5 " + d + ")";
    };

    @Test public void testTemporalStabilityInh3() throws Narsese.NarseseException {
        new T1(inheritencer, 1, 2, 5).test(200, new NARS().get());
    }

    @Test public void testTemporalStabilityImpl() throws Narsese.NarseseException {
        new T1(implicator, 1, 2, 5).test(200, new NARS().get());
    }
    @Test public void testTemporalStabilityProd() throws Narsese.NarseseException {
        new T1(productor, 1, 2, 5).test(200, new NARS().get());
    }
    @Test public void testTemporalStabilityBiProd() throws Narsese.NarseseException {
        new T1(biproductor, 1, 2, 5).test(200, new NARS().get());
    }
    @Test public void testTemporalStabilityLinkedProd() throws Narsese.NarseseException {
        new T1(linkedproductor, 1, 2, 5).test(200, new NARS().get());
    }

    @Test public void testTemporalStabilityLinkedInh() throws Narsese.NarseseException {
        new T1(linkedinh, 1, 2, 5).test(200, new NARS().get());
    }
    @Test public void testTemporalStabilityLinkedImpl() throws Narsese.NarseseException {
        new T1(linkedimpl, 1, 2, 5).test(200, new NARS().get());
    }
    @Test public void testTemporalStabilityLinkedTemporalConj() throws Narsese.NarseseException {
        new T1(linkedTempConj, 1, 2, 5).test(200, new NARS().get());
    }
    @Test public void testTemporalStabilityLinkedImplExt() throws Narsese.NarseseException {
        new T1(linkedimpl, 1, 2, 5).test(200, new NARS().get());
    }
    @Test public void testTemporalStabilityLinkedImplExt2() throws Narsese.NarseseException {

        //Param.DEBUG = true;

        @NotNull NAR n = new NARS().get();
        int time = 80;
        T1 a = new T1(linkedimpl, 1, 2, 5, 10);
        T1 b = new T1(linkedinh, 1, 2, 5, 10);

        a.test(-1, n);
        b.test(-1, n);

        n.run(time);

        a.evaluate(n);
        b.evaluate(n);

    }

}

