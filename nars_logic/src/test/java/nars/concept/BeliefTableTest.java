package nars.concept;

import nars.Global;
import nars.NAR;
import nars.nal.Tense;
import nars.nar.AbstractNAR;
import nars.nar.Default;
import nars.task.Task;
import nars.truth.DefaultTruth;
import nars.util.meter.BeliefAnalysis;
import nars.util.meter.MemoryBudget;
import org.junit.Test;

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

    public AbstractNAR newNAR(int maxBeliefs) {
        AbstractNAR d = new Default(256,1,2,3).nal(7);// {

            /*
            @Override
            public BeliefTable.RankBuilder getConceptRanking() {
                if (rb == null)
                    return super.getConceptRanking();
                else
                    return rb;
            }
            */

        //}
        d.memory.conceptBeliefsMax.set(maxBeliefs);
        return d;
    }

    @Test
    public void testRevision1() {
        //short term immediate test for correct revisionb ehavior
        testRevision(1);
    }
    @Test
    public void testRevision32() {
        //longer term test
        testRevision(32);
    }

    void testRevision(int delay1) {
        Global.DEBUG = true;

        AbstractNAR n = newNAR(6);


        //arbitrary time delays in which to observe that certain behavior does not happen
        int delay2 = delay1;

        //n.stdout();


        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>")
                .believe(1.0f, 0.9f).run(1);

        assertEquals(1, b.size());

                b.believe(0.0f, 0.9f).run(1);

        b.run(delay1);

        b.print();
        //List<Task> bb = Lists.newArrayList( b.beliefs() );

        assertEquals("revised", 3, b.size());

        n.run(delay2);

        assertEquals("no additional revisions", 3, b.size());



    }

    @Test
    public void testTruthOscillation() {

        NAR n = newNAR(8);
        n.memory.duration.set(1);

        int offCycles = 2;

        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");

        assertEquals(0.0, (Double)b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);

        b.believe(1.0f, 0.9f, Tense.Present);
        b.run(1);
        //b.printEnergy();

        b.run(1);
        //b.printEnergy();

        b.believe(0.0f, 0.9f, Tense.Present);
        b.run(1);
        //b.printEnergy();

        b.run(1);
        //b.printEnergy();

        b.print();
        assertEquals(2, b.size());

        b.believe(1.0f, 0.9f, Tense.Present).run(offCycles)
                .believe(0.0f, 0.9f, Tense.Present);

        for (int i = 0; i < 16; i++) {
            b.printEnergy();
            b.print();
            n.run(1);
            //TODO test that they are sorted ?
        }



    }


    @Test
    public void testTruthOscillation2() {

        Global.DEBUG = true;

        int maxBeliefs = 16;
        NAR n = newNAR(maxBeliefs);

        n.memory.duration.set(1);


        BeliefAnalysis b = new BeliefAnalysis(n, "<a-->b>");

        assertEquals(0.0, (Double)b.energy().get(MemoryBudget.Budgeted.ActiveConceptPrioritySum), 0.001);

        int period = 8;
        int loops = 4;
        for (int i = 0; i < loops; i++) {
            b.believe(1.0f, 0.9f, Tense.Present);


            b.run(period);
            //b.printEnergy();

            b.believe(0.0f, 0.9f, Tense.Present);

            b.run(period);
            //b.printEnergy();
            b.print();
        }

        b.run(period);

        b.print();

        //TODO test the belief table for something like the following:
        /*
        Beliefs[@72] 16/16
        <a --> b>. %0.27;0.98% [1, 2, 3, 4, 6] [Revision]
        <a --> b>. %0.38;0.98% [1, 2, 3, 4, 6, 7] [Revision]
        <a --> b>. %0.38;0.98% [1, 2, 3, 4, 5, 6] [Revision]
        <a --> b>. %0.23;0.98% [1, 2, 3, 4, 6, 8] [Revision]
        <a --> b>. %0.35;0.97% [1, 2, 3, 4] [Revision]
        <a --> b>. %0.52;0.95% [1, 2, 3] [Revision]
        <a --> b>. 56+0 %0.00;0.90% [8] [Input]
        <a --> b>. 48+0 %1.00;0.90% [7] [Input]
        <a --> b>. 40+0 %0.00;0.90% [6] [Input]
        <a --> b>. 32+0 %1.00;0.90% [5] [Input]
        <a --> b>. 24+0 %0.00;0.90% [4] [Input]
        <a --> b>. 16+0 %1.00;0.90% [3] [Input]
        <a --> b>. 8+0 %0.00;0.90% [2] [Input]
        <a --> b>. 0+0 %1.00;0.90% [1] [Input]
        <a --> b>. %0.09;0.91% [1, 2] [Revision]
        <a --> b>. 28-20 %0.00;0.18% [1, 2, 3] [((%1, <%1 </> %2>, shift_occurrence_forward(%2, "=/>")), (%2, (<Analogy --> Truth>, <Strong --> Desire>, <ForAllSame --> Order>)))]
         */


//        b.believe(1.0f, 0.9f, Tense.Present).run(offCycles)
//                .believe(0.0f, 0.9f, Tense.Present);

        /*for (int i = 0; i < 16; i++) {
            b.printEnergy();
            b.print();
            n.frame(1);
        }*/



    }

    @Test
    public void testExpectation() {

        assertEquals(0.859f, new DefaultTruth(0.9f,0.9f).expectationPositive(), 0.001f);
        assertEquals(0.859f, new DefaultTruth(0.1f,0.9f).expectationNegative(), 0.001f);


        NAR n = newNAR(12);

        n.memory.duration.set(5);

        BeliefAnalysis b = new BeliefAnalysis(n, "a:b");

        n.input("a:b. %0.9|0.9%"); //highest positive
        n.input("a:b. %0.8|0.8%");


        n.step();
        b.print();

        assertEquals(0.75f, b.beliefs().expectation(true, n.memory), 0.1f);
        assertEquals(0.2f, b.beliefs().expectation(false, n.memory), 0.01f);

        n.input("a:b. %0.2|0.7%");
        n.input("a:b. %0.1|0.8%"); //highest negative

        n.step();
        b.print();

        assertEquals(0.24f, b.beliefs().expectation(false, n.memory), 0.01f);
    }

    @Test
    public void testTemporalProjectionInterpolation() {

        Global.DEBUG = true;

        int maxBeliefs = 12;
        NAR n = newNAR(maxBeliefs);

        n.memory.duration.set(5);

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
        assertEquals(0.5, b.concept().beliefs().topEternal().conf(), 0.001);
        assertEquals(0.5, b.concept().beliefs().top(n.time()).conf(), 0.001);
        assertEquals(1, b.concept().beliefs().size());

        b.believe(1.0f, 0.5f); n.step();
        b.print();
        assertEquals(0.5, b.concept().beliefs().topEternal().conf(), 0.001);
        assertEquals(2, b.concept().beliefs().size());

        b.believe(1.0f, 0.5f); n.step();
        b.print();
        assertEquals(0.67, b.concept().beliefs().topEternal().conf(), 0.001);
        assertEquals(0.67, b.concept().beliefs().top(n.time()).conf(), 0.001);
        assertEquals(4, b.concept().beliefs().size());

        b.believe(1.0f, 0.5f); n.step();
        b.print();
        assertEquals(0.75, b.concept().beliefs().topEternal().conf(), 0.001);
        assertEquals(5, b.concept().beliefs().size());

        n.step();
        b.print();

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

        b.print();

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