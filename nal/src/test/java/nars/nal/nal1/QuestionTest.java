package nars.nal.nal1;

import com.gs.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import nars.*;
import nars.nal.meta.PatternCompound;
import nars.nar.Default;
import nars.nar.Terminal;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Operator;
import nars.term.Term;
import nars.term.subst.OneMatchFindSubst;
import nars.util.version.VersionMap;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

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

    abstract public static class Answerer implements Consumer<Task> {

        public final Compound pattern;
        private final NAR nar;

        public Answerer(Compound pattern, NAR n) {
            this.nar = n;
            this.pattern = pattern;
            n.eventTaskProcess.on(this);
        }

        @Override
        public void accept(Task task) {
            if (task.punc() == Symbols.QUESTION) {
                final OneMatchFindSubst match = new OneMatchFindSubst(nar); //re-using this is not thread-safe
                if (match.tryMatch(Op.VAR_PATTERN, pattern, task.term())) {
                    onMatch(match.xy);
                }
            }
        }

        abstract protected void onMatch(Map<Term,Term> xy);
    }
    abstract public static class OperationAnswerer extends Answerer {

        private final ObjectIntHashMap argIndex;
        private final int numArgs;

        public OperationAnswerer(Compound pattern, NAR n) {
            super(pattern, n);
            if (!Op.isOperation(pattern))
                throw new RuntimeException(pattern + " is not an operation compound pattern");

            this.argIndex = new ObjectIntHashMap<>();
            Compound args = Operator.opArgs(pattern);
            int i = 0;
            this.numArgs = args.size();
            for (Term t : args.terms()) {
                argIndex.put(t, i++);
            }


        }

        @Override
        protected final void onMatch(Map<Term, Term> xy) {
            Term[] args = new Term[numArgs];
            xy.forEach((k,v)-> {
                int i = argIndex.getIfAbsent(k, -1);
                if (i!=-1) {
                    args[i] = v;
                }
            });
            onMatch(args);
        }

        protected abstract void onMatch(Term[] args);
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
}
