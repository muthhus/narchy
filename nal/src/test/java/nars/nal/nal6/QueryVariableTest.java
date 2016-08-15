package nars.nal.nal6;

import nars.Task;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

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

        int time = 16;

        AtomicBoolean valid = new AtomicBoolean();


        Default nar = new Default();
        Termed beliefTerm = nar.term(beliefString);
        nar.believe(beliefTerm, 1f, 0.9f);
        nar.ask(question, Tense.ETERNAL, (Task a)-> {
            if (a.term().equals(beliefTerm)) {
                valid.set(true);
                return false;
            }
            return true;
        });
        nar.run(time);
        assertTrue(valid.get());

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

