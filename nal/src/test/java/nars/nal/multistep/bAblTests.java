package nars.nal.multistep;

import nars.NAR;
import nars.Param;
import nars.nal.AbstractNALTest;
import nars.test.TestNAR;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.function.Supplier;

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

    @Test @Ignore
    public void test1() {

        test()
                .believe("<(*,{john},{playground}) --> isin>") //john is in the playground.
                .believe("<(*,bob,office) --> isin>") //Bob is in the office.
                .ask("<(*,{john},{?where}) --> isin>")
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

        test()
            //.log()
            .believe("((&&, pick:($Person,$Object), isIn:($Person,$Place)) ==>+0 isIn:($Object,$Place))")
            .inputAt(0,"isIn:({john},{playground}). :|:") ////John is in the playground.
            .inputAt(0,"isIn:({bob},{office}). :|:") ////Bob is in the office.
            .inputAt(0,"pick:({john},{football}). :|:") ////John picked up the football.
            .inputAt(0,"isIn:({bob},{kitchen}). :|:") ////Bob went to the kitchen.
            .askAt  (0,"isIn:({football},{?where})") ////Where is the football?
            .mustBelieve(1500, "isIn:({football},{playground})", 1f, 0.73f, 0); ////A: playground

    }

    @Ignore @Test public void test19() {

        //(19) Path Finding
        TestNAR t = test();
        t.nar.compoundVolumeMax.setValue(40); //larger than default

        //.log()
               t.believe("<(&&, <( $1,$2) --> start>, <( $1,$B,$C) --> at>, <( $B,$2,$C2) --> at>) ==> (&&, <( id,$C,id,$C2) --> path>, <( $1,$2,$B) --> chunk>)>")
                .believe("<(&&, <( $1,$2) --> start>, <( $1,$B,$C) --> at>, <( $2,$B,$C2) --> at>) ==> (&&, <( id,$C,neg,$C2) --> path>, <( $1,$2,$B) --> chunk>)>")
                .believe("<(&&, <( $1,$2) --> start>, <( $B,$1,$C) --> at>, <( $B,$2,$C2) --> at>) ==> (&&, <( neg,$C,id,$C2) --> path>, <( $1,$2,$B) --> chunk>)>")
                .believe("<(&&, <( $1,$2) --> start>, <( $B,$1,$C) --> at>, <( $2,$B,$C2) --> at>) ==> (&&, <( neg,$C,neg,$C2) --> path>, <( $1,$2,$B) --> chunk>)>")
                .believe("<( kitchen,hallway,south) --> at>") //The kitchen is north of the hallway.
                .believe("<( den,hallway,west) --> at>") //The den is east of the hallway.
                .believe("<( den,kitchen) --> start>") //How do you go from den to kitchen?
                //.ask("<?what --> path>")
                .mustBelieve(2500, "<( id,west,neg,south) --> path>", 1f, 0.35f); //A:west,north


    }



}
