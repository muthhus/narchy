package nars.test;

import jcog.event.Topic;
import nars.*;
import nars.task.ITask;
import nars.task.Tasked;
import nars.test.condition.EternalTaskCondition;
import nars.test.condition.NARCondition;
import nars.test.condition.TemporalTaskCondition;
import nars.time.Tense;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.StringWriter;
import java.util.List;
import java.util.function.Consumer;

import static java.lang.Float.NaN;
import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * TODO use a countdown latch to provide early termination for successful tests
 */
public class TestNAR {

//    static {
//        $.logger.hashCode(); //HACK trigger logging?
//    }

    static final Logger logger = LoggerFactory.getLogger(TestNAR.class);

    //    @NotNull
//    public final Map<Object, HitMeter> eventMeters;
    @NotNull
    public final NAR nar;

    //    boolean showFail = true;
//    boolean showSuccess;
//    boolean showExplanations;
    final static boolean showOutput = false;

    /**
     * holds must (positive) conditions
     */
    final List<NARCondition> requires = $.newArrayList();

    /**
     * holds mustNot (negative) conditions which are tested at the end
     */
    final List<NARCondition> disqualifies = $.newArrayList();

    //public final List<ExplainableTask> explanations = new ArrayList();
    @Nullable
    private Object result;
    private boolean exitOnAllSuccess = true;

    public final List<Task> inputs = $.newArrayList();
    private static final int temporalTolerance = 0;


    /**
     * enable this to print reports even if the test was successful.
     * it can cause a lot of output that can be noisy and slow down
     * the test running.
     * TODO separate way to generate a test report containing
     * both successful and unsuccessful tests
     */
    static final boolean collectTrace = false;

    boolean finished;
    private boolean requireConditions = true;

    public TestNAR(@NotNull NAR nar) {


        this.outputEvents = new Topic[]{
                //nar.memory.eventDerived,
                //nar.memory.eventInput,
                nar.eventTaskProcess,
                //nar.eventTaskRemoved,
                //nar.memory.eventRevision,
        };

        this.nar = nar;


        //eventMeters = new EventCount(nar).eventMeters;


    }

//    /** returns the "cost", which can be considered the inverse of a "score".
//     * it is proportional to the effort (ex: # of cycles) expended by
//     * this reasoner in attempts to satisfy success conditions.
//     * If the conditions are not successful, the result will be INFINITE,
//     * though this can be normalized to a finite value in comparing multiple tests
//     * by replacing the INFINITE result with a maximum # of cycles limit,
//     * which will be smaller in cases where the success conditions are
//     * completed prior to the limit.
//     * */
//    public double getCost() {
//        return EternalTaskCondition.cost(requires);
//    }


    @NotNull
    public TestNAR debug() {
        Param.DEBUG = true;
        //nar.stdout();
        return this;
    }

//    /** asserts that (a snapshot of) the termlink graph is fully connected */
//    public TestNAR assertTermLinkGraphConnectivity() {
//        TermLinkGraph g = new TermLinkGraph(nar);
//        assert("termlinks form a fully connected graph:\n" + g.toString(), g.isConnected());
//        return this;
//    }

    @NotNull
    public TestNAR input(@NotNull String... s) {
        finished = false;
        for (String x : s)
            try {
                nar.input(x);
            } catch (Narsese.NarseseException e) {
                fail(e.toString());
            }
        return this;
    }
    @NotNull
    public TestNAR input(@NotNull ITask... s) {
        finished = false;
        for (ITask x : s) {
            if (x.pri()==0 || x.isDeleted())
                throw new RuntimeException("input task has zero or deleted priority");
            nar.input(x);
        }
        return this;
    }

    /**
     * warning may not work with time=0
     */
    @NotNull
    public TestNAR inputAt(long time, String s) {
        finished = false;
        try {
            nar.inputAt(time, s);
        } catch (Narsese.NarseseException e) {
            fail(e.toString());
        }
        return this;
    }

    @NotNull
    public TestNAR inputAt(long time, Task... t) {
        finished = false;
        nar.inputAt(time, t);
        return this;
    }

    @NotNull
    public TestNAR believe(@NotNull String t, @NotNull Tense tense, float f, float c) {
        finished = false;
        nar.believe(t, tense, f, c);
        return this;
    }

