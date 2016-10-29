package nars.nal.nal3;


import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.test.TestNAR;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.time.Tense.ETERNAL;

@RunWith(Parameterized.class)
public class NAL3Test extends AbstractNALTest {

    public static final int cycles = 550;

    public NAL3Test(Supplier<NAR> b) { super(b); }

    @Parameterized.Parameters(name= "{index}:{0}")
    public static Iterable configurations() {
        return AbstractNALTest.nars(3);
    }


    @Test
    public void compound_composition_two_premises()  {
        TestNAR tester = test();
        tester.believe("<swan --> swimmer>",0.9f,0.9f); //.en("Swan is a type of swimmer.");
        tester.believe("<swan --> bird>", 0.8f, 0.9f); //.en("Swan is a type of bird.");
        tester.mustBelieve(cycles, "<swan --> (|,bird,swimmer)>", 0.98f, 0.81f); //.en("Swan is a type of bird or a type of swimmer.");
        tester.mustBelieve(cycles, "<swan --> (&,bird,swimmer)>",0.72f,0.81f); //.en("Swan is a type of swimming bird.");

    }

    @Test
    public void compound_composition_two_premises2()  {
        TestNAR tester = test();
        tester.believe("<sport --> competition>", 0.9f, 0.9f); //.en("Sport is a type of competition.");
        tester.believe("<chess --> competition>", 0.8f, 0.9f); //.en("Chess is a type of competition.");
        tester.mustBelieve(cycles, "<(|,chess,sport) --> competition>", 0.72f ,0.81f); //.en("If something is either chess or sport, then it is a competition.");
        tester.mustBelieve(cycles, "<(&,chess,sport) --> competition>", 0.98f, 0.81f); //.en("If something is both chess and sport, then it is a competition.");

    }

    @Test
    public void compound_decomposition_two_premises()  {
        TestNAR tester = test();
        tester.believe("<robin --> (|,bird,swimmer)>",1.0f,0.9f); //.en("Robin is a type of bird or a type of swimmer.");
        tester.believe("<robin --> swimmer>", 0.0f, 0.9f); //.en("Robin is not a type of swimmer.");
        tester.mustBelieve(cycles*2, "<robin --> bird>", 1.0f ,0.81f); //.en("Robin is a type of bird.");

    }

    @Test //works, just control related issue (DecomposeNegativeNegativeNegative)
    public void compound_decomposition_two_premises2()  {
        TestNAR tester = test();
        
        tester.believe("<robin --> swimmer>",0.0f,0.9f); //.en("Robin is not a type of swimmer.");
        tester.believe("<robin --> (-,mammal,swimmer)>", 0.0f, 0.9f); //.en("Robin is not a nonswimming mammal.");
        tester.mustBelieve(cycles*3, "<robin --> mammal>", 0.0f ,0.81f); //.en("Robin is not a type of mammal.");

    }

