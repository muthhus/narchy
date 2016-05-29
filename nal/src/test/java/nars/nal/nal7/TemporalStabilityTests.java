package nars.nal.nal7;

import com.gs.collections.api.block.function.primitive.IntToObjectFunction;
import com.gs.collections.impl.set.mutable.primitive.IntHashSet;
import nars.NAR;
import nars.nar.Default;
import org.junit.Test;


/**
 * N independent events
 */
public class TemporalStabilityTests {

    static class T1 extends TemporalStabilityTest {

        private final IntHashSet whens;
        private final IntToObjectFunction<String> eventer;

        public T1(IntToObjectFunction<String> eventer, int... whens) {
            this.whens = new IntHashSet(whens);
            this.eventer = eventer;
        }

        @Override
        public boolean validOccurrence(long o) {
            return whens.contains((int)o);
        }

        @Override
        public void input(NAR n) {
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
        new T1(linkedinh, 1, 2, 5).test(400, new Default(1024, 8, 4, 3));
    }
    @Test public void testTemporalStabilityLinkedImpl() {
        new T1(linkedimpl, 1, 2, 5).test(400, new Default(1024, 12, 4, 3));
    }
    @Test public void testTemporalStabilityLinkedImplExt() {
        new T1(linkedimpl, 1, 2, 5, 10).test(400, new Default(1024, 12, 4, 3));
    }

}
