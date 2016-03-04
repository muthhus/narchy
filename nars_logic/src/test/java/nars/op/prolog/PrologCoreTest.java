package nars.op.prolog;

import alice.tuprolog.Agent;
import alice.tuprolog.MalformedGoalException;
import nars.NAR;
import nars.nar.Default;
import nars.op.sys.PrologCore;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 3/3/16.
 */
public class PrologCoreTest {

    @Test
    public void testPrologCoreBeliefAssertion() throws MalformedGoalException {
        NAR n = new Default();
        PrologCore p = new PrologCore(n);
        n.input("a:b.");
        n.input("(--, c:d).");
        n.step();

        assertTrue(p.isTrue("'-->'(b,a)."));
        assertFalse(p.isTrue("'-->'(a,b)."));
        assertTrue(p.isTrue("not('-->'(d,c))."));
        assertFalse(p.isTrue("'-->'(d,c)."));

    }

//    boolean prologAnswered = false;
//
//
//
//    @Test
//    public void testMultistep() throws Exception {
//        boolean prolog = true;
//        //boolean showOutput = false;
//        Global.DEBUG = true;
//
//        NAR nar = new NAR( new Default().setInternalExperience(null) );
//
//        NARPrologMirror p = new NARPrologMirror(nar, 0.1f, true, true, true) {
//
//            @Override
//            protected void onQuestion(Sentence s) {
//                super.onQuestion(s);
//                //System.err.println("QUESTION: " + s);
//            }
//
//
//
//            @Override
//            public Term answer(Task question, Term t, nars.tuprolog.Term pt) {
//                Term r = super.answer(question, t, pt);
//
//                //look for <a --> d> answer
//                //if (t.equals(aInhd))
//                prologAnswered = true;
//                assertTrue(true);
//
//                return r;
//            }
//
//
//        };
//
//
//        //nal1.multistep.nal
//        NALPerformance nts = new NALPerformance(nar, LibraryInput.get(nar, "other/nars_multistep_1.nal").getSource()) {
////
////
////            @Override
////            public NAR newNAR() {
////
////                Term aInhd;
////                try {
////                    aInhd = new Narsese(nar).parseTerm("<a --> d>");
////                } catch (Narsese.InvalidInputException ex) {
////                    assertTrue(false);
////                    return null;
////                }
////
////                if (prolog) {
////
////                }
////
////                return nar;
////            }
////
//
//        };
//
//
//
//        nts.run(3500);
//
//        assertTrue(prologAnswered);
//
////        //nar.addInput(new TextInput(new File("nal/Examples/Example-MultiStep-edited.txt")));
////        //nar.addInput(new TextInput(new File("nal/Examples/Example-NAL1-edited.txt")));
////        nar.addInput(new TextInput(new File("nal/test/nal1.multistep.nal")));
////        nar.finish(10);
//
//
//    }


//    NAR n;
//    PrologContext p;
//
//    @Before
//    public void start() {
//        n = new NAR(new Default());
//        p = new PrologContext(n);
//
//        //TextOutput.out(n);
//    }
//
//    @Test
//    public void testFact() {
//
//        n.input("fact(<x --> y>)!");
//        n.run(5);
//        String s = p.getProlog(null).getDynamicTheoryCopy().toString();
//        assertTrue("contains: " + s, s.contains("inheritance(x,y)."));
//
//    }
//
//    @Test
//    public void testFactual() {
//
//        //TextOutput.out(n);
//
//        n.input("fact(<a --> y>)!");
//        n.input("fact(<b --> y>)!");
//        n.input("factual(<$q --> y>, #result)!");
//        n.run(4);
//
//        //contains("<$2 <-> {<x --> y>}>>")
//        //..
//
//    }

}
