package nars.nal.nal8;

import nars.$;
import nars.Narsese;
import nars.Op;
import nars.task.NALTask;
import nars.term.Term;
import nars.test.TestNAR;
import nars.time.Tense;
import nars.util.AbstractNALTest;
import org.junit.Ignore;
import org.junit.Test;

import static nars.Op.*;
import static nars.time.Tense.ETERNAL;
import static org.junit.Assert.assertEquals;

public class NAL8Test extends AbstractNALTest {

    public static final int cycles = 830;



    @Test
    public void subsent_1_even_simpler_simplerBeliefTemporal() {

        test

                .input("(open(t1) &&+5 [opened]:t1). :|:")
                .mustBelieve(cycles, "open(t1)", 1.0f, 0.81f, 0)
                .mustBelieve(cycles, "[opened]:t1", 1.0f, 0.81f, 5)
                .mustNotOutput(cycles, "open(t1)", BELIEF, ETERNAL)
                .mustNotOutput(cycles, "[opened]:t1", BELIEF, ETERNAL)
        ;
    }

    @Test
    public void subsent_1_even_simpler_simplerGoalTemporal() {

        test

                .input("(open(t1) &&+5 opened(t1))! :|:")
                .mustDesire(cycles, "open(t1)", 1.0f, 0.81f, 0) //only temporal
                .mustNotOutput(cycles, "open(t1)", GOAL, t->t==ETERNAL || t == 5) //no eternal
                .mustDesire(cycles, "opened(t1)", 1.0f, 0.81f, 5) //only temporal
                .mustNotOutput(cycles, "opened(t1)", GOAL,  t->t==ETERNAL || t == 0) //no eternal
        ;
    }




















    @Test
    public void testCorrectGoalOccAndDuration() throws Narsese.NarseseException {
        /*
        $1.0 (happy-->dx)! 1751 %.46;.15% {1753: _gpeß~Èkw;_gpeß~Èky} (((%1-->%2),(%3-->%2),neqRCom(%1,%3)),((%1-->%3),((Induction-->Belief),(Weak-->Goal),(Backwards-->Permute))))
            $NaN (happy-->noid)! 1754⋈1759 %1.0;.90% {1748: _gpeß~Èky}
            $1.0 (dx-->noid). 1743⋈1763 %.46;.90% {1743: _gpeß~Èkw}
         */

        test
                .input(new NALTask($.$("(a-->b)"), GOAL, $.t(1f, 0.9f), 5, 10, 20, new long[]{100}).pri(0.5f))
                .input(new NALTask($.$("(c-->b)"), BELIEF, $.t(1f, 0.9f), 4, 5, 25, new long[]{101}).pri(0.5f))
                .mustDesire(cycles, "(a-->c)", 1f, 0.4f, 10, 20)
        ;

    }



//    @Test
//    public void detaching_condition_2present()  {
//        detachingCondition(true);
//    }
//    @Test
//    public void detaching_condition_2eternal()  {
//        detachingCondition(false);
//    }
//
//    public void detachingCondition(boolean presentOrEternal) {
//        String suffix = "(open({t001}) ==>+5 [opened]:{t001})";
//        test()
//                
//            .input("at(SELF,{t001}). :|: ")
//            .inputAt(10, "(at(SELF,{t001}) &&+5 " + suffix + "). " + (presentOrEternal ? ":|:" : "")) //the occurrence time of this event is ignored; what matter is the task
//            .mustBelieve(cycles, suffix, 1.0f, 0.81f, 5)
//            .mustNotOutput(cycles,suffix,BELIEF,ETERNAL)
//            .mustNotOutput(cycles,suffix,BELIEF,0)
//        ;
//    }






    @Test
    public void subgoal_2() {

        test
                .input("(hold(SELF,{t002}) &&+5 (at(SELF,{t001}) && open({t001})))! :|:")
                .mustDesire(cycles, "hold(SELF,{t002})", 1.0f, 0.81f, 0)
                .mustNotOutput(cycles, "hold(SELF,{t002})", GOAL, ETERNAL);
    }

    @Test
    public void subgoal_2_inner_dt() {

        test
                .input("(hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001})))! :|:")
                .mustDesire(cycles, "hold(SELF,{t002})", 1.0f, 0.73f, 0)
                .mustNotOutput(cycles, "hold(SELF,{t002})", GOAL, ETERNAL);
    }

