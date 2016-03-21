package nars.nal.multistep;

import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.util.experiment.DeductiveChainTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.util.experiment.DeductiveChainTest.inh;

//import static nars.util.meter.experiment.DeductiveChainTest.inh;

@RunWith(Parameterized.class)
public class PatrickNLP extends AbstractNALTest {

    public PatrickNLP(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Iterable<Supplier<NAR>> configurations() {
        return AbstractNALTest.nars(1, true);
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
            .log()
            .believe("((<$1 --> (/,REPRESENT,_,$3)> && <$2 --> (/,REPRESENT,_,$4)>) ==> REPRESENT:(($1,$2),($3,$4)))") ////the word fish represents the concept FOOD
            .believe("<cat --> (/,REPRESENT,_,ANIMAL)>")
            .believe("<eats --> (/,REPRESENT,_,EATING)>")
            .ask("REPRESENT:((cat,eats),?what)")
            .mustBelieve(15, "REPRESENT:((cat,eats),(ANIMAL,EATING))", 1f, 0.73f);

    }


}
