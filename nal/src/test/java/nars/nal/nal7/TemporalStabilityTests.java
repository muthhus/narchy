package nars.nal.nal7;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import org.eclipse.collections.api.block.function.primitive.IntToObjectFunction;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;


/**
 * N independent events
 */
public class TemporalStabilityTests {

    static final int CYCLES = 2500;

//    static {
//        Param.DEBUG = true;
//    }

    static class T1 extends TemporalStabilityTest {

        @NotNull
        private final IntHashSet whens;
        private final IntToObjectFunction<String> eventer;
        private final int minT, maxT;
        final int tolerance = 0;

        public T1(IntToObjectFunction<String> eventer, int... whens) {
            this.whens = new IntHashSet(whens);
            minT = this.whens.min();
            maxT = this.whens.max();
            this.eventer = eventer;
        }
        public T1(IntToObjectFunction<String> eventer, int[] whens, int minT, int maxT) {
            this.whens = new IntHashSet(whens);
            this.minT = minT;
            this.maxT = maxT;
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
    static final IntToObjectFunction<String> conjSeq2 = (j) -> {
        char c = (char) ('a' + j);
        char d = (char) ('a' + (j+1)); //next
        return "(" + c + " &&+5 " + d + ")";
    };
    static final IntToObjectFunction<String> conjInvertor = (j) -> {
        char c = (char) ('a' + j);
        return "(" + c + " &&+5 (--," + c + "))";
    };


    @Test public void testTemporalStabilityInh3() throws Narsese.NarseseException {
        new T1(inheritencer, 1, 2, 5).test(CYCLES, NARS.tmp());
    }

    @Test public void testTemporalStabilityImpl() throws Narsese.NarseseException {
        new T1(implicator, 1, 2, 5).test(CYCLES, NARS.tmp());
    }
    @Test public void testTemporalStabilityProd() throws Narsese.NarseseException {
        new T1(productor, 1, 2, 5).test(CYCLES, NARS.tmp());
    }
    @Test public void testTemporalStabilityBiProd() throws Narsese.NarseseException {
        new T1(biproductor, 1, 2, 5).test(CYCLES, NARS.tmp());
    }
    @Test public void testTemporalStabilityLinkedProd() throws Narsese.NarseseException {
        new T1(linkedproductor, 1, 2, 5).test(CYCLES, NARS.tmp());
    }

    @Test public void testTemporalStabilityLinkedInh() throws Narsese.NarseseException {
        new T1(linkedinh, new int[] { 1, 2, 5 }).test(CYCLES, NARS.tmp());
    }
    @Test public void testTemporalStabilityLinkedImpl() throws Narsese.NarseseException {
        new T1(linkedimpl, 1, 2, 5).test(CYCLES, NARS.tmp());
    }
    @Test public void testTemporalStabilityLinkedTemporalConj() throws Narsese.NarseseException {
        new T1(conjSeq2, new int[] { 1, 6, 11 }, 1, 16).test(CYCLES, NARS.tmp());
    }
    @Test public void testTemporalStabilityConjInvertor() throws Narsese.NarseseException {
        new T1(conjInvertor, new int[] { 1, 6, 11 }, 1, 16).test(CYCLES, NARS.tmp());
    }
    @Test public void testTemporalStabilityLinkedImplExt() throws Narsese.NarseseException {
        new T1(linkedimpl, 1, 2, 5).test(CYCLES, NARS.tmp());
    }
    @Test public void testTemporalStabilityLinkedImplExt2() throws Narsese.NarseseException {

        //Param.DEBUG = true;

        @NotNull NAR n = NARS.tmp();
        int time = CYCLES;
        T1 a = new T1(linkedimpl, 1, 2, 5, 10);
        T1 b = new T1(linkedinh, 1, 2, 5, 10);

        a.test(-1, n);
        b.test(-1, n);

        n.run(time);

//        a.evaluate(n);
//        b.evaluate(n);

    }

}

