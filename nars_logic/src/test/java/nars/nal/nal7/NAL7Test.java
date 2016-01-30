package nars.nal.nal7;

import nars.NAR;
import nars.nal.AbstractNALTester;
import nars.nal.Tense;
import nars.util.meter.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

/**
 * Created by me on 1/9/16.
 */
@RunWith(Parameterized.class)
public class NAL7Test extends AbstractNALTester {

    final int cycles = 60;

    public NAL7Test(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{0}")
    public static Iterable configurations() {
        return AbstractNALTester.nars(7, true, true);
    }

    @Test
    public void induction_on_events()  {
        /*

        //    P, S, after(Task,Belief), measure_time(I), notImplicationOrEquivalence(P), notImplicationOrEquivalence(S) |- ((&/,S,I) =/> P), (Belief:Induction, Eternalize:Immediate),
        //                                                                                                                       (P =\> (&/,S,I)), (Belief:Abduction),
        //                                                                                                                       ((&/,S,I) </> P), (Belief:Comparison)

        //    P, S, after(Task,Belief), notConjunction(P), notConjunction(S),  measure_time(I), notImplicationOrEquivalence(P), notImplicationOrEquivalence(S) |- (&/,S,I,P), (Belief:Intersection)

         */


        float inductionConf = 0.45f;
        float abductionConf = 0.45f;
        float comparisonConf = 0.45f;
        float intersectionConf = 0.81f;

        TestNAR t = test();
        //t.nar.log();

        t
        .input("x:before. :|:")
        .inputAt(10, "x:after. :|:")
        .mustBelieve(12, "(x:before ==>+10 x:after)", 1.00f, abductionConf, 10)
        .mustBelieve(12, "(x:after ==>-10 x:before)", 1.00f, inductionConf, 10)
        .mustBelieve(12, "(x:after <=>-10 x:before)", 1.00f, comparisonConf, 10)
        .mustBelieve(12, "(x:after &&-10 x:before)", 1.00f, intersectionConf, 10)
        ;

//        tester.mustBelieve(cycles, "<<(John, room) --> enter> =\\> (&/, <(John, door) --> open>, /6)>",
//                1.00f, 0.45f,
//                11);
    }



    @Test
    public void temporal_explification()  {
        TestNAR tester = test();
        tester.believe("(<($x, room) --> enter> ==>-5 <($x, door) --> open>)", 0.9f, 0.9f);
        tester.believe("(<($y, door) --> open> ==>-5 <($y, key) --> hold>)", 0.8f, 0.9f);

        tester.mustBelieve(cycles, "(<($1,key) --> hold> ==>+10 <($1,room) --> enter>)", 1.00f, 0.37f);

    }


    @Test
    public void temporal_analogy()  {
        TestNAR tester = test();
        tester.believe("(<($x, door) --> open> ==>+5 <($x, room) --> enter>)",
                0.95f, 0.9f);
        tester.believe("(<($x, room) --> enter> <=>+0 <($x, corridor_100) --> leave>)",
                1.0f, 0.9f);

        tester.mustBelieve(cycles, "(<($1, door) --> open> ==>+5 <($1, corridor_100) --> leave>)", 0.95f, 0.81f);

    }


    @Test public void updating_and_revision()  {
        testTemporalRevision(10, 0.08f, 0.75f, "<(John,key) --> hold>");
    }
    @Test public void updating_and_revision2()  {
        testTemporalRevision(1, 0.33f, 0.30f, "<(John,key) --> hold>");
    }

    void testTemporalRevision(int delay, float freq, float conf, String belief) {
        TestNAR tester = test();
        tester.input(belief + ". :|: %1%");
        tester.inputAt(delay, belief + ". :|: %0%");
        tester.mustBelieve(delay+2, belief,  freq, conf, delay+1);
    }

    @Test public void testSumNeg() {
        //(P ==> M), (M ==> S), neq(S,P), dt(sumNeg) |- (S ==> P), (Belief:Exemplification, Derive:AllowBackward)
        TestNAR tester = test();
        tester.believe("(x ==>+2 y)");
        tester.believe("(y ==>+3 z)");

        tester.mustBelieve(2, "(z ==>-5 x)", 1.00f, 0.45f);

    }

    @Test public void testSum() {
        test()
        .believe("(x ==>+2 y)")
        .believe("(y ==>+3 z)")
        .mustBelieve(2, "(x ==>+5 z)", 1.00f, 0.81f);
    }

    @Test public void testBminT() {
        //(P ==> M), (S ==> M), neq(S,P), dt(bmint) |- (S ==> P), (Belief:Induction, Derive:AllowBackward)
        test()
        .believe("(x ==>+2 y)")
        .believe("(z ==>+3 y)")
        .mustBelieve(5, "(z ==>+1 x)", 1.00f, 0.45f);
    }
    @Test public void testTminB() {
        //(M ==> P), (M ==> S), neq(S,P), dt(tminb) |- (S ==> P), (Belief:Abduction, Derive:AllowBackward)

        test()
        .believe("(y ==>+3 x)")
        .believe("(y ==>+2 z)")
        .mustBelieve(5, "(z ==>+1 x)", 1.00f, 0.45f);
    }

    @Test public void testImplQuery() {
        test()
        .believe("(y ==>+3 x)")
        .input("(y ==>+3 ?x)?")
        .mustAnswer(15, "(y ==>+3 x)", 1.00f, 0.9f, Tense.Eternal);
    }
    @Test public void testImplQueryTense() {
        test()
        .input("(y ==>+3 x). :|:")
        .input("(y ==>+3 ?x)? :|:")
        .mustAnswer(15, "(y ==>+3 x)", 1.00f, 0.90f, Tense.Present);
    }
    @Test public void testImplQueryTenseFuture() {
        test()
        .input("(y ==>+3 x). :\\:")
        .inputAt(45, "(y ==>+3 ?x)? :/:")
        //.mustAnswer(50, "(y ==>+3 x)", 1.00f, 0.74f, 15);
        .mustAnswer(50, "(y ==>+3 x)", 1.00f, 0.9f, -5);
    }

//    @Test public void testImplQuery2() {
//        TestNAR t = test();
//        t.nar.log();
//        t.believe("(y ==>+3 x)")
//        .input("(y ==> x)?")
//        .mustAnswer(15, "(y ==>+3 x)", 1.00f, 0.9f, Tense.Eternal);
//    }

    @Test public void intervalPreserve_and_shift_occurence_corner_case()  {
        TestNAR tester = test();
        tester.input("<s --> S>.");
        tester.inputAt(3, "(<s --> S> &&+3 <z --> Z>). :|:");
        tester.mustBelieve(cycles, "<z --> Z>.", 1.00f, 0.81f /* 0.42? */ , 6);
    }

    @Test
    public void intervalPreserve_and_shift_occurence()  {
        TestNAR tester = test();
        tester.input("S:s.");
        tester.inputAt(10, "(S:s &&+50 (Y:y &&+3 Z:z)). :|:");
        tester.mustBelieve(50, "(Y:y &&+3 Z:z).", 1.00f, 0.81f /* 0.42? */, 60);
    }


    @Test
    public void temporal_deduction()  {
        TestNAR tester = test();
        tester.believe("((($x, room) --> enter) ==>-3 (($x, door) --> open))", 0.9f, 0.9f);
        tester.believe("((($y, door) --> open) ==>-4 (($y, key) --> hold))", 0.8f, 0.9f);

        tester.mustBelieve(cycles, "((($1,room) --> enter) ==>-7 (($1,key) --> hold))", 0.72f, 0.58f);
    }

    @Test
    public void temporal_induction_comparison()  {
        TestNAR tester = test();
        tester.believe("((( $x, door) --> open) ==>+5 (( $x, room) --> enter))", 0.9f, 0.9f);
        tester.believe("((( $y, door) --> open) ==>-4 (( $y, key) --> hold))", 0.8f, 0.9f);

        tester.mustBelieve(cycles, "((($1,key) --> hold) ==>+9 (($1,room) --> enter))", 0.9f, 0.39f);
        tester.mustBelieve(cycles, "((($1,room) --> enter) ==>-9 (($1,key) --> hold))", 0.8f, 0.42f);
        tester.mustBelieve(cycles, "((($1,key) --> hold) <=>+9 (($1,room) --> enter))", 0.73f, 0.44f);

    }

    @Test
    public void inference_on_tense()  {
        TestNAR tester = test();

        tester.input("((($x, key) --> hold) ==>+7 (($x, room) --> enter)).");
        tester.input("<(John, key) --> hold>. :|:");

        tester.mustBelieve(cycles, "<(John,room) --> enter>", 1.00f, 0.81f, 7);
    }
    @Test
    public void inference_on_tense_reverse()  {
        TestNAR tester = test();

        tester.input("((($x, key) --> hold) ==>+7 (($x, room) --> enter)).");
        tester.input("<(John, room) --> enter>. :|:");

        tester.mustBelieve(cycles, "<(John,key) --> hold>", 1.00f, 0.45f, -7);
    }
    @Test
    public void inference_on_tense_reverse_novar()  {
        TestNAR tester = test();

        tester.input("(((John, key) --> hold) ==>+7 ((John, room) --> enter)).");
        tester.input("<(John, room) --> enter>. :|:");

        tester.mustBelieve(cycles, "<(John,key) --> hold>", 1.00f, 0.81f, -7);
    }

    @Test
    public void inference_on_tense_3()  {
        TestNAR tester = test();
        
        tester.believe("(((John,key) --> hold) ==>+3 ((John,room) --> enter))", 1.0f, 0.9f);
        tester.input("<(John,key) --> hold>. :|:");

        tester.mustBelieve(cycles, "<(John,room) --> enter>",
                1.00f, 0.81f, 3);

    }

    @Test
    public void inference_on_tense_4()  {
        TestNAR tester = test();
        tester.believe("(((John,key) --> hold) ==>+3 ((John,room) --> enter))");
        tester.input("<(John,room) --> enter>. :|:");

        tester.mustBelieve(cycles, "<(John,key) --> hold>",
                1.00f, 0.81f, -3);
    }

    @Test
    public void induction_on_events_0()  {
        TestNAR tester = test();

        tester.input("<(John,door) --> open>. :|:");
        tester.inputAt(4, "<(John,room) --> enter>. :|:");

        tester.mustBelieve(cycles, "(((John, room) --> enter) ==>-4 ((John, door) --> open))",
                1.00f, 0.45f, 4);
    }




    @Test
    public void induction_on_events2()  {
        TestNAR tester = test();

        tester.input("<(John,door) --> open>. :|:");
        tester.inputAt(4, "<(John,room) --> enter>. :|:");

        tester.mustBelieve(cycles, "(((John, door) --> open) ==>+4 ((John, room) --> enter))",
                1.00f, 0.45f, 4);

    }

    @Test
    public void induction_on_events3()  {
        TestNAR tester = test();

        tester.input("<(John,door) --> open>. :|:");
        tester.inputAt(4, "<(John,room) --> enter>. :|:");

        tester.mustBelieve(cycles, "(<(John, door) --> open> <=>+4 <(John, room) --> enter>)",
                1.00f, 0.45f,
                4);

    }

    @Test
    public void induction_on_events_with_variable_introduction()  {
        TestNAR tester = test();


        tester.input("<John --> (/,open,_,door)>. :|:");
        tester.inputAt(2, "<John --> (/,enter,_,room)>. :|:");

        //note: this result is reversed (pred equiv direction AND the occurrence time) from the original NAL7 test but its semantics are equivalent
        tester.mustBelieve(cycles,
                "(<$1 --> (/, enter, _, room)> <=>-2 <$1 --> (/, open, _, door)>)",
                1.00f, 0.81f, //0.45f,
                2
        );

    }
    @Test
    public void induction_on_events_with_variable_introduction2()  {
        TestNAR tester = test();


        tester.input("<John --> (/,open,_,door)>. :|:");
        tester.inputAt(2, "<John --> (/,enter,_,room)>. :|:");

        //note: this result is reversed (pred equiv direction AND the occurrence time) from the original NAL7 test but its semantics are equivalent
        tester.mustBelieve(cycles,
                "(<$1 --> (/, enter, _, room)> ==>-2 <$1 --> (/, open, _, door)>)",
                1.00f,
                0.81f, //0.45f,
                2
        );

        //REVERSE:
//        tester.mustBelieve(cycles*4,
//                "(<$1 --> (/, open, _, door)> ==>+2 <$1 --> (/, enter, _, room)>)",
//                1.00f, 0.45f,
//                0
//        );

    }

    @Test public void induction_on_events_composition_pre()  {
        TestNAR tester = test();


        tester.input("(open:(John,door) ==>+5 enter:(John,room)). :|:");


        tester.mustBelieve(cycles, "open:(John,door)",
                1.00f, 0.81f,
                0);

        //[((%1,(%2==>%3),occurr(belief,forward)),(substituteIfUnifies(%3,"$",%2,%1),((Deduction-->Belief),(Induction-->Desire),(ForAllSame-->Order),(Anticipate-->Event))))]".
        tester.mustBelieve(cycles, "enter:(John,room)",
                1.00f, 0.81f,
                5);
    }

    @Test public void induction_on_events_composition1()  {
        compositionTest(1, 5);
    }
    @Test public void induction_on_events_composition2()  {
        compositionTest(1, 7);
    }
    @Test public void induction_on_events_composition3()  {
        compositionTest(4, 3);
    }

    private void compositionTest(int t, int dt) {
        TestNAR tester = test();
        tester.inputAt(t, "<(John,key) --> hold>. :|:");
        tester.inputAt(t, "(open:(John,door) ==>+" + dt + " enter:(John,room)). :|:");

        tester.mustBelieve(cycles, "(hold:(John,key) &&+0 open:(John,door))",
                1.00f, 0.73f,
                t);

        tester.mustBelieve(cycles, "enter:(John,room)",
                1.00f, 0.81f,
                t+dt);

        tester.mustBelieve(cycles, "((hold:(John,key) && open:(John,door)) ==>+" + dt + " enter:(John,room))",
                1.00f, 0.34f,
                t);
    }

    @Test
    public void variable_introduction_on_events()  {
        TestNAR tester = test();

        tester.input("<{t003} --> (/,at,SELF,_)>. :|:");
        tester.inputAt(10, "<{t003} --> (/,on,{t002},_)>. :|:");

        tester.mustBelieve(cycles, "(<#1 --> (/,at,SELF,_)> &&+10 <#1 --> (/,on,{t002},_)>)",
                1.0f, 0.81f,
                10);

    }

//    //TODO: investigate
    @Test
    public void variable_elimination_on_temporal_statements()  {
        TestNAR tester = test();


        tester.input("(on:({t002},#1) &&+0 at:(SELF,#1)). :|:");
        tester.inputAt(10, "((on:($1,#2) &&+0 at:(SELF,#2)) ==>+0 reachable:(SELF,$1)).");

        tester.mustBelieve(cycles, "reachable:(SELF,$1)",
                1.0f, 0.81f, 0);
        tester.mustBelieve(cycles, "reachable:(SELF,{t002})",
                1.0f, 0.81f, 0);

    }
//
    @Test
    public void temporalOrder()  {
        TestNAR tester = test();
        tester.input("(<m --> M> ==>+5 <p --> P>).");
        tester.inputAt(10, "(<s --> S> <=>+0 <m --> M>). %0.9;0.9%");
        tester.mustBelieve(cycles, "(<s --> S> ==>+5 <p --> P>)", 0.90f, 0.73f);


        //(M =/> P), (S <|> M), not_equal(S,P) |- (S =/> P), (Truth:Analogy, Derive:AllowBackward)
    }
//


}