    @Test
    public void set_operations()  {
        TestNAR tester = test();
        tester.believe("<planetX --> {Mars,Pluto,Venus}>",0.9f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.7f,0.9f); //.en("PlanetX is probably Pluto or Saturn.");
        tester.mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.97f ,0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles, "<planetX --> {Pluto}>", 0.63f ,0.81f); //.en("PlanetX is probably Pluto.");

    }

    @Test
    public void set_operationsSetExt_union()  {
        TestNAR tester = test();
        tester.believe("<planetX --> {Mars,Pluto,Venus}>",0.9f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> {Mars,Pluto,Saturn,Venus}>", 0.91f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
    }
    @Test
    public void set_operationsSetExt_unionNeg()  {
        TestNAR tester = test();
        tester.believe("<planetX --> {Earth}>",0.1f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Mars}>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles, "<planetX --> {Earth,Mars}>", 0.19f, 0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
    }


    @Test
    public void set_operationsSetInt_union_2_3_4()  {
        TestNAR tester = test();
        tester.believe("<planetX --> [marsy,earthly,venusy]>",1.0f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles*2, "<planetX --> [marsy,earthly,saturny,venusy]>", 0.1f ,0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
    }

    @Test
    public void set_operationsSetInt_union1_1_2_3()  {
        TestNAR tester = test();
        tester.believe("<planetX --> [marsy,venusy]>",1.0f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> [earthly]>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles*3, "<planetX --> [marsy,earthly,venusy]>", 0.1f ,0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");

    }

    @Test
    public void set_operations2_difference()  {
        TestNAR tester = test();
        tester.believe("<planetX --> {Mars,Pluto,Venus}>",0.9f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> {Pluto,Saturn}>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles*8, "<planetX --> {Mars,Venus}>", 0.81f ,0.81f); //.en("PlanetX is either Mars or Venus.");

    }


    @Test
    public void set_operations3_difference()  {
        TestNAR tester = test();
        tester.believe("<planetX --> [marsy,earthly,venusy]>",1.0f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<planetX --> [earthly,saturny]>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles*8, "<planetX --> [marsy,venusy]>", 0.90f , 0.81f); //.en("PlanetX is either Mars or Venus.");
    }

    @Test
    public void set_operations4()  {
        TestNAR tester = test();
        tester.believe("<[marsy,earthly,venusy] --> planetX>",1.0f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<[earthly,saturny] --> planetX>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles*4, "<[marsy,earthly,saturny,venusy] --> planetX>", 1.0f ,0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles*4, "<[marsy,venusy] --> planetX>", 0.90f ,0.81f); //.en("PlanetX is either Mars or Venus.");

    }

    @Test
    public void set_operations5()  {
        TestNAR tester = test();
        tester.believe("<{Mars,Pluto,Venus} --> planetX>",1.0f,0.9f); //.en("PlanetX is Mars, Pluto, or Venus.");
        tester.believe("<{Pluto,Saturn} --> planetX>", 0.1f, 0.9f); //.en("PlanetX is probably neither Pluto nor Saturn.");
        tester.mustBelieve(cycles*4, "<{Mars,Pluto,Saturn,Venus} --> planetX>", 0.1f ,0.81f); //.en("PlanetX is Mars, Pluto, Saturn, or Venus.");
        tester.mustBelieve(cycles*4, "<{Mars,Venus} --> planetX>", 0.90f ,0.81f); //.en("PlanetX is either Mars or Venus.");

    }

    @Test
    public void composition_on_both_sides_of_a_statement()  {
        TestNAR tester = test();
        tester.believe("<bird --> animal>",0.9f,0.9f); //.en("Bird is a type of animal.");
        tester.ask("<(&,bird,swimmer) --> (&,animal,swimmer)>"); //.en("Is a swimming bird a type of swimming animal?");
        tester.mustBelieve(cycles, "<(&,bird,swimmer) --> (&,animal,swimmer)>", 0.90f ,0.73f); //.en("A swimming bird is probably a type of swimming animal.");

    }

    @Test
    public void composition_on_both_sides_of_a_statement_2()  {
        TestNAR tester = test();
        tester.believe("<bird --> animal>",0.9f,0.9f); //.en("Bird is a type of animal.");
        tester.ask("<(|,bird,swimmer) --> (|,animal,swimmer)>"); //.en("Is a swimming bird a type of swimming animal?");
        tester.mustBelieve(cycles*2, "<(|,bird,swimmer) --> (|,animal,swimmer)>", 0.90f ,0.73f); //.en("A swimming bird is probably a type of swimming animal.");

        /*<bird --> animal>. %0.9;0.9%
                <(|,bird,swimmer) --> (|,animal,swimmer)>?*/
    }

    @Test
    public void composition_on_both_sides_of_a_statement2()  {
        TestNAR tester = test();
        tester.believe("<bird --> animal>",0.9f,0.9f); //.en("Bird is a type of animal.");
        tester.ask("<(-,swimmer,animal) --> (-,swimmer,bird)>"); //.en("Is a nonanimal swimmer a type of a nonbird swimmer?");
        tester.mustBelieve(cycles, "<(-,swimmer,animal) --> (-,swimmer,bird)>", 0.90f ,0.73f); //.en("A nonanimal swimmer is probably a type of nonbird swimmer.");

    }

    @Test
    public void composition_on_both_sides_of_a_statement2_2()  {
        TestNAR tester = test();
        tester.believe("<bird --> animal>",0.9f,0.9f); //.en("Bird is a type of animal.");
        tester.ask("<(~,swimmer,animal) --> (~,swimmer,bird)>"); //.en("Is a nonanimal swimmer a type of a nonbird swimmer?");
        tester.mustBelieve(cycles, "<(~,swimmer,animal) --> (~,swimmer,bird)>", 0.90f ,0.73f); //.en("A nonanimal swimmer is probably a type of nonbird swimmer.");

    }

    @Test
     public void compound_composition_one_premise()  {
        TestNAR tester = test();
        tester.believe("<swan --> bird>",0.9f,0.9f); //.en("Swan is a type of bird.");
        tester.ask("<swan --> (|,bird,swimmer)>"); //.en("Is a swan a type of bird or swimmer?");
        tester.mustBelieve(cycles, "<swan --> (|,bird,swimmer)>", 0.90f ,0.73f); //.en("A swan is probably a type of bird or swimmer.");

    }

    @Test
     public void compound_composition_one_premise2()  {
        TestNAR tester = test();
        tester.believe("<swan --> bird>",0.9f,0.9f); //.en("Swan is a type of bird.");
        tester.ask("<(&,swan,swimmer) --> bird>"); //.en("Is swimming swan a type of bird?");
        tester.mustBelieve(cycles, "<(&,swan,swimmer) --> bird>", 0.90f ,0.73f); //.en("Swimming swan is a type of bird.");

    }

    @Test
    public void compound_composition_one_premise3()  {
        TestNAR tester = test();
        
        tester.believe("<swan --> bird>",0.9f,0.9f); //.en("Swan is a type of bird.");
        tester.askAt(cycles/2,"<swan --> (-,swimmer,bird)>"); //.en("Is swan a type of nonbird swimmer?");
        tester.mustBelieve(cycles, "<swan --> (-,swimmer,bird)>", 0.10f ,0.73f); //.en("A swan is not a type of nonbird swimmer.");

    }

    @Test
    public void compound_composition_one_premise4()  {
        TestNAR tester = test();
        tester.believe("<swan --> bird>",0.9f,0.9f); //.en("Swan is a type of bird.");
        tester.askAt(cycles/2,"<(~,swimmer, swan) --> bird>"); //.en("Is being bird what differ swimmer from swan?");
        tester.mustBelieve(cycles*2, "<(~,swimmer, swan) --> bird>", 0.10f, 0.73f); //.en("What differs swimmer from swan is not being bird.");

    }

    @Test
    public void compound_decomposition_one_premise()  {
        TestNAR tester = test();
        tester.believe("<robin --> (-,bird,swimmer)>", 0.9f, 0.9f); //.en("Robin is a type of nonswimming bird.");
        tester.mustBelieve(cycles, "<robin --> bird>", 0.90f ,0.73f); //.en("Robin is a type of bird.");

    }

    @Test
    public void compound_decomposition_one_premise2()  {
        TestNAR tester = test();
        tester.believe("<(|, boy, girl) --> youth>", 0.9f, 0.9f); //.en("Boys and gials are youth.");
        tester.mustBelieve(cycles*3, "<boy --> youth>", 0.90f ,0.73f); //.en("Boys are youth.");

    }

    @Test
    public void compound_decomposition_one_premise3()  {
        TestNAR tester = test();
        tester.believe("<(~, boy, girl) --> [strong]>", 0.9f, 0.9f); //.en("What differs boys from girls are being strong.");
        tester.mustBelieve(cycles, "<boy --> [strong]>", 0.90f ,0.73f); //.en("Boys are strong.");

    }
    
    @Test public void testNoCommonSubterm1() {
        test()
            .believe("a:b")
            .believe("b:c")
            .mustNotOutput(200, "(((a~c)~c)-->b)", '.', ETERNAL);

    }

    @Test public void testArity1_Decomposition_IntersectExt() {
        //(M --> S), (M --> (&,S,A..+)) |- (M --> (&,A..+)), (Belief:DecomposePositiveNegativeNegative)
        test()
                //.log()
                .believe("(a-->b)")
                .believe("(a-->(&,b,c))", 0f, 0.9f)
                .mustBelieve(cycles, "(a-->c)", 0f, 0.81f, ETERNAL);

    }
    @Test public void testArity1_Decomposition_IntersectExt2() {
        //(M --> S), (M --> (&,S,A..+)) |- (M --> (&,A..+)), (Belief:DecomposePositiveNegativeNegative)
        test()
                ////.log()
                .believe("(a-->b)")
                .believe("(a-->(&,b,c))")
                .mustBelieve(cycles, "(a-->c)", 1f, 0.81f, ETERNAL);

    }

    @Test public void testArity1_Decomposition_IntersectInt() {
        //(M --> S), (M --> (|,S,A..+)) |- (M --> (|,A..+)), (Belief:DecomposeNegativePositivePositive)

        test()
                //.log()
                .believe("(a-->b)", 0.25f, 0.9f)
                .believe("(a-->(|,b,c))", 0.25f, 0.9f)
                .mustBelieve(cycles, "(a-->c)", 0.19f, 0.15f, ETERNAL);

    }

    @Test public void testIntersectDiffUnionOfCommonSubterms() {
        TestNAR x = test();

        x
            .believe("<{x,y}-->c>")
            .believe("<{x,z}-->c>")
            .mustBelieve(cycles, "<{x,y,z}-->c>", 1f, 0.81f) //union
            .mustBelieve(cycles, "<{x}-->c>", 1f, 0.73f) //intersect
            .mustBelieve(cycles, "<{y}-->c>", 1f, 0.73f) //difference
            .mustBelieve(cycles, "<{y}-->c>", 0f, 0.73f) //difference
            .mustBelieve(cycles, "<{z}-->c>", 0f, 0.73f) //difference
        //these are probably ok:
            //.mustNotOutput(cycles,"<{x}-->c>", '.', 0, 0, 0.5f, 1, ETERNAL) //contradiction of input above conf=0.5
            //.mustNotOutput(cycles,"<{x,y}-->c>", '.', 0, 0, 0.5f, 1, ETERNAL) //contradiction of input above conf=0.5
            //.mustNotOutput(cycles,"<{x,z}-->c>", '.', 0, 0, 0.5f, 1, ETERNAL) //contradiction of input above conf=0.5
        ;

    }
}

