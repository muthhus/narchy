package nars.nal.nal8;

import com.gs.collections.api.tuple.primitive.IntObjectPair;
import com.gs.collections.impl.tuple.primitive.PrimitiveTuples;
import nars.$;
import nars.Global;
import nars.NAR;
import nars.concept.CompoundConcept;
import nars.concept.OperationConcept;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.Truth;
import nars.util.Texts;
import nars.util.meter.TemporalMetrics;
import nars.util.meter.event.SourceFunctionMeter;
import nars.util.signal.MotorConcept;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 5/6/16.
 */
public class ControlFlowTest {

    private static final boolean METER = false;
    private static boolean LOG;


    private final List<CompoundConcept> states = new ArrayList();
    private final TemporalMetrics ts = new TemporalMetrics<>(4096);

    @Test public void testSequence2()   { testSequence(n, 2, 5);     }
    @Test public void testSequence3()   { testSequence(n, 3, 20);     }
    @Test public void testSequence4()   {
        LOG = true;
        testSequence(n, 4, 30);
    }
    @Test public void testSequence8()   { testSequence(n, 8, 30);    }
    //@Test public void testSequence10()  { testSequence(n, 10, 50);     }


    abstract public static class Sequence extends ConceptGroup {

        @NotNull
        private final List<Termed> states;


        public Sequence(@NotNull String id, @NotNull NAR nar, int length) {
            super(id, nar);


            states = Global.newArrayList(length);
            for (int i = 0; i < length; i++) {
                states.add(newState(i));
            }



        }


        /** flow desire forward */
        @NotNull
        public Sequence forward(int delay, float startFrequency) {

            int length = states.size();
            for (int i = 0; i < length - 1; i++) {
                nar.goal($.conj(states.get(i).term(), delay, states.get(i + 1).term()));
            }

            if (startFrequency > 0) {
                nar.goal(start(), Tense.Present, startFrequency);
            }

            return this;
        }


        public Termed start() { return states.get(0); }
        public Termed end() { return states.get(states.size()-1); }

        @NotNull
        protected abstract Termed newState(int i);


        @Override
        public void forEachMember(@NotNull Consumer<Termed> each) {
            states.forEach(each);
        }

    }

    @NotNull
    public ExeTracker testSequence(@NotNull Supplier<NAR> nn, int length, int delay) {

        System.out.println("sequence execution:  states=" + length + " inter-state delay=" + delay);

        int runtime = (delay * length) * 10;


        ExeTracker exeTracker = new ExeTracker();
        NAR n = nn.get();

        if (LOG)
            n.log();

        Sequence seq = new Sequence("(seq)", n, length) {

            @NotNull
            @Override
            protected Termed newState(int i) {
                return newExeState(n, $.p( id , Integer.toString(i) ), exeTracker);
            }

        }.forward(delay, 1f);


        n.run(runtime);

        exeTracker.assertLength(length, delay);

        if (METER) {
            ts.printCSV4(System.out);
            ts.clear();
        }

        return exeTracker;

    }

    @Test public void testBranchThen()  {
        Global.DEBUG = true;
        testBranch(n, 30, 1f);
    }

    @Test public void testBranchElse()  {
        testBranch(n, 50, 0f);
    }

    @Test public void testBranchThenThen()  {
        testBranch(n, 50, 1f, 1f);
    }

    abstract public static class ConceptGroup extends OperationConcept {

        @NotNull
        public final String id;

        public ConceptGroup(@NotNull String id, @NotNull NAR nar) {
            super(id, nar);
            this.id = id;
        }

        /* visitor */
        abstract public void forEachMember(Consumer<Termed> each);

    }





    @NotNull
    public ExeTracker testBranch(@NotNull Supplier<NAR> nn, int delay, @NotNull float... conditionSequence) {


        int beforeBranchLength = 2;
        int afterBranchLength = 2;

        int length = (beforeBranchLength + afterBranchLength );
        System.out.println("branch execution:  states=" + length + " inter-state delay=" + delay);

        int runtime = (delay * length) * 10;


        ExeTracker exeTracker = new ExeTracker();
        NAR n = nn.get();
        if (LOG)
            n.log();

        final String PRE = "pre";
        final String THEN = "then";
        final String ELSE = "else";


        for (int i = 0; i < beforeBranchLength; i++) {
            newExeState(n, s(PRE, i), exeTracker);
        }
        for (int i = 0; i < beforeBranchLength - 1; i++) {
            n.goal($.conj( s(PRE, i), delay, s(PRE, i + 1)));
        }

        Term condition = b(0);
                        //newExeState(n, b(0), exeTracker, 0.01f /* low thresh = pass thru */).term();

        //n.goal($.conj(delay, s(PRE, beforeBranchLength-1), condition));

        for (int i = 0; i < afterBranchLength; i++) {
            newExeState(n, s(THEN, i), exeTracker);
            newExeState(n, s(ELSE, i), exeTracker);
        }
        for (int i = 0; i < afterBranchLength - 1; i++) {
            n.goal($.conj(s(THEN, i), delay, s(THEN, i + 1)));
            n.goal($.conj(s(ELSE, i), delay, s(ELSE, i + 1)));
        }

        //n.goal($.conj( delay, $.conj( condition, s(PRE, beforeBranchLength-1) ), s(THEN, 0)));
        //n.goal($.conj( delay, $.conj( $.neg(condition), s(PRE, beforeBranchLength-1) ), s(ELSE, 0)));
        n.goal($.conj( condition, delay, s(THEN, 0)));
        n.goal($.conj( $.neg(condition), delay, s(ELSE, 0)));


        //n.goal($.conj(delay, $.conj(0, s(PRE, beforeBranchLength-1), condition), s(THEN, 0)));
        //n.goal($.conj(delay, $.conj(0, s(PRE, beforeBranchLength-1), $.neg(condition)), s(ELSE, 0)));

        //n.goal($.conj(delay, $.conj(  s(PRE, beforeBranchLength-1), condition), s(THEN, 0)));
        //n.goal($.conj(delay, $.conj(  s(PRE, beforeBranchLength-1), $.neg(condition)), s(ELSE, 0)));

        //n.believe($.impl(condition, delay, s(THEN, 0)));
        //n.believe($.impl($.neg(condition), delay, s(ELSE, 0)));


        Compound start = s(PRE, 0);


        for (float B : conditionSequence ) {
            System.out.println("Execute Forward branch w/ condition=" + B);
            n.believe(condition, Tense.Present, B).step();

            n.goal(start, Tense.Present, 1f);

            n.run(runtime);

            exeTracker.assertLength(length, delay);

            String postBranch = B > 0.5f ? THEN : ELSE;
            exeTracker.assertPath(s(PRE, 0), s(PRE, 1), s(postBranch, 0), s(postBranch, 1));

            exeTracker.clear();

            //pause between
            n.run(delay);
        }



        return exeTracker;

    }


