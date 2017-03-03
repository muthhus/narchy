package nars.concept;

import nars.NAR;
import nars.Param;
import nars.conceptualize.state.DefaultConceptState;
import nars.table.BeliefTable;
import nars.test.analyze.BeliefAnalysis;
import nars.truth.DefaultTruth;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import static nars.task.RevisionTest.AB;
import static nars.task.RevisionTest.newNAR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 7/5/15.
 */
public class BeliefTableTest  {


//    @Test public void testRevisionBeliefs() {
//        NAR n = new Default();
//
//        //ArrayListBeliefTable t = new ArrayListBeliefTable(4);
//
//        Task pos = n.inputTask("<a --> b>. %1.00;0.90%");
//        n.frame(1);
//
//        Concept c = n.concept("<a -->b>");
//        BeliefTable b = c.getBeliefs();
//        assertEquals(b.toString(), 1, b.size());
//
//
//        //after the 2nd belief, a revision is created
//        //and inserted with the 2 input beliefs
//        //to produce two beliefs.
//        Task neg = n.inputTask("<a --> b>. %0.00;0.90%");
//
//        n.frame(100);
//        assertEquals(b.toString(), 3, b.size());
//
//        //sicne they are equal and opposite, the
//        //revised belief will be the average of them
//        //but with a higher confidence.
//
//        //assertTrue(p && n);
//        //assertTrue();
//
//        System.out.println(b);
//
//    }


    @Test
    public void testExpectation() {

        assertEquals(0.859f, new DefaultTruth(0.9f,0.9f).expectation(), 0.001f);



        NAR n = newNAR(12);


        BeliefAnalysis b = new BeliefAnalysis(n, AB);

        n.input("b:a. %0.9|0.9%"); //highest positive
        n.input("b:a. %0.8|0.8%");


        n.next();
        b.print();

        assertEquals(0.86f, b.beliefs().match(n.time(), n.time.dur()).expectation(), 0.1f);

        n.input("b:a. %0.2|0.7%");
        n.input("b:a. %0.1|0.8%"); //highest negative

        n.next();
        b.print();

        //assertEquals(0.24f, b.beliefs().top(n).expectation(false), 0.01f);
    }



    @Test
    public void testEternalBeliefRanking() {

        Param.DEBUG = true;

        int maxBeliefs = 10;
        NAR n = newNAR(maxBeliefs);

        BeliefAnalysis b = new BeliefAnalysis(n, AB);

        b.believe(1.0f, 0.5f); n.next();
        b.print();

        BeliefTable beliefs = b.concept().beliefs();

        assertEquals(0.5, beliefs.matchEternal().conf(), 0.001);
        float dur = n.time.dur();
        assertEquals(0.5, beliefs.match(n.time(), dur).conf(), 0.001);
        assertEquals(1, beliefs.size());

        b.believe(1.0f, 0.5f); n.next();
        b.print();
        assertEquals(3 /* revision */, beliefs.size());
        assertEquals(0.669, beliefs.matchEternal().conf(), 0.01);

        b.believe(1.0f, 0.5f); n.next();
        b.print();
        assertEquals(5, beliefs.size());
        @NotNull BeliefTable bb = beliefs;
        assertEquals(0.75, bb.matchEternal().conf(), 0.001);
        assertEquals(0.75, bb.match(n.time(), dur).conf(), 0.01);

        b.believe(1.0f, 0.5f); n.next();
        b.print();
        assertEquals(0.79, beliefs.matchEternal().conf(), 0.02);
        assertEquals(7, beliefs.size());

        //n.step();
        //b.print();

//        int period = 1;
//        int loops = 20;
//
//        for (int i = 0; i < loops; i++) {
//
//
//            b.run(period);
//            //b.printEnergy();
//
//
//            long now = b.nar.time();
//
//            Task tt = b.concept().getBeliefs().top(now);
//            //float p = tt.getExpectation() * tt.projectionRank(now);
//
//            System.out.println(now + " " + " " +  tt);
//
//            //b.print();
//        }

        //b.print();

    }
    @Test
    public void testPolation0() {

        Param.DEBUG = true;

        int maxBeliefs = 3; //includes 3 eternal beliefs we arent using:
        NAR n = newNAR(maxBeliefs*2);
        ((DefaultConceptState)n.concepts.conceptBuilder().awake()).beliefsMaxTemp.set(maxBeliefs);


        BeliefAnalysis b = new BeliefAnalysis(n, AB);

        //assertEquals(0.0, (Double) b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);

        int spacing = 4;
        float conf = 0.85f;

        //create linear gradient of belief across time, freq beginning at 0 and increasing to 1
        for (int i = 0; i < maxBeliefs; i++) {
            b.believe(0.5f, i/((float)maxBeliefs-1), conf, i * spacing).run(spacing);

            assertEquals(i+1, b.size());
        }

        b.print();
        System.out.println();

        assertEquals(maxBeliefs, b.size());

        int margin = spacing * (maxBeliefs/2);

        @NotNull BeliefTable table = b.concept().beliefs();


        for (int i = -margin; i < spacing * maxBeliefs + margin; i++) {
            System.out.println(i + "\t" + table.truth(i, n.time.dur()));
        }
        System.out.println();
        for (int i = -margin; i < spacing * maxBeliefs + margin; i++) {
            System.out.println(i + "\t" + table.truth(i, 0   /* relative to zero */, n.time.dur()));
        }

        /* first */
        @Nullable Truth firstBeliefTruth = table.truth(0, n.time.dur());
        assertEquals(0.03f, firstBeliefTruth.freq(), 0.1f);

        /* last */
        float dur = n.time.dur();
        @Nullable Truth lastBeliefTruth = table.truth(spacing * (maxBeliefs - 1), dur);
        assertEquals(1f, lastBeliefTruth.freq(), 0.1f);

        /** closer to 0.5 as uncertainty grows */
        @Nullable Truth endTruth = table.truth(spacing * (maxBeliefs - 1) + margin, dur);
        assertEquals(0.79f, endTruth.freq(), 0.2f);
        assertTrue(lastBeliefTruth.conf() >= endTruth.conf());

        /** closer to 0.5 as uncertainty grows */
        @Nullable Truth startTruth = table.truth(0 - margin, dur);
        assertEquals(0.2f, startTruth.freq(), 0.2f);
        assertTrue(firstBeliefTruth.conf() >= startTruth.conf());
    }



//    @Ignore
//    @Test
//    public void testTruthOscillationLongTerm() {
//
//        NAR n = newNAR(16, (c, b) -> {
//            return new BeliefTable.BeliefConfidenceAndCurrentTime(c);
//        });
//        n.memory().duration.set(1);
//
//        int period = 2;
//
//        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");
//
//        boolean state = true;
//
//        //for (int i = 0; i < 16; i++) {
//        for (int i = 0; i < 255; i++) {
//
//            if (i % (period) == 0) {
//                b.believe(state ? 1f : 0f, 0.9f, Tense.Present);
//                state = !state;
//            }
//            else {
//                //nothing
//            }
//
//            n.frame();
//
//            /*if (i % 10 == 0) {
//                b.printWave();
//                b.printEnergy();
//                b.print();
//            }*/
//        }
//
//
//    }
}