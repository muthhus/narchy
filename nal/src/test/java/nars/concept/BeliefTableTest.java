package nars.concept;

import nars.Global;
import nars.NAR;
import nars.concept.util.BeliefTable;
import nars.task.Task;
import nars.truth.DefaultTruth;
import nars.util.signal.BeliefAnalysis;
import nars.util.signal.MemoryBudget;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static nars.concept.RevisionTest.newNAR;
import static org.junit.Assert.assertEquals;

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

        assertEquals(0.859f, new DefaultTruth(0.9f,0.9f).expectationPositive(), 0.001f);
        assertEquals(0.859f, new DefaultTruth(0.1f,0.9f).expectationNegative(), 0.001f);


        NAR n = newNAR(12);

        n.duration.set(5);

        BeliefAnalysis b = new BeliefAnalysis(n, "a:b");

        n.input("a:b. %0.9|0.9%"); //highest positive
        n.input("a:b. %0.8|0.8%");


        n.step();
        b.print();

        assertEquals(0.859f, b.beliefs().top(n.time()).expectation(true), 0.1f);
        //assertEquals(0.2f, b.beliefs().top(n).expectation(false), 0.01f);

        n.input("a:b. %0.2|0.7%");
        n.input("a:b. %0.1|0.8%"); //highest negative

        n.step();
        b.print();

        //assertEquals(0.24f, b.beliefs().top(n).expectation(false), 0.01f);
    }

    @Test
    public void testTemporalProjectionInterpolation() {

        Global.DEBUG = true;

        int maxBeliefs = 12;
        NAR n = newNAR(maxBeliefs);

        n.duration.set(5);

        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");

        assertEquals(0.0, (Double) b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);


        b.believe(0.5f, 1.0f, 0.85f, 5);
        b.believe(0.5f, 0.0f, 0.85f, 10);
        b.believe(0.5f, 1.0f, 0.85f, 15);

        int period = 1;
        int loops = 20;

        for (int i = 0; i < loops; i++) {


            b.run(period);
            //b.printEnergy();


            long now = b.nar.time();

            Task tt = b.concept().beliefs().top(now, now);

            System.out.println(now + " " +  tt);

        }

        b.print();

    }

    @Test
    public void testEternalBeliefRanking() {

        Global.DEBUG = true;

        int maxBeliefs = 10;
        NAR n = newNAR(maxBeliefs);

        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");

        b.believe(1.0f, 0.5f); n.step();
        b.print();

        BeliefTable beliefs = b.concept().beliefs();

        assertEquals(0.5, beliefs.topEternal().conf(), 0.001);
        assertEquals(0.5, beliefs.top(n.time()).conf(), 0.001);
        assertEquals(1, beliefs.size());

        b.believe(1.0f, 0.5f); n.step();
        b.print();
        assertEquals(2 /* revision */, beliefs.size());
        assertEquals(0.5, beliefs.topEternal().conf(), 0.001);

        b.believe(1.0f, 0.5f); n.step();
        b.print();
        assertEquals(4, beliefs.size());
        @NotNull BeliefTable bb = beliefs;
        assertEquals(0.67, bb.topEternal().conf(), 0.001);
        assertEquals(0.67, bb.top(n.time()).conf(), 0.001);

        b.believe(1.0f, 0.5f); n.step();
        b.print();
        assertEquals(0.75, beliefs.topEternal().conf(), 0.001);
        assertEquals(6, beliefs.size());

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