    @Test
    public void subbelief_2() throws Narsese.NarseseException {
        Term t = $.$("(hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001})))");
        assertEquals(2, t.size());
        assertEquals(10, t.dtRange());

        test
                .input("(hold(SELF,{t002}) &&+5 (at(SELF,{t001}) &&+5 open({t001}))). :|:")
                .mustBelieve(cycles, "hold(SELF,{t002})", 1.0f, 0.73f, 0)
                .mustBelieve(cycles, "(at(SELF,{t001}) &&+5 open({t001}))", 1.0f, 0.81f, 5, 10)
        ;
    }

    @Test
    public void subbelief_2easy() {
        //requires StructuralDeduction to AllowOverlap

        test

                .input("(a:b &&+5 x:y). :|:")
                .mustBelieve(cycles, "a:b", 1.0f, 0.81f, 0)
                .mustBelieve(cycles, "x:y", 1.0f, 0.81f, 5)
        ;
    }

    @Test
    public void subbelief_2medium() {
        //requires StructuralDeduction to AllowOverlap

        test
                .input("(a:b &&+5 (c:d &&+5 x:y)). :|:")
                .mustBelieve(cycles, "a:b", 1.0f, 0.73f, 0)
                .mustBelieve(cycles, "c:d", 1.0f, 0.73f, 5)
                .mustBelieve(cycles, "x:y", 1.0f, 0.73f, 10)
        ;
    }








    //below here: CONTINUE moving appropriate tests to NAL8EternalMixTest -------



    @Test
    public void testDeiredImpl() {
        /*
        $0.61;0.39;0.50$ (y)! :172: %1.00;0.45%
              PARENT   $0.75;0.80;0.95$ <(x) ==> (y)>! :3: %1.00;0.90%
              BELIEF   $0.50;0.80;0.95$ (x). :4: %1.00;0.90%
         */

        TestNAR t = test;
        t

                .believe("(x)")
                .goal("((x)&&(y))")
                .mustDesire(cycles, "(y)", 1f, 0.81f);
    }

    @Test
    public void testBelievedImplOfDesire() {

        TestNAR t = test;
        t
                .goal("(x)")
                .believe("((x)==>(y))")
                .mustDesire(cycles, "(y)", 1f, 0.45f);
    }

    @Test
    public void testGoalConjunctionDecompose() {

        test
                .goal("((x) &&+3 (y))", Tense.Present, 1f, 0.9f)
                .mustDesire(cycles, "(x)", 1f, 0.81f, 0)
                //.mustNotOutput(cycles, "(y)", GOAL, 3)
                .mustNotOutput(cycles, "(y)", GOAL, ETERNAL);
    }


//    @Test public void testSubIfUnifiesForwardWontDecomposeAntecedentGoal() {
//
//        /*
//        $.32;.54$ (happy)! 1272-1272 %.56;.40% {1272-1272: 1;2;f;v} ((%1,(%2&&%3),task(positive),time(decomposeBelief),neqCom(%1,%3)),(subIfUnifiesForward(%3,%2,%1),((Strong-->Goal))))
//
//            $1.0;.90$ (happy)! :0: %1.0;.99% {0: f}
//            $.56;.67$ (((happy) &&+2 #1) &&+37 #1). 45-45 %.56;.73% {45-45: 1;2;v} ((%1,%2,task(positive),task("."),time(dtAfterOrEternal),neqAndCom(%1,%2)),(varIntro((%1 &&+- %2)),((Intersection-->Belief))))
//        */
//        test()
//                .goal("(happy)", Tense.Eternal, 1f, 0.9f)
//                .input("(((happy) &&+2 #1) &&+37 #1). :|:")
//                .mustNotOutput(cycles, "(happy)", GOAL, new long[] { (long)ETERNAL, 0, 2, 37, 39 } )
//                ;
//    }

    @Test
    public void testConditionalGoalConjunctionDecomposePositiveGoal() {

        test
                .goal("(x)", Tense.Present, 1f, 0.9f)
                .believe("((x) &&+3 (y))", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "(x)", 1f, 0.81f, 0)
                .mustBelieve(cycles, "(y)", 1f, 0.81f, 3)
                //.mustDesire(cycles, "(y)", 1f, 0.81f, 3)
        ;
    }

 @Test
    public void testConditionalGoalConjunctionDecomposePositivePostconditionGoal() {

        test
                .goal("(y)", Tense.Present, 1f, 0.9f)
                .believe("((x) &&+3 (y))", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "(x)", 1f, 0.81f, 0)
                .mustBelieve(cycles, "(y)", 1f, 0.81f, 3)
                .mustDesire(cycles, "(x)", 1f, 0.81f, (t) -> t > 0);
    }

