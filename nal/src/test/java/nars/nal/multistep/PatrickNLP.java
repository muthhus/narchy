package nars.nal.multistep;

import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.util.experiment.DeductiveChainTest;
import nars.util.signal.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.util.experiment.DeductiveChainTest.inh;

@RunWith(Parameterized.class)
public class PatrickNLP extends AbstractNALTest {

    public PatrickNLP(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Iterable<Supplier<NAR>> configurations() {
        return AbstractNALTest.nars(8, true);
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
        test()
            //.log()
            .believe("(((/,REPRESENT,_,$3):$1 && (/,REPRESENT,_,$4):$2) ==> REPRESENT:(($1,$2),($3,$4)))")
            .believe("(/,REPRESENT,_,ANIMAL):cat")
            .believe("(/,REPRESENT,_,EATING):eats")
            .askAt(450,"REPRESENT:((eats,cat),?what)")
            .mustBelieve(500, "REPRESENT:((cat,eats),(ANIMAL,EATING))", 1f, 0.73f);

    }

    @Test public void testExample1a() {
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
        TestNAR t = test();
        t.nar.DEFAULT_JUDGMENT_PRIORITY = 0.1f;
        t.nar.DEFAULT_QUESTION_PRIORITY = 0.1f;
        t        .log()
                .believe("(((/,REPR,_,$3):$1 && (/,REPR,_,$4):$2) ==> REPR:{($1,$3),($2,$4)})")
                .believe("(/,REPR,_,ANIMATING):cat")
                .believe("(/,REPR,_,EATING):eats")
                .askAt(75,"REPR:{(cat,eats),?what}")
                .mustBelieve(100, "REPR:{(eats,EATING),(cat,ANIMATING)}", 1f, 0.81f);
        

    }


}
