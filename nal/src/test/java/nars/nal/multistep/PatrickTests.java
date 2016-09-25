package nars.nal.multistep;

import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.test.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

/** see Natural_Language_Processing2.md */
@RunWith(Parameterized.class)
public class PatrickTests extends AbstractNALTest {

    public PatrickTests(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Iterable<Supplier<NAR>> configurations() {
        return AbstractNALTest.nars(8);
    }


    @Test public void testExample1() {
        /*
        ////Example 1, REPRESENT relation with lifting
        //the whole can sometimes be understood by understanding what the parts represent (lifting)

        <(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
        //the word fish represents the concept FOOD
        <cat --> (/,REPRESENT,_,ANIMAL)>.
        //the word eats represents the concept EATING
        <eats --> (/,REPRESENT,_,EATING)>.

        //what does cat eats represent?
        <(*,(*,cat,eats),?what) --> REPRESENT>?
        //RESULT: <(*,(*,cat,eats),(*,ANIMAL,EATING)) --> REPRESENT>. %1.00;0.73%
         */
        TestNAR tt = test();
        tt
            //.log()
            .believe("(((/,REPRESENT,_,$3):$1 && (/,REPRESENT,_,$4):$2) ==> REPRESENT:(($1,$2),($3,$4)))")
            .believe("(/,REPRESENT,_,ANIMAL):cat")
            .believe("(/,REPRESENT,_,EATING):eats")

            //should WORK with either of these two questions:
            //.askAt(1250,"REPRESENT:((eats,cat),?what)")
            .askAt(1250,"REPRESENT:((cat,eats),(?x, ?y))")

            //.mustBelieve(2500, "REPRESENT:((cat,eats),(ANIMAL,EATING))", 1f, 0.73f);
            .mustBelieve(2500, "REPRESENT:((eats, cat),(EATING,ANIMAL))", 1f, 0.73f);

    }

//    @Test public void testExample1a() {
//        /*
//        ////Example 1, REPRESENT relation with lifting
//        //the whole can sometimes be understood by understanding what the parts represent (lifting)
//
//        <(&&,<$1 --> (/,REPRESENT,_,$3)>,<$2 --> (/,REPRESENT,_,$4)>) ==> <(*,(*,$1,$2),(*,$3,$4)) --> REPRESENT>>.
//        //the word fish represents the concept FOOD
//        <cat --> (/,REPRESENT,_,ANIMAL)>.
//        //the word eats represents the concept EATING
//        <eats --> (/,REPRESENT,_,EATING)>.
//
//        //what does cat eats represent?
//        <(*,(*,cat,eats),?what) --> REPRESENT>?
//        //RESULT: <(*,(*,cat,eats),(*,ANIMAL,EATING)) --> REPRESENT>. %1.00;0.73%
//         */
//        TestNAR t = test();
//        //t.nar.DEFAULT_JUDGMENT_PRIORITY = 0.1f;
//        //t.nar.DEFAULT_QUESTION_PRIORITY = 0.1f;
//        t
//                .log()
//                .believe("(($1:(/,REPR,_,$3) && $2:(/,REPR,_,$4)) ==> REPR:(($1,$3)<->($2,$4)))")
//                .believe("cat:(/,REPR,_,ANIMATING)")
//                .believe("eats:(/,REPR,_,EATING)")
//
//                //should WORK with either of these two questions:
//                //.askAt(100,"REPR:((cat,ANIMATING)<->?what)")
//                .askAt(10000,"REPR:((cat,ANIMATING)<->(?x, ?y))")
//
//                .mustBelieve(11250, "REPR:((eats,EATING)<->(cat,ANIMATING))", 1f, 0.73f);
//
//
//    }


    @Test public void testToothbrush() {
        /*
        <(*,toothbrush,plastic) --> made_of>.
        <(&/,<(*,$1,plastic) --> made_of>,<({SELF},$1) --> op_lighter>) =/> <$1 --> [heated]>>.
        <<$1 --> [heated]> =/> <$1 --> [melted]>>.
        <<$1 --> [melted]> <|> <$1 --> [pliable]>>.
        <(&/,<$1 --> [pliable]>,<({SELF},$1) --> op_reshape>) =/> <$1 --> [hardened]>>.
        <<$1 --> [hardened]> =|> <$1 --> [unscrewing]>>.
        <toothbrush --> object>.
        (&&,<#1 --> object>,<#1 --> [unscrewing]>)!

            >> lighter({SELF},$1) instead of <({SELF},$1) --> op_lighter>

        */


        TestNAR tt = test();



        tt.input("made_of:(toothbrush,plastic).",
                "( ( made_of:($1, plastic) &&+10 lighter({SELF}, $1) ) ==>+10 <$1 --> [heated]>).",
                "(<$1 --> [heated]> ==>+10 <$1 --> [melted]>).",
                "(<$1 --> [melted]> <=>+0 <$1 --> [pliable]>).",
                "(( <$1 --> [pliable]> &&+0 reshape({SELF},$1)) ==>+10 <$1 --> [hardened]>).",
                "(<$1 --> [hardened]> ==>+0 <$1 --> [unscrewing]>).",
                "<toothbrush --> here>. :|:", //there is a toothbrush here NOW

                "( <#1 --> here> &&+0 <#1 --> [unscrewing]>)!" //simultaneous
                    //"( <#1 --> here> && <#1 --> [unscrewing]>)! :|:" //alternate: NOW

                );

        tt.mustOutput(0, 4000, "lighter({SELF}, toothbrush)", '!', 1f, 1f,
                0.3f, 1f, //at least some confidence
                /*@*/ -30L);  //is this correct time? might be off by +/-10 , will check


    }

}
