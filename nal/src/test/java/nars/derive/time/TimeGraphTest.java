package nars.derive.time;

import astar.model.TimeProblem;
import com.google.common.collect.Sets;
import jcog.math.random.XoRoShiRo128PlusRandom;
import nars.$;
import nars.Narsese;
import nars.term.Term;
import nars.time.Tense;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static astar.model.TimeProblem.ETERNAL;
import static nars.Op.BELIEF;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeGraphTest {

    /**
     * example time graphs
     */
    final TimeGraph A; {
        A = newTimeGraph(1);
        A.know($.$safe("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
        A.know($.$safe("one"), 1);
        A.know($.$safe("two"), 20);
    }

//    @BeforeEach
//    void init() {
//    }

    @Test
    public void testAtomEvent() throws Narsese.NarseseException {
        assertSolved(A, "one", "one@1");
    }

    @Test
    public void testSimpleConjWithOneKnownAbsoluteSubEvent1() throws Narsese.NarseseException {

        int nodesBefore = A.nodes().size();
        long edgesBefore = A.edges().count();

        assertSolved(A,
                "(one &&+1 two)",
                "(one &&+1 two)@1", "(one &&+1 two)@19");

        int nodesAfter = A.nodes().size();
        long edgesAfter = A.edges().count();
        assertEquals(nodesBefore, nodesAfter, "# of nodes changed as a result of solving");
        assertEquals(edgesBefore, edgesAfter, "# of edges changed as a result of solving");
    }

    @Test
    public void testSimpleConjWithOneKnownAbsoluteSubEvent2() throws Narsese.NarseseException {
        assertSolved(A,
                "(one &&+- two)",
                "(one &&+1 two)@1", "(one &&+1 two)@19");
    }

    @Test
    public void testSimpleConjOfTermsAcrossImpl1() throws Narsese.NarseseException {

        assertSolved(A, "(two &&+1 three)",
                "(two &&+1 three)@20");
    }
    @Test
    public void testSimpleConjOfTermsAcrossImpl2() throws Narsese.NarseseException {
        assertSolved(A, "(two &&+- three)",
                "(two &&+1 three)@20");
    }

    @Test
    public void testSimpleImplWithOneKnownAbsoluteSubEvent() throws Narsese.NarseseException {
        assertSolved(A, "(one ==>+- three)",
                "(one ==>+2 three)@1");
    }

    @Test public void testImplChain1() throws Narsese.NarseseException {
//               .believe("(y ==>+3 x)")
//                .believe("(y ==>+2 z)")
//                .mustBelieve(cycles, "(z ==>+1 x)", 1.00f, 0.45f)
//                .mustNotOutput(cycles, "(z ==>-1 x)", BELIEF, Tense.ETERNAL)
//                .mustBelieve(cycles, "(x ==>-1 z)", 1.00f, 0.45f)
//                .mustNotOutput(cycles, "(x ==>+1 z)", BELIEF, Tense.ETERNAL)
        TimeGraph b = newTimeGraph(1);
        b.know($.$("(y ==>+3 x)"), ETERNAL);
        b.know($.$("(y ==>+2 z)"), ETERNAL);
        b.print();
        assertSolved(b, "(z ==>+- x)", "(z ==>+1 x)@ETE");
        assertSolved(b, "(x ==>+- z)", "(x ==>-1 z)@ETE");
    }

    private static final String[] implWithConjPredicateSolutions = {
            //using one@1
            "(one ==>+1 (two &&+1 three))@ETE",
            //using two@20
            "(one ==>+1 (two &&+1 three))@19"
    };

    @Test
    public void testImplWithConjPredicate1() throws Narsese.NarseseException {
        assertSolved(A, "(one ==>+- (two &&+1 three))", implWithConjPredicateSolutions);
    }

    @Test
    public void testImplWithConjPredicate2() throws Narsese.NarseseException {
        assertSolved(A, "(one ==>+- (two &&+- three))", implWithConjPredicateSolutions);
    }

    final List<Runnable> afterEach = $.newArrayList();

    @AfterEach
    void test() {
        afterEach.forEach(Runnable::run);
    }

    void assertSolved(TimeGraph t, String inputTerm, String... solutions) {
        t.solve($.$safe(inputTerm), new ExpectSolutions(solutions));
    }

    private class ExpectSolutions extends TreeSet<String> implements Predicate<TimeProblem.Event<Term>> {

        final Supplier<String> errorMsg;
        private final String[] solutions;

        public ExpectSolutions(String... solutions) {
            this.solutions = solutions;
            errorMsg = () ->
                    "expect: " + Arrays.toString(solutions) + "\n   got: " + toString();

            afterEach.add(() -> {
                assertEquals(Sets.newTreeSet(List.of(solutions)), this, errorMsg);
                System.out.print(errorMsg.get());
            });
        }

        @Override
        public boolean test(TimeProblem.Event<Term> termEvent) {
            add(termEvent.toString());
            return true;
        }

    }

    static TimeGraph newTimeGraph(long seed) {
        XoRoShiRo128PlusRandom rng = new XoRoShiRo128PlusRandom(seed);
        return new TimeGraph() {
            @Override
            protected Random random() {
                return rng;
            }
        };
    }
}

//    public static void main(String[] args) throws Narsese.NarseseException {
//
//        TimeGraph t = new TimeGraph();
////        t.know($.$("(a ==>+1 b)"), ETERNAL);
////        t.know($.$("(b ==>+1 (c &&+1 d))"), 0);
////        t.know($.$("(a &&+1 b)"), 4);
//
//        t.know($.$("((one &&+1 two) ==>+1 (three &&+1 four))"), ETERNAL);
//        t.know($.$("one"), 1);
//        t.know($.$("two"), 20);
//
//        t.print();
//
//        System.out.println();
//
//        for (String s : List.of(
//                "one",
//                "(one &&+1 two)", "(one &&+- two)",
//                "(two &&+1 three)", "(two &&+- three)",
//                "(one ==>+- three)",
//                "(one ==>+- (two &&+1 three))",
//                "(one ==>+- (two &&+- three))"
//        )) {
//            Term x = $.$(s);
//            System.out.println("SOLVE: " + x);
//            t.solve(x, (y) -> {
//                System.out.println("\t" + y);
//                return true;
//            });
//        }
//
//
//    }