    @Test
    public void testConditionalGoalConjunctionDecomposePositiveGoalNegativeBeliefSubterm() {

        test
                .goal("(x)", Tense.Present, 1f, 0.9f)
                .believe("(--(x) &&+3 (y))", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "(x)", 0f, 0.81f, 0)
                .mustBelieve(cycles, "(y)", 1f, 0.81f, 3)
                ;
                //.mustDesire(cycles, "(y)", 0f, 0.81f, 3);
    }

    @Test
    public void testConditionalGoalConjunctionDecomposeNegativeGoal() {

        test
                .goal("(x)", Tense.Present, 0f, 0.9f)
                .believe("((x) &&+3 (y))", Tense.Present, 1f, 0.9f)
                .mustBelieve(cycles, "(x)", 1f, 0.81f, 0)
                .mustBelieve(cycles, "(y)", 1f, 0.81f, 3)
                //.mustDesire(cycles, "(y)", 0f, 0.81f, 0)
        ;
    }


    @Test
    public void testGoalConjunctionPostDecompose() {
        //after a belief has been fedback, continue decomposing the conjunction goal to expose the (y) desire:

        test

                .goal("((x) &&+3 (y))", Tense.Present, 1f, 0.9f)
                .believe("(x)", Tense.Present, 1f, 0.9f)
                .mustDesire(cycles, "(y)", 1f, 0.81f, 3)
                .mustNotOutput(cycles, "(y)", GOAL, ETERNAL);
    }


    //        @Test
    //        public void subgoal_2_small()  {
    //            TestNAR tester = test();
    //
    //            tester.input("(hold(SELF,y) &&+5 at(SELF,x))!");
    //
    //            tester.mustDesire(cycles, "hold(SELF,y)", 1.0f, 0.81f);
    //
    //        }


    //        @Test public void subsent_1_even_simpler_simplerGoalTemporal()  {
    //        test()
    //
    //                .input("(open(t1) &&+5 [opened]:t1)! :|:")
    //                .mustDesire(cycles, "open(t1)", 1.0f, 0.81f, 0) //temporal
    //                .mustNotDesire(cycles, "[opened]:t1", 1.0f, 0.81f, 5) //temporal
    //        ;
    //    }

    //        @Test
    //        public void detaching_single_premise2()  {
    //            TestNAR tester = test();
    //
    //            tester.input("(at(SELF,{t001}) &&+5 open({t001}) )!");
    //            tester.mustDesire(cycles, "at(SELF,{t001})", 1.0f, 0.81f);
    //            tester.mustNotDesire(cycles, "open({t001})", 1.0f, 0.81f);
    //
    //        }

    //        @Test
    //        public void detaching_single_premise_temporal()  {
    //            TestNAR tester = test();
    //            tester
    //
    //                    .input("(reachable:(SELF,{t002}) &&+5 pick({t002}))! :|:")
    //                    .mustDesire(6, "reachable:(SELF,{t002})", 1.0f, 0.81f, 0)
    //                    .mustNotDesire(6, "pick({t002})", 1.0f, 0.81f, 5)
    //            ;
    //        }

    //        @Test
    //        public void detaching_single_premise()  {
    //            TestNAR tester = test();
    //            tester
    //
    //                    .input("(reachable:(SELF,{t002}) &&+5 pick({t002}))!")
    //                    .mustDesire(cycles, "reachable:(SELF,{t002})", 1.0f, 0.81f)
    //                    .mustDesire(cycles, "pick({t002})", 1.0f, 0.81f);
    //        }
    //}




    @Test
    public void testInhibition() {
        //by deduction

        test
                .goal("(reward)")
                .believe("((good) ==> (reward))", 1, 0.9f)
                .believe("((--,(bad)) ==> (reward))", 1, 0.9f)
                .mustDesire(cycles, "(good)", 1.0f, 0.81f)
                .mustDesire(cycles, "(bad)", 0.0f, 0.81f);

    }

    @Test
    public void testInhibitionInverse() {

        test
                .goal("(--,(reward))")
                .believe("((good) ==> (reward))", 1, 0.9f)
                .believe("((--,(bad)) ==> (reward))", 1, 0.9f)
                .mustDesire(cycles, "(good)", 0.0f, 0.81f)
                .mustDesire(cycles, "(bad)", 1.0f, 0.81f)
        ;
    }

