package nars.op.sys.java;

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


    long delay = 25;


    public GrammarTest() throws Exception {

        //Global.DEBUG = true;


        Default n = new Default(1000, 3, 2, 3);
        n.logSummaryGT(System.out, 0.3f);


        //n.memory.executionThreshold.setValue(0.55f);
        n.premiser.confMin.setValue(0.01f);

        n.memory.DEFAULT_JUDGMENT_PRIORITY = 0.5f;
        n.memory.DEFAULT_GOAL_PRIORITY = 0.6f;
        n.memory.activationRate.setValue(0.1f);

        n.memory.duration.set(1);
        n.memory.shortTermMemoryHistory.set(3);
        n.memory.cyclesPerFrame.set(8);
        //n.initNAL9();


        Trainer grammar = new Lobjects(n).
                the("grammar", Trainer.class, "ababcdcdeeee");

        //get the vocabulary
        Set<Character> vocab = grammar.chars();

        n.input("(($x<->#y) &&+0 Trainer(predict,grammar,($x),#y))! %1.0|0.99%"); //input and output are demanded to be the same
        n.input("(Trainer(predict,grammar,(#x),#y) ==> Trainer(predict,grammar,(#y),$z))? :|:");

        int period = 32;

        //start
        for (char c : vocab) {
            n.run(period/2);
            grammar.predict(c);
            n.run(period/2);
            grammar.prev();
        }

        int frames = 1064;
        for (int i = 0; i < frames; i++) {


            if (i% period == 0) {
                n.input("Trainer(predict,grammar,(#x),#y)! :|:");
                n.input("Trainer(predict,grammar,(#x),#y)? :/:");
                n.input("Trainer(prev,grammar,(),#y)! :/:");
            }


            try {
                n.step();
            } catch (Throwable e) {
                //e.printStackTrace();
                //System.exit(1);
                System.err.println(e);
                n.stop();
            }



            if (n.time() % 3000 == 0) {
                dump(n);
            }

            Util.pause(delay);
        }

        dump(n);
        System.out.println("SCORE: " + grammar.score + " / " + grammar.i);

    }

    private void dump(Default n) {
        n.core.active.forEach(10, b -> {
            Concept c = b.get();
            if (c.hasBeliefs())
                c.beliefs().print(System.out);
        });
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

    /** Grammar Trainer */
    public static class Trainer {

        public final String tape;

        int i;

        public final CharHashSet chars;

        public StringBuilder log;
        private int score;

        public Trainer(String tape) {
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
            char actual = c(i++);
            log.append(actual).append(next); //pairs of chars
            System.out.println(next + " ?= " + actual + " " + score());
            score += (actual == next) ? 1 : 0;
            return actual;
        }

        private char c(int p) {
            return tape.charAt( p % tape.length() );
        }

        private float score() {
            return (float)score / i;
        }

        public char prev() {
            if (i == 0) return '?';
            return c(i-1);
        }

    }
}
