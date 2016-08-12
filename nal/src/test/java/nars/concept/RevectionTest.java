package nars.concept;

import nars.NAR;
import nars.Param;
import nars.task.Task;
import nars.truth.Truth;
import nars.util.analyze.BeliefAnalysis;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static nars.concept.RevisionTest.newNAR;
import static nars.nal.UtilityFunctions.w2c;
import static nars.truth.TruthFunctions.c2w;
import static org.junit.Assert.assertEquals;

/**
 * Created by me on 7/2/16.
 */
public class RevectionTest {

    @Test
    public void testTemporalProjectionInterpolation() {

        Param.DEBUG = true;

        int maxBeliefs = 12;
        NAR n = newNAR(maxBeliefs);


        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");
        b.believe(0.5f, 1.0f, 0.85f, 5);
        b.believe(0.5f, 0.0f, 0.85f, 10);
        b.believe(0.5f, 1.0f, 0.85f, 15);
        b.run(1);

        assertEquals(3, b.size());

        int period = 1;
        int loops = 20;

        Set<Task> tops = new HashSet();
        for (int i = 0; i < loops; i++) {


            b.run(period);

            long now = b.nar.time();

            Task tt = b.concept().beliefs().top(now, now);
            tops.add(tt);

            System.out.println(now + " " +  tt);

        }

        assertEquals("all beliefs covered", 3, tops.size());

        b.print();

    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation2_1() {
        testConfidenceAccumulation(2, 1f, 0.1f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation2_5() {
        testConfidenceAccumulation(2, 1f, 0.5f);
    }
    @Test
    public void testTemporalProjectionConfidenceAccumulation2_9() {

        testConfidenceAccumulation(2, 1f, 0.9f);
        testConfidenceAccumulation(2, 0f, 0.9f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation3_1() {
        testConfidenceAccumulation(3, 1f, 0.1f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation3_5() {
        testConfidenceAccumulation(3, 1f, 0.5f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation3_9() {
        testConfidenceAccumulation(3, 1f, 0.9f);
    }


    public void testConfidenceAccumulation(int repeats, float freq, float inConf) {
        int maxBeliefs = repeats*4;
        NAR n = newNAR(maxBeliefs);

        n.log();

        long at = 5;

        float outConf = w2c( c2w(inConf)*repeats );

        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");
        for (int i = 0; i < repeats; i++) {
            b.believe(0.5f, freq, inConf, at);
        }

        b.run(1);
        b.print();
        assertEquals(repeats, b.size());

        @Nullable Truth result = b.beliefs().truth(at);
        assertEquals(freq, result.freq(), Param.TRUTH_EPSILON);
        assertEquals(outConf, result.conf(), Param.TRUTH_EPSILON);
    }


    @Test
    public void testTemporalRevection() {

        Param.DEBUG = true;

        int maxBeliefs = 4; //includes 3 eternal beliefs we arent using:
        NAR n = newNAR(maxBeliefs);


        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");

        //assertEquals(0.0, (Double) b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);


        b.believe(0.5f, 0.0f, 0.85f, 5);
        n.next();
        b.believe(0.5f, 0.95f, 0.85f, 10);
        n.next();
        b.believe(0.5f, 1.0f, 0.85f, 11); //this and the previous one should get combined when inserting the 4th
        n.next();

        b.print();
        assertEquals(3, b.size());
        assertEquals(5, b.wave().start());
        assertEquals(11, b.wave().end());

        b.believe(0.5f, 1.0f, 0.99f, 12); //this should cause the cycle=10 and cycle=11 beliefs to get revected into one and allow this belief to be inserted
        //the cycle=5 belief should remain since it is more unique

        n.next().next().next();
        b.print();
        assertEquals(4, b.size());

        assertEquals(5, b.wave().start());
        assertEquals(12, b.wave().end());

    }

}