    @Test
    public void testInhibition0() {

        test

                .goal("(reward)")
                .believe("((bad) ==> (--,(reward)))", 1, 0.9f)
                .mustDesire(cycles, "(bad)", 0.0f, 0.81f)
                .mustNotOutput(cycles, "(bad)", GOAL, 0.5f, 1f, 0f, 1f, ETERNAL);
    }

    @Test
    public void testInhibition1() {
        //deisreDed, and its negative counterpart for the negated belief

        test
                //.log()
                .goal("(reward)")
                .believe("((good) ==> (reward))", 1, 0.9f)
                .believe("((bad) ==> (--,(reward)))", 1, 0.9f)
                .mustDesire(cycles, "(good)", 1.0f, 0.81f)
                .mustNotOutput(cycles, "(good)", GOAL, 0.0f, 0.7f, 0.5f, 1f, ETERNAL)
                .mustDesire(cycles, "(bad)", 0.0f, 0.81f)
                .mustNotOutput(cycles, "(bad)", GOAL, 0.3f, 1f, 0f, 1f, ETERNAL);
    }

    @Test
    public void testInhibitionReverse() {
        //deisreDed, and its negative counterpart for the negated belief

        test
                //.log()
                .goal("(reward)")
                .believe("((reward) ==> (good))", 1, 0.9f)
                .believe("((--,(reward)) ==> (bad))", 1, 0.9f)
                .mustDesire(cycles, "(good)", 1.0f, 0.45f)
                .mustDesire(cycles, "(bad)", 0.0f, 0.45f)
                .mustNotOutput(cycles, "(good)", GOAL, 0.0f, 0.5f, 0.0f, 1f, ETERNAL)
                .mustNotOutput(cycles, "(bad)", GOAL, 0.5f, 1f, 0.0f, 1f, ETERNAL);
    }




    //similarity should not spread desire
    @Test
    public void testGoalSimilaritySpreading() {

        test
                .input("(R)!")
                .input("((G) <-> (R)).")
                //.mustDesire(cycles, "(G)", 1.0f, 0.81f);
                .mustNotOutput(cycles, "(G)", GOAL, ETERNAL); // because <-> isnt symmetric
    }

    //similarity should not spread desire
    @Test
    public void testNegatedGoalSimilaritySpreading() {

        test
                .input("--(R)!")
                .input("((G) <-> (R)).")
                .mustNotOutput(cycles, "(G)", GOAL, ETERNAL); // because <-> isnt symmetric
    }



//    @Test public void testInheritanceCompositionTemporal() {
//        /*
//        WRONG OCCURRENCE TIME:
//        $.38;.39;.78$ (((in)|(left))-->^cam)! 474-424 %.12;.99% {474-424: 67;7k;ab;kv;lx} PremiseRule{	 prePreconditions=[TermNotEquals(1:(0),0:(0)), task:".!"]	 match=MatchTaskBelief[((%1-->%2),(%3-->%2))]	 postconditions=[PostCondition{term=((%1|%3)-->%2), beliefTruth=Intersection, goalTruth=Intersection, puncOverride= }]	 temporalize=nars.nal.TimeFunctions$$Lambda$122/684230144@3fbfa96	 eternalize=false	 anticipate=false	 minNAL=1	 source='<(P --> M), (S --> M), notSet(S), notSet(P), neq(S,P), no_common_subterm(S,P) |- ((S | P) --> M), (Belief:Intersection, Desire:Intersection, Derive:NoSwap)>'}
//              $1.0;.50;.51$ cam(left)! 464-414 %.78;1.0% {464-414: 67;7k;ab} Revection Merge
//              $.96;.46;.43$ (((in)|(left))-->^cam). 442-399 %.16;.99% {442-399: kv;lx} Revection Merge
//        */
//
//        //uses AUTO TimeFunction
//
//        test()
//                
//                .inputAt(0, "cam(left)! :|:")
//                .inputAt(4, "(((in)|(left))-->^cam). :|:")
//
//                //must interpolate
//                .mustDesire(cycles, "(((in)|(left))-->^cam)", 1f,0.73f, 4)
//                .mustNotOutput(cycles, "(((in)|(left))-->^cam)", GOAL, 0, ETERNAL);
//    }

