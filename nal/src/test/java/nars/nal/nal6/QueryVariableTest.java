package nars.nal.nal6;

import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.nar.Default;
import nars.term.Termed;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.assertTrue;


public class QueryVariableTest {

    @Test public void testNoVariableAnswer() {
        testQuestionAnswer("<a --> b>", "<a --> b>");
    }
    @Test public void testQueryVariableAnswerUnified() {

        testQuestionAnswer("<a --> b>", "<?x --> b>");
    }
    @Test public void testQueryVariableAnswerUnified2() {

        testQuestionAnswer("<c --> (a&b)>", "<?x --> (a&b)>");
    }

    void testQuestionAnswer(@NotNull String beliefString, @NotNull String question) {

        int time = 32;

        AtomicBoolean valid = new AtomicBoolean();


        Default nar = new Default();
        nar.log();
        Termed beliefTerm = nar.term(beliefString);
        nar.believe(beliefTerm, 1f, 0.9f);
        nar.ask(question, Tense.ETERNAL, (q,a)-> {
            if (a.term().equals(beliefTerm)) {
                valid.set(true);
                q.delete();
            }
        });
        nar.run(time);
        assertTrue(valid.get());

    }

    @Test
    public void testQuery2() throws Narsese.NarseseException {
        testQueryAnswered(64, 1);
    }

    @Test
    public void testQuery1()  {
        testQueryAnswered(1, 32);
    }


    public void testQueryAnswered(int cyclesBeforeQuestion, int cyclesAfterQuestion) throws Narsese.NarseseException {

        AtomicBoolean b = new AtomicBoolean(false);

        String question = cyclesBeforeQuestion == 0 ?
                "<a --> b>" /* unknown solution to be derived */ :
                "<b --> a>" /* existing solution, to test finding existing solutions */;


        NAR n = new Default(100, 1, 1, 3);
        n.nal(2);
        n.log()
                .input("<a <-> b>. %1.0;0.5%",
                        "<b --> a>. %1.0;0.5%")
                .run(cyclesBeforeQuestion)
                .stopIf(b::get)
                .ask(question, ETERNAL, (q, a) -> {
                    b.set(true);
                });
        n.run(cyclesAfterQuestion);

        assertTrue(b.get());

    }


//    /** simple test for solutions to query variable questions */
//    @Test public void testQueryVariableSolution() throws InvalidInputException {
//
//        Global.DEBUG = true;
//
//        /*
//        int time1 = 5;
//        int time2 = 15;
//        int time3 = 5;
//        */
//
//        int time1 = 55;
//        int time2 = 115;
//        int time3 = 115;
//
//        //TextOutput.out(n);
//        //new TraceWriter(n, System.out);
//
//        NAR nar = nar();
//
//        nar.frame(time1);
//        String term = "<a --> b>";
//        nar.believe(term);
//        nar.frame(time2);
//
//        //should not output 0.81
//        nar.memory.eventDerived.on( d-> {
//            if (d.isJudgment() && d.getTerm().toString().equals(term)) {
//                assertFalse(d + " should not have been derived", Util.isEqual(d.getConfidence(), 0.81f, 0.01f));
//            }
//        } );
//
//        nar.ask("<?x --> b>");
//
//        nar.frame(time3);
//    }

}

