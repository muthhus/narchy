package nars.nal.nal1;

import nars.NAR;
import nars.Narsese;
import nars.nal.AbstractNALTest;
import nars.util.signal.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.nal.Tense.ETERNAL;

@RunWith(Parameterized.class)
public class NAL1Test extends AbstractNALTest {

    final int withinCycles = 64;

    public NAL1Test(Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Iterable<Supplier<NAR>> configurations() {
        return AbstractNALTest.nars(1, true, true);
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
    public void revision() throws Narsese.NarseseException {

        String belief = "<bird --> swimmer>";

        test()
                //.log()
                .mustBelieve(3, belief, 0.87f, 0.91f)
                .believe(belief)                 //.en("bird is a type of swimmer.");
                .believe(belief, 0.10f, 0.60f)                 //.en("bird is probably not a type of swimmer."); //.en("bird is very likely to be a type of swimmer.");*/
                ;
    }


    @Test
    public void deduction() throws Narsese.NarseseException {

        test().believe("<bird --> animal>")
                /*.en("bird is a type of animal.")
                .es("bird es un tipo de animal.")
                .de("bird ist eine art des animal.");*/
                .believe("<robin --> bird>")
                        //.en("robin is a type of bird.");
                .mustBelieve(50, "<robin --> animal>", 0.81f);
    }

    @Test
    public void abduction() throws Narsese.NarseseException {

        int time = withinCycles;

        test().mustBelieve(time, "<sport --> chess>", 1.0f, 0.42f)
              /*  .en("I guess sport is a type of chess.")
                .en("sport is possibly a type of chess.")
                .es("es posible que sport es un tipo de chess.");*/
                .log()
                .believe("<sport --> competition>")
                        //.en("sport is a type of competition.");
                .believe("<chess --> competition>", 0.90f, 0.9f)
                .mustBelieve(time, "<chess --> sport>", 0.90f, 0.45f);
                //.en("I guess chess is a type of sport");
    }

    @Test
    public void abduction2() throws Narsese.NarseseException {
        
        /*
        <swan --> swimmer>. %0.9;0.9%
        <swan --> bird>.
         */
        //(A --> B), (A --> C), neq(B,C) |- (C --> B), (Belief:Abduction, Desire:Weak, Derive:AllowBackward)

        test()
            .log()
            .believe("<swan --> swimmer>", 0.90f, 0.9f) //.en("Swan is a type of swimmer.");
            .believe("<swan --> bird>") //.en("Swan is a type of bird.");
            .mustBelieve(withinCycles, "<bird --> swimmer>", 0.90f, 0.45f) //.en("I guess bird is a type of swimmer.");
            .mustNotOutput(withinCycles, "<bird --> swimmer>", '.', 1f, 1f, 0.41f, 0.43f, ETERNAL) //test for correct ordering of the premise wrt truth value function
            .mustBelieve(withinCycles, "<swimmer --> bird>", 1.0f, 0.42f)
            .mustNotOutput(withinCycles, "<swimmer --> bird>", '.', 0.9f, 0.9f, 0.44f, 0.46f, ETERNAL) //test for correct ordering of the premise wrt truth value function
            ;
    }

    @Test public void induction() {
        //(A --> C), (B --> C), neq(A,B) |- (B --> A), (Belief:Induction, Desire:Weak, Derive:AllowBackward)
        test()
                .believe("<parakeet --> bird>", 0.90f, 0.9f) //.en("Swan is a type of swimmer.");
                .believe("<pteradactyl --> bird>") //.en("Swan is a type of bird.");
                .mustBelieve(withinCycles, "<pteradactyl --> parakeet>", 1, 0.42f)
        ;
    }


    @Test
    public void exemplification() throws Narsese.NarseseException {

        test()
            //.debug()
            .believe("<robin --> bird>")
            .believe("<bird --> animal>")
            .mustOutput(withinCycles, "<animal --> robin>. %1.00;0.4475%");
    }


    @Test
    public void conversion() throws Narsese.NarseseException {

        TestNAR test = test();
        test.believe("<bird --> swimmer>")
            .ask("<swimmer --> bird>") //.en("Is swimmer a type of bird?");
            .mustOutput(withinCycles, "<swimmer --> bird>. %1.00;0.47%");
    }




    @Test
    public void backwardInference() throws Narsese.NarseseException {
        long time = withinCycles;


        TestNAR test = test();
        //test.nar.log();
        test
                .believe("<bird --> swimmer>", 1.0f, 0.8f)
                .ask("<?1 --> swimmer>")
                .mustOutput(time, "<?1 --> bird>?") //.en("What is a type of bird?");
                .mustOutput(time, "<bird --> ?1>?") //.en("What is the type of bird?");
        ;
    }




}