    @Ignore
    @Test
    public void testInheritanceDecompositionTemporalGoal() {
        //(((in)|(left))-->^cam)!
        //   cam(in)!
        //   cam(out)!

        test

                .inputAt(0, "(((in)|(left))-->cam)! :|:")
                .mustDesire(cycles, "cam(in)", 1f, 0.81f, 0)
                .mustDesire(cycles, "cam(left)", 1f, 0.81f, 0);

    }

    @Test
    public void testInheritanceDecompositionTemporalBelief() {
        //(((in)|(left))-->^cam)!
        //   cam(in)!
        //   cam(out)!

        test
                .inputAt(0, "(((in)|(left))-->cam). :|:")
                .mustBelieve(cycles, "cam(in)", 1f, 0.81f, 0)
                .mustBelieve(cycles, "cam(left)", 1f, 0.81f, 0);

    }

    @Test
    public void disjunctionBackwardsQuestionEternal() {

        test
                .inputAt(0, "(||, (x), (y))?")
                .believe("(x)")
                .mustBelieve(cycles, "(&&, (--,(x)), (--,(y)))", 0f, 0.81f, ETERNAL);
    }


//    @Test public void testImplBackward1() {
//        test()
//            
//                //.inputAt(2, "(?x ==>+2 (b))? :|:")
//                //.inputAt(2, "(?x &&+2 (b))? :|:")
//                .inputAt(2, "(a). :|:")
//                .inputAt(4, "(b). :|:")
//                .mustBelieve(cycles, "(UNWRITTEN)", 1f,0.81f, 0);
//
//    }
//    @Test public void testMixedTemporalInductionGoalConj() {
//        test()
//                
//                .inputAt(0, "(x). :|:")
//                .inputAt(1, "(y)! :|:")
//                .mustDesire(cycles, "((x) &&+1 (y))", 1f,0.81f, 0)
//        ;
//    }

    @Ignore
    @Test
    public void questConjunction() {

        test
                .input("((a) && (b)).")
                .input("(a)@")
                .mustOutput(cycles, "(b)", Op.QUEST, 0, 1f);

    }

    @Test
    public void testGoalConjunctionNegative1() {

        test
                .input("(a)!")
                .input("((a) && (b)).")
                .mustDesire(cycles, "(b)", 1f, 0.81f);
    }

    @Test
    public void testGoalConjunctionNegative1N() {

        test
                .input("--(a)!")
                .input("(--(a) && (b)).")
                .mustDesire(cycles, "(b)", 1f, 0.81f);
    }

    @Test
    public void testGoalConjunctionNegative2() {

        test
                .input("(a)!")
                .input("((a) && --(b)).")
                .mustDesire(cycles, "(b)", 0f, 0.81f);
    }



    @Test
    public void testGoalImplComponentEternal() {

        test
                .input("(happy)!")
                .input("((--,(in)) =|> ((happy)&&(--,(out)))).")
                .mustDesire(cycles, "(in)", 0f, 0.73f);
    }

    @Test
    public void testConjDecomposeWithDepVar() {

        test
                .input("(#1&&(--,(out)))! :|:")
                .mustDesire(cycles, "(out)", 0f, 0.81f, 0);
    }

    @Test
    public void testPredictiveImplicationTemporalTemporal() {
        /*
        wrong timing: should be (out)! @ 16
        $.36;.02$ (out)! 13 %.35;.05% {13: 9;a;b;t;S;Ü} ((%1,(%2==>%3),belief(negative),time(decomposeBelief)),((--,subIfUnifiesAny(%2,%3,%1)),((AbductionPN-->Belief),(DeductionPN-->Goal))))
            $.50;.90$ (happy)! 13 %1.0;.90% {13: Ü}
            $0.0;.02$ ((out) ==>-3 (happy)). 10 %.35;.05% {10: 9;a;b;t;S} ((%1,(%2==>((--,%3)&&%1073742340..+)),time(dtBeliefExact),notImpl(%1073742340..+)),(subIfUnifiesAny((%2 ==>+- (&&,%1073742340..+)),(--,%3),(--,%1)),((DeductionN-->Belief))))
        */

        test
                .inputAt(0, "((out) ==>-3 (happy)). :|:")
                .inputAt(13, "(happy)! :|:")
                .mustDesire(cycles, "(out)", 1f, 0.81f, 13)
                .mustNotOutput(cycles, "(out)", GOAL, 3);
    }

