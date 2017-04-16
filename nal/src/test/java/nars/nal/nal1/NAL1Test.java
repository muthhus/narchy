package nars.nal.nal1;

import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.test.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.Op.BELIEF;
import static nars.time.Tense.ETERNAL;

@RunWith(Parameterized.class)
public class NAL1Test extends AbstractNALTest {

    final int CYCLES = 250;

    public NAL1Test(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Iterable<Supplier<NAR>> configurations() {
        return AbstractNALTest.nars(1);
    }
//
//                new Supplier[]{
//                //{new Default()},
////                {new Default().setInternalExperience(null)},
//
//                //{new NewDefault()},
//                { () -> new Default().nal(1)},
//                { () -> new Default().nal(2)},
//                { () -> new Default() },
//
////                {new Default().level(2)}, //why does this need level 2 for some tests?
////                {new DefaultMicro().level(2) },
////                {new Classic()},
//
//                { () -> new DefaultAlann(48)},
//
//                //{new Solid(1, 48, 1, 2, 1, 3).level(1)},
//                //{new Solid(1, 64, 1, 2, 1, 3).level(2)},
//        });
//}

//
//    @Before
//    public void setup() {
//
//        //tester.setTemporalTolerance(50 /* cycles */);
//    }


    @Test
    public void revision()  {

        String belief = "<bird --> swimmer>";

        test()

                .mustBelieve(4, belief, 0.87f, 0.91f)
                .believe(belief)                 //.en("bird is a type of swimmer.");
                .believe(belief, 0.10f, 0.60f)                 //.en("bird is probably not a type of swimmer."); //.en("bird is very likely to be a type of swimmer.");*/
                ;
    }


    @Test
    public void deduction()  {

        test().believe("<bird --> animal>")
                /*.en("bird is a type of animal.")
                .es("bird es un tipo de animal.")
                .de("bird ist eine art des animal.");*/
                .believe("<robin --> bird>")
                        //.en("robin is a type of bird.");
                .mustBelieve(250, "<robin --> animal>", 0.81f);
    }

    @Test
    public void abduction()  {

        int time = CYCLES;

//                .believe("<sport --> competition>")
//                .believe("<chess --> competition>", 0.90f, Global.DEFAULT_JUDGMENT_CONFIDENCE)
//                 mustBelieve(time, "<sport --> chess>", 1.0f, 0.42f)
//                .mustBelieve(time, "<chess --> sport>", 0.90f, 0.45f)
//        //.en("chess is a type of competition.");

        test()
                
                .believe("<sport --> competition>", 1f, 0.9f)
                .believe("<chess --> competition>", 0.90f, 0.9f)
                .mustBelieve(time, "<chess --> sport>", 1f, 0.42f)
                .mustBelieve(time, "<sport --> chess>", 0.9f, 0.45f);

                //.en("I guess chess is a type of sport");
    }

    @Test
    public void abduction2()  {
        
        /*
        <swan --> swimmer>. %0.9;0.9%
        <swan --> bird>.
         */
        //(A --> B), (A --> C), neq(B,C) |- (C --> B), (Belief:Abduction, Desire:Weak, Derive:AllowBackward)

        test()
            .believe("<swan --> swimmer>", 0.90f, 0.9f) //.en("Swan is a type of swimmer.");
            .believe("<swan --> bird>") //.en("Swan is a type of bird.");
            .mustBelieve(CYCLES, "<bird --> swimmer>", 0.90f, 0.45f) //.en("I guess bird is a type of swimmer.");
            .mustNotOutput(CYCLES, "<bird --> swimmer>", BELIEF, 1f, 1f, 0.41f, 0.43f, ETERNAL) //test for correct ordering of the premise wrt truth value function
            .mustBelieve(CYCLES, "<swimmer --> bird>", 1.0f, 0.42f)
            .mustNotOutput(CYCLES, "<swimmer --> bird>", BELIEF, 0.9f, 0.9f, 0.44f, 0.46f, ETERNAL) //test for correct ordering of the premise wrt truth value function
            ;
    }



    @Test public void induction() {
        //(A --> C), (B --> C), neq(A,B) |- (B --> A), (Belief:Induction, Desire:Weak, Derive:AllowBackward)
        test()

                .believe("<parakeet --> bird>", 0.90f, 0.9f) //.en("Swan is a type of swimmer.");
                .believe("<pteradactyl --> bird>") //.en("Swan is a type of bird.");
                .mustBelieve(CYCLES, "<parakeet --> pteradactyl>", 1, 0.42f)
                .mustBelieve(CYCLES, "<pteradactyl --> parakeet>", 0.9f, 0.45f)
        ;
    }


    @Test
    public void exemplification()  {

        test()

            .believe("<robin --> bird>")
            .believe("<bird --> animal>")
            .mustOutput(CYCLES, "<animal --> robin>. %1.00;0.4475%");
    }


    @Test
    public void conversion()  {

        TestNAR test = test();
        test.believe("<bird --> swimmer>")
            .ask("<swimmer --> bird>") //.en("Is swimmer a type of bird?");
            .mustOutput(CYCLES, "<swimmer --> bird>. %1.00;0.47%");
    }




    @Test
    public void backwardInference()  {

        test()

                .believe("<bird --> swimmer>", 1.0f, 0.8f) //Bird is a type of swimmer
                .ask(    "<?1 --> swimmer>") //What is a type of swimmer?
                .mustOutput(125, "<?1 --> bird>?") //.en("What is a type of bird?");
                .mustOutput(125, "<bird --> ?1>?") //.en("What is the type of bird?");
        ;
    }


}
