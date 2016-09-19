package nars.nal.nal7;

import nars.NAR;
import nars.nar.Default;
import nars.op.mental.Anticipate;
import nars.test.TestNAR;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Created by me on 1/21/16.
 */
@Ignore
public class AnticipationTest {

    @Test
    public void testAnticipation1() {
        /*
        <(&/,<a --> b>,+3) =/> <b --> c>>.
        <a --> b>. :|:
        25
        ''outputMustContain('<(&/,<a --> b>,+3) =/> <b --> c>>. :\: %0.00;0.45%')
        ''outputMustContain('<(&/,<a --> b>,+3) =/> <b --> c>>. :\: %0.92;0.91%')
        */
        NAR d = new Default();
        new Anticipate(d);

        TestNAR t = new TestNAR(d);
        t.input("(<a --> b> ==>+16 <b --> c>).");
        t.input("<a --> b>. :|:");
        //t.mustBelieve(55, "(--,(<a --> b> ==>+8 <b --> c>))", 1f, 0.45f, 0); // :\:
        t.mustBelieve(55, "(--,(b-->c)).", 1f, 0.9f, 8);
        //t.mustBelieve(55, "(<a --> b> ==>+8 <b --> c>)", 0.92f, 0.91f, 0);  // :\:
        t.test();
    }

//    @Test
//    public void testAnticipation2() {
//        /*
//        <(&/,<a --> b>,+3) =/> <b --> c>>.
//        <a --> b>. :|:
//        5
//        <b --> c>. :|:
//        20
//        ''outputMustContain('<(&/,<a --> b>,+3) =/> <b --> c>>. :|: %1.00;0.91%')
//        */
//        NAR d = new Default();
//        new Anticipate(d);
//
//        TestNAR t = new TestNAR(d);
//        t.input("(<a --> b> ==>+8 <b --> c>).");
//        t.input("<a --> b>. :|:");
//        t.inputAt(5, "<b --> c>. :|:");
//        t.mustBelieve(55, "(<a --> b> ==>+8 <b --> c>)", 1f, 0.90f, 5); // :\:
//        t.test();
//
//    }

}