    @Test
    public void testPredictiveImplicationTemporalTemporalOpposite() {

        test
                .inputAt(0, "((happy) ==>-3 (out)). :|:")
                .inputAt(13, "(happy)! :|:")
                .mustDesire(cycles, "(out)", 1f, 0.45f, 10)
                .mustNotOutput(cycles, "(out)", GOAL,
                        t -> t == 3 || t == 16 || t == 0
                        );
    }

    @Test
    public void testPredictiveImplicationTemporalTemporalNeg() {

        test
                .log()
                .inputAt(0, "(--(out) ==>-3 (happy)). :|:")
                .inputAt(5, "(happy)! :|:")
                .mustDesire(cycles, "(out)", 0f, 0.81f, /*~*/8)
                .mustNotOutput(cycles, "(out)", GOAL, t -> t != 8);
    }





    @Test
    public void testPredictiveEquivalenceTemporalTemporalNeg() {

        test
                .inputAt(0, "(--(out) ==>-3 (happy)). :|:")
                .inputAt(0, "((happy) ==>+3 --(out)). :|:")
                .inputAt(13, "(happy)! :|:")
                .mustDesire(cycles, "(out)", 0f, 0.81f, 16)
                .mustNotOutput(cycles, "(out)", GOAL, 3);
    }

    @Test
    public void conjDecomposeGoalAfter() {

        test
                .inputAt(3, "((a) &&+3 (b)). :|:")
                .inputAt(13, "(b)! :|:")
                .mustDesire(cycles, "(a)", 1f, 0.81f, 13) //desired NOW, not at time 10 as would happen during normal decompose
                .mustNotOutput(cycles, "(a)", GOAL, ETERNAL);
    }

    @Test
    public void conjDecomposeGoalAfterPosNeg() {

        test
                .inputAt(3, "(--(a) &&+3 (b)). :|:")
                .inputAt(6, "(b)! :|:")
                .mustDesire(cycles, "(a)", 0f, 0.81f, 6) //desired NOW, not at time 10 as would happen during normal decompose
                .mustNotOutput(cycles, "(a)", GOAL, ETERNAL);
    }

    @Test
    public void implDecomposeGoalAfterPosPos() {

        test
                .inputAt(3, "((a) ==>+3 (b)). :|:")
                .inputAt(6, "(b)! :|:")
                .mustDesire(cycles, "(a)", 1f, 0.81f,
                        (t)-> t >= 6) //desired NOW, not at time 10 as would happen during normal decompose
                .mustNotOutput(cycles, "(a)", GOAL, t -> t == ETERNAL);
    }

    @Test
    public void implDecomposeGoalAfterPosNeg() {

        test
                .inputAt(3, "(--(a) ==>+3 (b)). :|:")
                .inputAt(13, "(b)! :|:")
                .mustDesire(cycles, "(a)", 0f, 0.81f, 13) //desired NOW, not at time 10 as would happen during normal decompose
                .mustNotOutput(cycles, "(a)", GOAL, t -> t == ETERNAL || t == 10);
    }

    @Test
    public void conjDecomposeGoalAfterNegNeg() {

        test
                .log()
                .inputAt(3, "((a) &&+3 --(b)). :|:")
                .inputAt(6, "(--,(b))! :|:")
                .mustDesire(cycles, "(a)", 1f, 0.81f, (t)->t>=6) //since b is not desired now, it should reverse predict the goal of (a)
                .mustNotOutput(cycles, "(a)", GOAL, ETERNAL);
    }


    @Test
    public void deriveNegInhGoal() {

        test
                .input("b:a!") //positive pair
                .input("c:b.")
                .input("--y:x!") //negative pair
                .input("z:y.")
                .mustDesire(cycles, "c:a", 1f, 0.81f)
                .mustDesire(cycles, "z:x", 0f, 0.81f);
    }



    @Ignore
    @Test
    public void questImplDt() {

        test
                //.log()
                .inputAt(0, "((a),(b)).") //to create termlinks
                .inputAt(0, "(a). :|:")
                .inputAt(4, "(b)@ :|:")
                //TODO needs a 'mustAsk' condition
                .mustOutput(0, cycles, "((b) ==>-4 (a))?", QUESTION, 0f, 1f, 0f, 1f, 4);
    }
//
//    @Test
//    public void testNegativeSimliarityGoal() {
//
//        test
//                .input("((me) <-> --(you))!") //i dont want to be like you
//                .input("((me) --> (you)).") //i am like you
//                //TODO repeat this for <->
//                .mustDesire(cycles, "((you) --> (me))", 0f, 0.81f)
//        ;
//    }


}
