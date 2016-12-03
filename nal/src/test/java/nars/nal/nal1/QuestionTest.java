package nars.nal.nal1;

import nars.*;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.nar.util.Answerer;
import nars.nar.util.OperationAnswerer;
import nars.term.Compound;
import nars.term.Term;
import nars.term.obj.IntTerm;
import nars.test.DeductiveMeshTest;
import nars.test.TestNAR;
import nars.util.TaskStatistics;
import org.jetbrains.annotations.NotNull;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Arrays;
import java.util.DoubleSummaryStatistics;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.*;

/**
 * Created by me on 5/24/16.
 */
public class QuestionTest {

    final int withinCycles = 16;

    @Test
    public void whQuestionUnifyQueryVar() throws Narsese.NarseseException {
        testQuestionAnswer(withinCycles, "<bird --> swimmer>", "<?x --> swimmer>", "<bird --> swimmer>");
    }

    @Test
    public void yesNoQuestion() throws Narsese.NarseseException {
        testQuestionAnswer(withinCycles, "<bird --> swimmer>", "<bird --> swimmer>", "<bird --> swimmer>");
    }

    /** question to answer matching */
    public void testQuestionAnswer(int cycles, @NotNull String belief, @NotNull String question, @NotNull String expectedSolution) {
        AtomicBoolean ok = new AtomicBoolean(false);


        Term expectedSolutionTerm = $.$(expectedSolution);

        NAR nar = new Default();
        nar.nal(1);
        //nar.log();

        nar
                .believe(belief, 1.0f, 0.9f)
                .next()
                .ask(question, ETERNAL, b -> {
                    if (b.punc() == '.' && b.term().equals(expectedSolutionTerm))
                        ok.set(true);
                    return false;
                });

        nar.run(cycles);

        assertTrue(ok.get());

//           .onAnswer(question, a -> { //.en("What is a type of swimmer?")
//
//                System.out.println(nar.time() + ": " + question + " " + a);
//                //test for a few task conditions, everything except for evidence
//                if (a.punc() == expectedTask.punc())
//                    if (a.term().equals(expectedTask.term())) {
//                        if (Objects.equals(a.truth(), expectedTask.truth()))
//                            solved.set(true);
//                }
//
//            }).run(cycles);


    }


    @Test public void testQuestionHandler() {
        NAR nar = new Terminal();

        final int[] s = {0};
        new Answerer( $.$("add(%1, %2, #x)"), nar) {

            @Override
            protected void onMatch(Map<Term, Term> xy) {
                s[0] = xy.size();
            }
        };

        nar.ask($.$("add(1, 2, #x)"));

        assertEquals(3, s[0]);

    }

    @Test public void testOperationHandler() {
        NAR nar = new Terminal();

        final int[] s = {0};
        new OperationAnswerer( $.$("add(%1, %2, #x)"), nar) {

            @Override
            protected void onMatch(Term[] args) {
                System.out.println(Arrays.toString(args));
            }
        };

        nar.ask($.$("add(1, 2, #x)"));


    }

    /** tests whether the use of a question guides inference as measured by the speed to reach a specific conclusion */
    @Test public void questionDrivesInference() {

        final int[] dims = {4, 3};
        final int timelimit = 23000;

        TaskStatistics withTasks = new TaskStatistics();
        TaskStatistics withoutTasks = new TaskStatistics();
        DoubleSummaryStatistics withTime = new DoubleSummaryStatistics();
        DoubleSummaryStatistics withOutTime = new DoubleSummaryStatistics();

        IntFunction<NAR> narProvider = (seed) -> {
            NAR d = new Default(256, 1, 1, 1);
            d.random.setSeed(seed);
            d.nal(4);
            return d;
        };

        BiFunction<Integer,Integer,TestNAR> testProvider = (seed, variation) -> {
            NAR n = narProvider.apply(seed);
            TestNAR t = new TestNAR(n);
            switch (variation) {
                case 0:
                    new DeductiveMeshTest(t, dims, timelimit);
                    break;
                case 1:
                    new DeductiveMeshTest(t, dims, timelimit) {
                        @Override
                        public void ask(@NotNull TestNAR n, Compound term) {
                            //disabled
                        }
                    };
                    break;
            }
            return t;
        };

        for (int i = 0; i < 10; i++) {
            int seed = i + 1;

            {
                TestNAR withQuestion = testProvider.apply(seed, 0);
                withQuestion.run(true);
                withTime.accept(withQuestion.time());
                withTasks.add(withQuestion.nar);
            }

            {
                TestNAR withoutQuestion = testProvider.apply(seed, 1);
                withoutQuestion.run(true);
                withOutTime.accept(withoutQuestion.time());
                withoutTasks.add(withoutQuestion.nar);
            }
        }

        withTasks.print();
        withoutTasks.print();

        assertNotEquals(withTime, withOutTime);
        System.out.println("with: " + withTime);
        System.out.println("withOut: " + withOutTime);


//        assertTrue(withTime.getSum() < withOutTime.getSum());
//        assertTrue(withTime.getSum() < 2 * withOutTime.getSum()); //less than half, considering that a search "diameter" becomes a "radius" by providing the answer end-point
    }


    @Test @Ignore
    public void testMathBackchain() {
        NAR n = new Default();
        n.log();

        Param.DEBUG = true;

        n.on("odd", a->{
            if (a.length == 1 && a[0].op()== Op.INT) {
                return ((IntTerm)a[0]).val() % 2 == 0 ? Term.False : Term.True;
            }
            return null; //$.f("odd", a[0]); //vars, etc.
        });
        n.termVolumeMax.set(24);
        n.input(
            "({1,2,3,4} --> number).",
            "((({#x} --> number) && odd(#x)) ==> ({#x} --> ODD)).",
            "((({#x} --> number) && --odd(#x)) ==> ({#x} --> EVEN)).",
            "({#x} --> ODD)?",
            "({#x} --> EVEN)?"
//            "(1 --> ODD)?",
//            "(1 --> EVEN)?",
//            "(2 --> ODD)?",
//            "(2 --> EVEN)?"
        );
        n.run(2500);

    }

//    @Test public void testSaneBudgeting() {
//        Param.DEBUG = true;
//        String c = "((parent($X,$Y) && parent($Y,$Z)) ==> grandparent($X,$Z))";
//        new Default(1000, 8, 1, 3)
//            .logSummaryGT(System.out, 0.1f)
//            .eachFrame(nn->{
//                Concept cc = nn.concept(c);
//                if (cc!=null) {
//                    cc.print(System.out, false, false, true, false);
//                }
//            })
//            .input(c + ".", "")
//            .run(100);
//
//    }

//    @Test public void testPrologLike1() {
//        Param.DEBUG = true;
//        new Default(1000, 8, 1, 3)
//            .logSummaryGT(System.out, 0.1f)
//            .input(
//                "((parent($X,$Y) && parent($Y,$Z)) ==> grandparent($X,$Z)).",
//                "parent(c, p).",
//                "parent(p, g).",
//                "grandparent(p, #g)?"
//            )
//            .run(800);
//
//    }
}
