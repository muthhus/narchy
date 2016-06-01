package nars.nal.nal1;

import nars.*;
import nars.nal.meta.PatternCompound;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.subst.OneMatchFindSubst;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static nars.nal.Tense.ETERNAL;
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

    public static class Answerer implements Consumer<Task> {

        private final Compound pattern;
        final OneMatchFindSubst match; //re-using this is not thread-safe

        public Answerer(Compound pattern, NAR n) {
            this.match = new OneMatchFindSubst(n);
            this.pattern = pattern;
            n.eventTaskProcess.on(this);
        }

        @Override
        public void accept(Task task) {
            if (task.punc() == Symbols.QUESTION) {
                if (match.tryMatch(Op.VAR_PATTERN, pattern, task.term())) {
                    System.out.println(match.xy);

                }
            }
        }
    }

    @Test public void testQuestionHandler() {
        NAR nar = new Terminal();
        //new Answerer($.$("add(%1, %2, #x)"), nar);

        Compound c = (Compound) $.$("add(%1, %2, #x)");

        new Answerer(
                c,
                nar);
        nar.ask($.$("add(1, 2, #x)"));

    }

}
