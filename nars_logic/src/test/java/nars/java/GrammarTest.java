package nars.java;

import com.gs.collections.impl.set.mutable.primitive.CharHashSet;
import nars.concept.Concept;
import nars.nar.Default;
import nars.util.data.Util;

import java.util.HashSet;
import java.util.Set;

/**
 * Tool for measuring ability to learn (predict) symbol
 * pattern sequences (ie. grammars)
 *
 * Two modes:
 *      synchronous - where the symbol advances at a regular scheduler
 *      asynch - where NARS invokes a 'next' method to trigger the next
 */
public class GrammarTest {


    long delay = 50;


    public GrammarTest() throws Exception {

        //Global.DEBUG = true;


        Default n = new Default(1000, 1, 2, 3);
        n.log();
        //n.memory.activationRate.setValue(0.05f);
        n.memory.executionExpectationThreshold.setValue(0.55f);
        n.core.confidenceDerivationMin.setValue(0.01f);
        n.memory.shortTermMemoryHistory.set(3);
        n.memory.cyclesPerFrame.set(4);
        //n.initNAL9();


        GrammarTrainer grammar = new NALObjects(n).
                the("g", GrammarTrainer.class, "ababababcdcdcdcdeeee");

        //get the vocabulary
        System.out.println("VOCABULARY: " + grammar.chars());

        n.input("GrammarTrainer(predict,g,#x,#x)! %1.0|0.99%"); //input and output are demanded to be the same

        //start
        //grammar.predict('a');

        while (true) {


            try {
                n.step();
            } catch (Throwable e) {
                //e.printStackTrace();
                //System.exit(1);
                System.err.println(e);
                n.stop();
            }



            if (n.time() % 3000 == 0) {
                n.core.active.forEach(10, b -> {
                    Concept c = b.get();
                    if (c.hasBeliefs())
                        c.beliefs().print(System.out);
                });
            }

            Util.pause(delay);
        }


    }

    public static void main(String[] args) throws Exception {
        new GrammarTest();
    }


//    public void train() {
//
//        //n.input("UnitValTaskInc(move,h,((--,true)),#x). :|:  %0.0;0.75%");
//        n.input("UnitValTaskInc(move,h,((--,true)),#x)! :|:  %1.0;0.75%");
//        //n.input("UnitValTaskInc(move,h,(true),#x). :|:  %0.0;0.75%");
//        n.input("UnitValTaskInc(move,h,(true),#x)! :|: %1.0;0.75%");
//        n.input("(true -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");
//        n.input("((--,true) -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");
//        n.input("((--,true) -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");
//
//
////        if (Math.random() < 0.15f) {
//        //h.inc(Math.random() < 0.5);
////        }
//        ////(-1-->(/,^UnitVal_compare,h,(0.61368304,0.1),_)).
//        //n.input("(-1-->(/,^UnitVal_compare,h,#p,_))! %0%");
//        //n.input("(1-->(/,^UnitVal_compare,h,#p,_))! %0%");
//
//
//        //d.input("h(1)! :|: %0.65%");
//        //d.input("h(0)! :|: %0.65%");
//        //d.input("<dx --> zero>! :|:");
//        //d.input("<dy --> zero>! :|:");
//    }

    public static class GrammarTrainer {

        public final String tape;

        int i;

        public final CharHashSet chars;

        public StringBuilder log;
        private int score;

        public GrammarTrainer(String tape) {
            this.tape = tape;
            i = 0;
            score = 0;
            this.log = new StringBuilder(1024);

            chars = new CharHashSet();
            chars.addAll(tape.toCharArray());
        }

        Set<Character> chars() {
            Set<Character> s = new HashSet();
            chars.forEach(c -> s.add(c));
            return s;
        }

        public char predict(char next) {
            char actual = tape.charAt( (i++) % tape.length() );
            log.append(actual).append(next); //pairs of chars
            System.out.println(next + " ?= " + actual + " " + score());
            score += (actual == next) ? 1 : 0;
            return actual;
        }

        private float score() {
            return (float)score / i;
        }

    }
}
