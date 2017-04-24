package nars.nal.multistep;

import nars.NAR;
import nars.nal.AbstractNALTest;
import nars.test.TestNAR;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

import static nars.time.Tense.ETERNAL;

/**
 * see bAbl.nal
 */
@RunWith(Parameterized.class)
public class bAblTests extends AbstractNALTest {

    public bAblTests (Supplier<NAR> b) {
        super(b);
    }

    @Parameterized.Parameters(name = "{index}:{0}")
    public static Iterable<Supplier<NAR>> configurations() {
        return AbstractNALTest.nars(8);
    }

    @Ignore @Test
    public void test1() throws nars.Narsese.NarseseException {

        test()
                .believe("in({john},{playground})") //john is in the playground.
                .believe("in(bob,office)") //Bob is in the office.
                .ask("in({john},{?where})") //Where is john?
                .mustBelieve(100, "in({john},{playground})", 1f, 0.73f) //note that the 0.90 conf result should have been provided as an answer to the question, not as a belief. the 0.73 conf version is a side effect so we'll test for that at least
                ;


        ////(1) Basic Factoid QA with Single Supporting Fact
        ////john is in the playground.
        //<(*,{john},{playground}) --> isin>.
        ////Bob is in the office.
        //<(*,bob,office) --> isin>.
        ////Where is john?
        //<(*,{john},{?where}) --> isin>?
        ////A:playground
        ////Answer <(*,{john},{playground}) --> isin>. %1.00;0.90%
    }

    @Test
    public void test2() {

        //(2) Factoid QA with Two Supporting Facts
        //background knowledge (multiple input for priority boost to have the answer faster ^^)
        //if something is picked, it means that the object which is picked is where the person is

        TestNAR t = test()
                //.log()
                .believe("((pick(#Person,$Object) &&+0 isIn(#Person,$Place)) ==>+0 isIn($Object,$Place))")
                .inputAt(0, "isIn(john,playground).") ////John is in the playground.
                .inputAt(0, "isIn(bob,office).") ////Bob is in the office.
                .inputAt(0, "pick(john,football).") ////John picked up the football.
                .inputAt(0, "isIn(bob,kitchen).") ////Bob went to the kitchen.
                .inputAt(0, "isIn(football,#where)?") ////Where is the football?
                .mustBelieve(2000, "isIn(football,playground)",
                        1f, 0.40f, ETERNAL); ////A: playground

    }

    /** TODO find a better problem representation, this one isnt good */
    @Ignore @Test public void test19() throws nars.Narsese.NarseseException {

        //(19) Path Finding
        TestNAR t = test();
        t.nar.termVolumeMax.setValue(40); //larger than default

        //t.log();

               t.believe("((&&, start($1,$2), at( $1,$B,$C), at( $B,$2,$C2) ) ==> ( path( id,$C,id,$C2)   && chunk( $1,$2,$B) ))")
                .believe("((&&, start($1,$2), at( $1,$B,$C), at( $2,$B,$C2) ) ==> ( path( id,$C,neg,$C2)  && chunk( $1,$2,$B) ))")
                .believe("((&&, start($1,$2), at( $B,$1,$C), at( $B,$2,$C2) ) ==> ( path( neg,$C,id,$C2)  && chunk( $1,$2,$B) ))")
                .believe("((&&, start($1,$2), at( $B,$1,$C), at( $2,$B,$C2) ) ==> ( path( neg,$C,neg,$C2) && chunk( $1,$2,$B) ))")
                .believe("at(kitchen,hallway,south)") //The kitchen is north of the hallway.
                .believe("at(den,hallway,west)") //The den is east of the hallway.
                .believe("start(den,kitchen)") //How do you go from den to kitchen?
                .ask("<?what --> path>")
                .mustBelieve(2500, "path(id,west,neg,south)", 1f, 0.35f); //A:west,north


    }



}
