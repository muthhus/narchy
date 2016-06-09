package nars.nal.nal1;

import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.nar.util.Answerer;
import nars.nar.util.OperationAnswerer;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static nars.nal.Tense.ETERNAL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
                .step()
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

        nar.ask((Compound)$.$("add(1, 2, #x)"));

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

        nar.ask((Compound)$.$("add(1, 2, #x)"));


    }
}
