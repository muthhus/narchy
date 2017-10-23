//package nars.prolog;
//
//import alice.tuprolog.MalformedGoalException;
//import nars.NAR;
//import nars.Narsese;
//import nars.nar.Default;
//import nars.op.PrologCore;
//import org.junit.jupiter.api.Test;
//
//import java.io.IOException;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.junit.jupiter.api.Assertions.*;
//
///**
// * Created by me on 3/3/16.
// */
//public class PrologCoreTest {
//
//    @Test
//    public void testPrologCoreBeliefAssertion() throws MalformedGoalException, IOException {
//        NAR n = new Default();
//        PrologCore p = new PrologCore(n);
//        n.input("a:b.");
//        n.input("(--, c:d).");
//        n.next();
//
//        assertTrue(p.isTrue("b('-->'(b,a), 1)."));
//        assertFalse(p.isTrue("b('-->'(a,b), 1)."));
//        assertTrue(p.isTrue("b('-->'(d,c), 0)."));
//        assertFalse(p.isTrue("b('-->'(d,c), 1)."));
//
//    }
//
//    @Test
//    public void testPrologCoreQuestionTruthAnswer() throws MalformedGoalException, IOException {
//        NAR n = new Default();
//        PrologCore p = new PrologCore(n);
//        n.input("a:b.");
//        n.input("a:c.");
//        n.input("(--, c:d).");
//        n.next();
//
//        n.input("a:b?");
//        //expect true
//        n.next();
//
//        n.input("c:d?");
//        //expect false
//        n.next();
//
//        n.input("a:?x?");
//        //expect true with 2 answers
//        n.next();
//
//    }
//
//    @Test
//    public void testPrologCoreDerivedTransitive() throws MalformedGoalException, IOException {
//        NAR n = new Default();
//        PrologCore p = new PrologCore(n);
//        n.input("a:b.");
//        n.input("b:c.");
//        n.next();
//
//        n.input("a:c?");
//        //expect true
//        n.next();
//
//        n.input("a:d?");
//        //expect false
//        n.next();
//    }
//
//    @Test
//    public void testConjunction3() throws MalformedGoalException, IOException {
//        NAR n = new Default();
//        PrologCore p = new PrologCore(n);
//        n.input("(&&,a,b,c).");
//        n.next();
//
//        assertTrue(p.isTrue("b('&&'(a,b,c),1)."));
//        //assertTrue(p.isTrue("','(a,','(b,c))."));
//    }
//    @Test
//    public void testConjunction3b() throws MalformedGoalException, IOException, Narsese.NarseseException {
//        NAR n = new Default();
//        PrologCore p = new PrologCore(n);
//        n.believe("x:a");
//        n.believe("y:b");
//        n.believe("z:c", false);
//        n.next();
//
//        assertTrue(p.isTrue("'&&'('-->'(a,x), '-->'(b,y), not('-->'(c,z)))."));
//        assertFalse(p.isTrue("'&&'('-->'(a,x), '-->'(b,y), '-->'(c,z))."));
//        //assertTrue(p.isTrue("','(a,','(b,c))."));
//
//    }
//
//    @Test
//    public void testPrologCoreDerivedTransitive2() throws MalformedGoalException, IOException {
//        NAR n = new Default();
//        PrologCore p = new PrologCore(n);
//        n.input("a:b.");
//        n.input("b:c.");
//        n.input("c:d.");
//        n.input("d:e.");
//        n.input("e:f.");
//        n.next();
//
//        n.input("a:f?");
//        //expect true
//        n.next();
//
//        n.input("a:?x?");
//        //expect true
//        n.next();
//
//    }
//
////    boolean prologAnswered = false;
////
////
////
////    @Test
////    public void testMultistep() throws Exception {
////        boolean prolog = true;
////        //boolean showOutput = false;
////        Global.DEBUG = true;
////
////        NAR nar = new NAR( new Default().setInternalExperience(null) );
////
////        NARPrologMirror p = new NARPrologMirror(nar, 0.1f, true, true, true) {
////
////            @Override
////            protected void onQuestion(Sentence s) {
////                super.onQuestion(s);
////                //System.err.println("QUESTION: " + s);
////            }
////
////
////
////            @Override
////            public Term answer(Task question, Term t, nars.tuprolog.Term pt) {
////                Term r = super.answer(question, t, pt);
////
////                //look for <a --> d> answer
////                //if (t.equals(aInhd))
////                prologAnswered = true;
////                assertTrue(true);
////
////                return r;
////            }
////
////
////        };
////
////
////        //nal1.multistep.nal
////        NALPerformance nts = new NALPerformance(nar, LibraryInput.get(nar, "other/nars_multistep_1.nal").getSource()) {
//////
//////
//////            @Override
//////            public NAR newNAR() {
//////
//////                Term aInhd;
//////                try {
//////                    aInhd = new Narsese(nar).parseTerm("<a --> d>");
//////                } catch (Narsese.InvalidInputException ex) {
//////                    assertTrue(false);
//////                    return null;
//////                }
//////
//////                if (prolog) {
//////
//////                }
//////
//////                return nar;
//////            }
//////
////
////        };
////
////
////
////        nts.run(3500);
////
////        assertTrue(prologAnswered);
////
//////        //nar.addInput(new TextInput(new File("nal/Examples/Example-MultiStep-edited.txt")));
//////        //nar.addInput(new TextInput(new File("nal/Examples/Example-NAL1-edited.txt")));
//////        nar.addInput(new TextInput(new File("nal/test/nal1.multistep.nal")));
//////        nar.finish(10);
////
////
////    }
//
//
////    NAR n;
////    PrologContext p;
////
////    @Before
////    public void start() {
////        n = new NAR(new Default());
////        p = new PrologContext(n);
////
////        //TextOutput.out(n);
////    }
////
////    @Test
////    public void testFact() {
////
////        n.input("fact(<x --> y>)!");
////        n.run(5);
////        String s = p.getProlog(null).getDynamicTheoryCopy().toString();
////        assertTrue("contains: " + s, s.contains("inheritance(x,y)."));
////
////    }
////
////    @Test
////    public void testFactual() {
////
////        //TextOutput.out(n);
////
////        n.input("fact(<a --> y>)!");
////        n.input("fact(<b --> y>)!");
////        n.input("factual(<$q --> y>, #result)!");
////        n.run(4);
////
////        //contains("<$2 <-> {<x --> y>}>>")
////        //..
////
////    }
//
//}