    @NotNull
    public TestNAR goal(@NotNull String t, @NotNull Tense tense, float f, float c) {
        finished = false;
        try {
            nar.goal(nar.term(t), tense, f, c);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @NotNull
    public TestNAR goal(@NotNull String s) {
        nar.goal(s);
        return this;
    }

    @NotNull
    public TestNAR log() {
        nar.log();
        return this;
    }

    /**
     * fails if anything non-input is processed
     */
    @NotNull
    public TestNAR mustNotOutput() {
        exitOnAllSuccess = false;
        requireConditions = false; //this is the condition
        nar.onTask(c -> {
            if (!c.isInput())
                fail(c.toString() + " output, but must not output anything");
        });
        return this;
    }

    public TestNAR dur(int newDur) {
        nar.time.dur(newDur);
        return this;
    }


    final class EarlyExit implements Consumer<NAR> {

        final int checkResolution; //every # cycles to check for completion
        int cycle;

        public EarlyExit(int checkResolution) {
            this.checkResolution = checkResolution;
            nar.onCycle(this);
        }

        @Override
        public void accept(NAR nar) {

            if (++cycle % checkResolution == 0 && !requires.isEmpty()) {

                boolean finished = true;

                for (int i = 0, requiresSize = requires.size(); i < requiresSize; i++) {
                    if (!requires.get(i).isTrue()) {
                        finished = false;
                        break;
                    }
                }

                if (finished) {
                    stop();
                }

            }
        }
    }


    public void stop() {
        finished = true;
    }

    //TODO initialize this once in constructor
    @NotNull
    final Topic<Tasked>[] outputEvents;

    @NotNull
    public TestNAR mustOutput(long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occTimeAbsolute) {
        return mustOutput(cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, occTimeAbsolute, occTimeAbsolute);
    }

    @NotNull
    public TestNAR mustOutput(long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long start, long end) {
        mustEmit(outputEvents, cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, start, end);
        return this;
    }


    @NotNull
    public TestNAR mustOutput(long withinCycles, @NotNull String task) {
        try {
            return mustEmit(outputEvents, withinCycles, task);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }


    @NotNull
    public TestNAR mustEmit(@NotNull Topic<Tasked>[] c, long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax) {
        return mustEmit(c, cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, ETERNAL, ETERNAL);
    }


    @NotNull
    TestNAR mustEmit(@NotNull Topic<Tasked>[] c, long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long start, long end) {
        try {
            return mustEmit(c, cycleStart, cycleEnd, sentenceTerm, punc, freqMin, freqMax, confMin, confMax, start, end, true);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }
    }

    @NotNull
    TestNAR mustEmit(@NotNull Topic<Tasked>[] c, long cycleStart, long cycleEnd, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long start, long end, boolean must) throws Narsese.NarseseException {


        float h = Param.TESTS_TRUTH_ERROR_TOLERANCE / 2.0f;

        if (freqMin == -1)
            freqMin = freqMax;

        int tt = temporalTolerance;
        cycleStart -= tt;
        cycleEnd += tt;

        EternalTaskCondition tc = start == ETERNAL ?
                new EternalTaskCondition(nar,
                        cycleStart, cycleEnd,
                        sentenceTerm, punc, freqMin - h, freqMax + h, confMin - h, confMax + h) :
                new TemporalTaskCondition(nar,
                        cycleStart, cycleEnd,
                        start, start, end, end,
                        sentenceTerm, punc, freqMin - h, freqMax + h, confMin - h, confMax + h);

        for (Topic<Tasked> cc : c) {
            cc.on(tc);
        }

        finished = false;

        if (must) {
            requires.add(tc);
        } else {
            exitOnAllSuccess = false; //require entire execution, not just finish early
            disqualifies.add(tc);
        }

        return this;
//
//        ExplainableTask et = new ExplainableTask(tc);
//        if (showExplanations) {
//            explanations.add(et);
//        }
//        return et;
    }



    @Nullable
    public Object getResult() {
        return result;
    }


    public final long time() {
        return nar.time();
    }

    @NotNull
    public TestNAR mustEmit(@NotNull Topic<Tasked>[] c, long withinCycles, @NotNull String task) throws Narsese.NarseseException {
        Task t = nar.task(task);
        //TODO avoid reparsing term from string

        long now = time();
        String termString = t.term().toString();
        if (t.truth() != null) {
            float freq = t.freq();
            float conf = t.conf();
            return mustEmit(c, now, now + withinCycles, termString, t.punc(), freq, freq, conf, conf, t.start(), t.end());
        } else {
            return mustEmit(c, now, now + withinCycles, termString, t.punc(), NaN, NaN, NaN, NaN, t.start(), t.end());
        }
    }

    @NotNull
    public TestNAR mustOutput(long withinCycles, @NotNull String term, byte punc, float freq, float conf) {
        long now = time();
        return mustOutput(now, now + withinCycles, term, punc, freq, freq, conf, conf, ETERNAL);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freqMin, float freqMax, float confMin, float confMax) {
        return mustBelieve(withinCycles, term, freqMin, freqMax, confMin, confMax, ETERNAL);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freqMin, float freqMax, float confMin, float confMax, long tense) {
        long now = time();
        return mustOutput(now, now + withinCycles, term, BELIEF, freqMin, freqMax, confMin, confMax, tense);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freq, float confidence, @NotNull Tense t) {
        long ttt = nar.time();
        return mustOutput(ttt, ttt + withinCycles, term, BELIEF, freq, freq, confidence, confidence, nar.time(t));
    }

    /**
     * tests for any truth value at the given occurrences
     */
    @NotNull
    public TestNAR mustNotOutput(long withinCycles, @NotNull String sentenceTerm, byte punc, @NotNull long... occs) {
        assert(occs.length > 0);
        for (long occ : occs)
            mustNotOutput(withinCycles, sentenceTerm, punc, 0, 1, 0, 1, occ);
        return this;
    }

    @NotNull
    public TestNAR mustNotOutput(long withinCycles, @NotNull String sentenceTerm, byte punc, float freqMin, float freqMax, float confMin, float confMax, long occ) {
        if (freqMin < 0 || freqMin > 1f || freqMax < 0 || freqMax > 1f || confMin < 0 || confMin > 1f || confMax < 0 || confMax > 1f || freqMin != freqMin || freqMax != freqMax)
            throw new UnsupportedOperationException();

        try {
            long time = nar.time();
            return mustEmit(outputEvents,
                    time, time + withinCycles,
                    sentenceTerm, punc, freqMin, freqMax, confMin,
                    confMax, occ, occ, false);
        } catch (Narsese.NarseseException e) {
            throw new RuntimeException(e);
        }

    }


    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freq, float confidence, long occTimeAbsolute) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, term, BELIEF, freq, freq, confidence, confidence, occTimeAbsolute);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freq, float confidence, long start, long end) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, term, BELIEF, freq, freq, confidence, confidence, start, end);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float freq, float confidence) {
        return mustBelieve(withinCycles, term, freq, confidence, Tense.Eternal);
    }

    @NotNull
    public TestNAR mustBelieve(long withinCycles, @NotNull String term, float confidence) {
        return mustBelieve(withinCycles, term, 1.0f, confidence);
    }

    @NotNull
    public TestNAR mustDesire(long withinCycles, @NotNull String goalTerm, float freq, float conf) {
        return mustOutput(withinCycles, goalTerm, GOAL, freq, conf);
    }

    @NotNull
    public TestNAR mustQuestion(long withinCycles, @NotNull String qt) {
        return mustOutput(withinCycles, qt, QUESTION);
    }

    @NotNull
    public TestNAR mustQuest(long withinCycles, @NotNull String qt) {
        return mustOutput(withinCycles, qt, QUEST);
    }

    private TestNAR mustOutput(long withinCycles, @NotNull String qt, byte question) {
        return mustOutput(withinCycles, qt, question, NaN, NaN);
    }

    @NotNull
    public TestNAR mustDesire(long withinCycles, @NotNull String goalTerm, float freq, float conf, long occ) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, goalTerm, GOAL, freq, freq, conf, conf, occ);
    }

    @NotNull
    public TestNAR mustDesire(long withinCycles, @NotNull String goalTerm, float freq, float conf, long start, long end) {
        long t = nar.time();
        return mustOutput(t, t + withinCycles, goalTerm, GOAL, freq, freq, conf, conf, start, end);
    }


    public TestNAR ask(@NotNull String termString) throws Narsese.NarseseException {
        nar.question(termString);
        return this;
    }
    public TestNAR quest(@NotNull String termString) throws Narsese.NarseseException {
        nar.quest($.$(termString));
        return this;
    }

    @NotNull
    public TestNAR askAt(int i, String term) {
        try {
            nar.inputAt(i, term + '?');
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    /** TODO make this throw NarseseException */
    @NotNull public TestNAR believe(@NotNull String termString) {
        try {
            nar.believe(termString);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }

    @NotNull
    public TestNAR believe(@NotNull String termString, float freq, float conf) {
        try {
            nar.believe(termString, freq, conf);
        } catch (Narsese.NarseseException e) {
            e.printStackTrace();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
        return this;
    }


    //    public static final class Report implements Serializable {
//
//        public final long time;
//        @NotNull
//        public final HitMeter[] eventMeters;
//        //@Nullable
//        protected final Serializable error;
//        protected final Task[] inputs;
//        private final TestNAR test;
    //final transient int stackElements = 4;


//
//        public boolean isSuccess() {
//            return true;
//        }

//        public void toString(@NotNull PrintStream out) {
//
//            if (error!=null) {
//                out.print(error);
//            }
//
//            out.print("@" + time + ' ');
//            out.print(Arrays.toString(eventMeters) + '\n');
//
//            for (Task t : inputs) {
//                out.println("IN " + t);
//            }
//
//            cond.forEach(c ->
//                c.toString(out)
//            );
//        }
//    }

    @NotNull
    public TestNAR run() {
        return run(true);
    }

    protected boolean requireConditions() {
        return requireConditions;
    }

    @NotNull
    public TestNAR run(boolean testAndPrintReport /* for use with JUnit */) {
        return run(0, testAndPrintReport);
    }

    @NotNull
    public TestNAR test(long cycles) {
        return run(cycles, true);
    }

    @NotNull
    public TestNAR run(long finalCycle, boolean testAndPrintReport /* for use with JUnit */) {

        if (requireConditions())
            assertTrue("No conditions tested", !requires.isEmpty() || !disqualifies.isEmpty());


        //TODO cache requires & logger, it wont change often
        String id = requires.toString();

        for (NARCondition oc : requires) {
            long oce = oc.getFinalCycle();
            if (oce > finalCycle) finalCycle = oce + 1;
        }
        for (NARCondition oc : disqualifies) {
            long oce = oc.getFinalCycle();
            if (oce > finalCycle) finalCycle = oce + 1;
        }

        StringWriter trace;
        if (collectTrace)
            nar.trace(trace = new StringWriter());
        else
            trace = null;

        if (exitOnAllSuccess) {
            new EarlyExit(3);
        }

        runUntil(finalCycle);

        boolean success = true;
        for (NARCondition t : requires) {
            if (!t.isTrue()) {
                success = false;
                break;
            }
        }
        for (NARCondition t : disqualifies) {
            if (t.isTrue()) {

                logger.error("mustNot: {}", t);
                t.log(logger);
                ((EternalTaskCondition) t).matched.forEach(shouldntHave -> logger.error("Must not:\n{}", shouldntHave.proof()));


                success = false;
            }
        }

        if (testAndPrintReport) {

            //if (requires.isEmpty())
            //return this;


            //assertTrue("No cycles elapsed", tester.nar.memory().time/*SinceLastCycle*/() > 0);

            long time = time();

            //Task[] inputs = n.inputs.toArray(new Task[n.inputs.size()]);
//            Collection<HitMeter> var = eventMeters.values();
//            HitMeter[] eventMeters1 = var.toArray(new HitMeter[var.size()]);


            String pattern = "{}\n\t{} {} {}IN \ninputs";
            Object[] args = {id, time, result/*, eventMeters1*/};

            if (result != null) {
                logger.error(pattern, args);
            } else {
                logger.info(pattern, args);
            }

            requires.forEach(c ->
                    c.log(logger)
            );

            if (trace != null)
                logger.trace("{}", trace.getBuffer());

        }

        assertTrue(success);

        return this;
    }


    @NotNull
    public TestNAR run(long extraCycles) {
        return runUntil(time() + extraCycles);
    }

    @NotNull
    public TestNAR runUntil(long finalCycle) {

        result = null;

        if (showOutput)
            nar.trace();


        //try {
        int frames = (int) (finalCycle - time());
        while (frames-- > 0 && !finished)
            nar.cycle();

        /*}
        catch (Exception e) {
            error = e;
        }*/

        return this;
    }


//    /** returns null if there is no error, or a non-null String containing report if error */
//    @Deprecated public String evaluate() {
//        //TODO use report(..)
//
//        int conditions = requires.size();
//        int failures = getError()!=null ? 1 : 0;
//
//        for (TaskCondition tc : requires) {
//            if (!tc.isTrue()) {
//                failures++;
//            }
//        }
//
//        int successes = conditions - failures;
//
//
//        if (error!=null || failures > 0) {
//            String result = "";
//
//            if (error!=null) {
//                result += error.toString() + " ";
//            }
//
//            if (failures > 0) {
//                result += successes + "/ " + conditions + " conditions passed";
//            }
//
//            return result;
//        }
//
//        return null;
//
//    }

//    public void report(PrintStream out, boolean showFail, boolean showSuccess, boolean showExplanations) {
//
//        boolean output = false;
//
//        if (showFail || showSuccess) {
//
//            for (TaskCondition tc : requires) {
//
//                if (!tc.isTrue()) {
//                    if (showFail) {
//                        out.println(tc.getFalseReason());
//                        output = true;
//                    }
//                } else {
//                    if (showSuccess) {
//                        out.println(tc.getTrueReasons());
//                        output = true;
//                    }
//                }
//            }
//
//        }
//
//        if (error!=null) {
//            error.printStackTrace();
//            output = true;
//        }
//
//        if (showExplanations) {
//            for (ExplainableTask x : explanations ) {
//                x.printMeaning(out);
//                output = true;
//            }
//        }
//
//        if (output)
//            out.println();
//    }
//
//
//    public void inputTest(String script) {
//
//        if (script == null)
//            throw new RuntimeException("null input");
//
//        nar.input( new TestInput(script) );
//
//    }

//    class TestInput extends TextInput {
//        public TestInput(String script) {
//            super(nar, script);
//        }
//
//        @Override
//        public void accept(Task task) {
//            super.accept(task);
//            inputs.add(task);
//        }
//    }
}