    final Supplier<NAR> n = () -> {
        Default x = new Default(512, 1, 2, 2);
        x.onFrame(c -> {
            ts.update(c.time());
        });
        x.cyclesPerFrame.set(4);
        return x;
    };


    public static Compound s(int i) {
        return $.p("s" + i);
    }
    public static Compound s(String group, int i) {
        return $.p(group + "_" + i);
    }
    public static Compound b(int i) {
        return $.p("b" + i);
    }

    static class ExeTracker {

        final List<IntObjectPair<Compound>> events = Global.newArrayList();
        final SummaryStatistics eventIntervals = new SummaryStatistics();

        public void record(Compound c, long now) {

            if (!events.isEmpty())
                eventIntervals.addValue(now - events.get(events.size() - 1).getOne());

            events.add(PrimitiveTuples.pair((int)now, c));

        }

        public void assertLength(int length, int delay) {
            System.out.println("Execution Intervals: \t min=" + eventIntervals.getMin() + " avg=" + eventIntervals.getMean() + " max=" + eventIntervals.getMax() + " stddev=" + eventIntervals.getStandardDeviation());

            System.out.println("  mean timing error: " + Texts.n2((((float) Math.abs(eventIntervals.getMean() - delay) / delay) * 100.0)) + "%");
            System.out.println();

            assertEquals(length, events.size());
        }

        public void clear() {
            events.clear();
            eventIntervals.clear();
        }

        public void assertPath(Term... states) {
            final int[] i = {0};
            assertTrue( events.stream().allMatch(p -> p.getTwo().equals(states[i[0]++])) );
        }
    }


    @NotNull
    public CompoundConcept newExeState(@NotNull NAR n, @NotNull Compound term, @NotNull ExeTracker e) {

        return newExeState(n, term, e, 0.5f);
    }

    @NotNull
    public CompoundConcept newExeState(@NotNull NAR n, @NotNull Compound term, @NotNull ExeTracker e, float exeThresh) {


        MotorConcept c = new MotorConcept(term, n, (b, d) -> {
            if (d > 0.5f && (d > b + exeThresh)) {
                long now = n.time();
                System.out.println(term + " at " + now + " b=" + b + ", d=" + d + " (d-b)=" + (d - b));

                e.record(term, now);

                //n.goal(term, Tense.Present, 0f); //neutralize

                return d;
                //return (d-b);
                //return 1f;
            }
            //System.out.println("\t not: " + term + " at " + n.time() + " b=" + b + ", d=" + d + " (d-b)=" + (d - b));

            return Float.NaN;
            //return 1f;
        });
        //n.goal(c, Tense.Present, 0.5f, 0.5f); //initially off
        //n.believe(c, Tense.Present, 0.5f, 0.5f); //initially off

        if (METER)
            ts.add(new ConceptBeliefGoalPriorityMeter(c, n));

        states.add(c);

        return c;
    }


    @Test
    public void testToggledSequence() {

    }

    @Test
    public void testWeightedFork() {

    }

    @Test
    public void testLoop() {

    }

    @Test
    public void testToggledLoop() {

    }

    private static class ConceptBeliefGoalPriorityMeter extends SourceFunctionMeter {

        @NotNull
        private final CompoundConcept c;
        private final NAR n;
        @Nullable
        public Truth desire;
        @Nullable
        public Truth belief;

        public ConceptBeliefGoalPriorityMeter(@NotNull CompoundConcept c, NAR n) {
            super(c.toString(), "_blf_frq", "_blf_conf", "_gol_frq", "_gol_conf", "_pri");
            this.c = c;
            this.n = n;
        }

        @Override public Object getValue(Object key, int index) {

            switch (index) {
                case 0:
                    belief = c.beliefs().truth(n.time());
                    return belief.freq();
                case 1:
                    return belief.conf();
                case 2:
                    desire = c.goals().truth(n.time());
                    return desire.freq();
                case 3:
                    return desire.conf();
                case 4:
                    return n.conceptPriority(c);
            }

            return Float.NaN; //shouldnt happen
        }
    }
}
