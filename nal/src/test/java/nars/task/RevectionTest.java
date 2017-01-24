package nars.task;

import nars.*;
import nars.test.analyze.BeliefAnalysis;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static nars.task.RevisionTest.newNAR;
import static nars.truth.TruthFunctions.c2w;
import static nars.truth.TruthFunctions.w2c;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 5/8/16.
 */
public class RevectionTest {

    @Test
    public void testRevisionEquivalence() throws Narsese.NarseseException {
        MutableTask a = t(1f, 0.5f, 0); //c~=0.67
        a.evidence(0);
        MutableTask b = t(1f, 0.5f, 0);
        b.evidence(1); //cause different hash

        //assertEquals(a.truth(), TruthPolation.truth(0, a, a)); //same item

        //System.out.println( TruthPolation.truth(0, a, b) );
        assertEquals(Revision.revise(a, b), TruthPolation.truth(0, a, b));

    }

    @Test
    public void testRevisionInequivalenceDueToTemporalSeparation() throws Narsese.NarseseException {
        MutableTask a = t(1f, 0.5f, -4).evidence(1).dur(1);
        MutableTask b = t(0f, 0.5f, 4).evidence(2).dur(1);

        Truth pt = TruthPolation.truth(0, a, b);
        @Nullable Truth rt = Revision.revise(a, b);

        assertEquals(pt.freq(), rt.freq(), 0.01f);
        assertTrue(pt.conf() < rt.conf()); //revection result will be less than eternal revision

    }


    @Test
    public void testRevisionEquivalence2Instant() throws Narsese.NarseseException {
        Task a = t(1f, 0.5f, 0);
        Task b = t(0f, 0.5f, 0);
        assertEquals( Revision.revise(a, b), TruthPolation.truth(0, a, b) );
    }

    @Test
    public void testPolation1() throws Narsese.NarseseException {
        Task a = t(1f, 0.5f, 3).evidence(1).dur(1);
        Task b = t(0f, 0.5f, 6).evidence(2).dur(1);
        for (int i = 0; i < 10; i++) {
            System.out.println(i + " " + TruthPolation.truth(i, a, b));
        }

        System.out.println();

        Truth ab2 = TruthPolation.truth(3, a, b);
        assertTrue( ab2.conf() >= 0.5f );

        Truth abneg1 = TruthPolation.truth(3, a, b);
        assertTrue( abneg1.freq() > 0.6f );
        assertTrue( abneg1.conf() >= 0.5f );

        Truth ab5 = TruthPolation.truth(6, a, b);
        assertTrue( ab5.freq() < 0.35f );
        assertTrue( ab5.conf() >= 0.5f );
    }

    @Test
    public void testRevisionEquivalence4() throws Narsese.NarseseException {
        Task a = t(0f, 0.1f, 3).evidence(1).dur(1);
        Task b = t(0f, 0.1f, 4).evidence(2).dur(1);
        Task c = t(1f, 0.1f, 5).evidence(3).dur(1);
        Task d = t(0f, 0.1f, 6).evidence(4).dur(1);
        Task e = t(0f, 0.1f, 7).evidence(5).dur(1);

        for (int i = 0; i < 15; i++) {
            System.out.println(i + " " + TruthPolation.truth(i, a, b, c, d, e));
        }

    }

    public static MutableTask t(float freq, float conf, long occ) throws Narsese.NarseseException {
        return new MutableTask("a:b", '.', $.t(freq, conf)).time(0, occ);
    }

//    public static void _main(String[] args) {
//        TruthPolation p = new TruthPolation(4,
//                0f);
//        //0.1f);
//
//        List<Task> l = Global.newArrayList();
//
//        //NAR n = new Default();
//        l.add( new MutableTask("a:b", '.', new DefaultTruth(0f, 0.5f) ).occurr(0).setCreationTime(0) );
//        l.add( new MutableTask("a:b", '.', new DefaultTruth(1f, 0.5f) ).occurr(5).setCreationTime(0) );
//        l.add( new MutableTask("a:b", '.', new DefaultTruth(0f, 0.75f) ).occurr(10).setCreationTime(0) );
//        print(p, l, -5, 15);
//
//
//    }

    public static void print(@NotNull TruthPolation p, @NotNull List<Task> l, int start, int end) {
        //interpolation (revision) and extrapolation (projection)
        System.out.println("INPUT");
        for (Task t : l) {
            System.out.println(t);
        }

        System.out.println();

        System.out.println("TRUTHPOLATION");
        for (long d = start; d < end; d++) {
            Truth a1 = TruthPolation.truth(d, l);
            System.out.println(d + ": " + a1);
        }
    }


    @Test
    public void testTemporalProjectionInterpolation() throws Narsese.NarseseException {

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

            Task tt = b.concept().beliefs().match(now, now);
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
        testConfidenceAccumulation(2, 0.5f, 0.9f);
        testConfidenceAccumulation(2, 0f, 0.9f);
    }

    @Test
    public void testTemporalProjectionConfidenceAccumulation3_1_pos() {
        testConfidenceAccumulation(3, 1f, 0.1f);
    }
    @Test
    public void testTemporalProjectionConfidenceAccumulation3_1_neg() {
        testConfidenceAccumulation(3, 0f, 0.1f);
    }
    @Test
    public void testTemporalProjectionConfidenceAccumulation3_1_mid() {
        testConfidenceAccumulation(3, 0.5f, 0.1f);
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

        BeliefAnalysis b = null;
        try {
            b = new BeliefAnalysis(n, "<a-->b>");
        } catch (Narsese.NarseseException e) {
            assertTrue(false);
        }
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
    public void testTemporalRevection() throws Narsese.NarseseException {

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