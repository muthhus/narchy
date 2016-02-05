package nars.java;

import com.gs.collections.impl.set.mutable.primitive.CharHashSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import nars.concept.Concept;
import nars.nar.Default;
import nars.util.data.Util;

import java.util.TreeSet;
import java.util.function.DoubleSupplier;
import nars.Global;
import nars.NAR;
import nars.util.signal.NarQ;
import nars.util.signal.NarQ.InputGoal;
import nars.util.signal.NarQ.Vercept;
import org.apache.commons.lang3.mutable.MutableFloat;

/**
 * Tool for measuring ability to learn (predict) symbol
 * pattern sequences (ie. grammars)
 *
 * Two modes:
 *      synchronous - where the symbol advances at a regular scheduler
 *      asynch - where NARS invokes a 'next' method to trigger the next
 */
public class GrammarTest2 {

    

    public GrammarTest2() throws Exception {

        //Global.DEBUG = true;


        Default n = new Default(1000, 3, 2, 3);
        Naljects o = new Naljects(n);
        final Tape tape = o.the("tape", Tape.class, "ababcdcdeeee");
        
        Vercept i = new Vercept();
        NarQ q = new NarQ(n, i);
        
        //INPUTS
        i.add(new DoubleSupplier() {           
            @Override public double getAsDouble() {
                return tape.phase();
            }                        
        });
        i.addAll(tape.getCharSensors(-1));
        i.addAll(tape.getCharSensors(0));
        
        q.reward.put(tape.correctPredictionReward, new MutableFloat(1f));
        
        q.outs.addAll(tape.getPredictActions(n));
        
        n.logSummaryGT(System.out, 0.01f);


        //n.memory.executionExpectationThreshold.setValue(0.55f);
        n.core.confidenceDerivationMin.setValue(0.01f);

        n.memory.DEFAULT_JUDGMENT_PRIORITY = 0.5f;
        n.memory.DEFAULT_GOAL_PRIORITY = 0.6f;
        n.memory.activationRate.setValue(0.5f);

        n.memory.duration.set(1);
        n.memory.shortTermMemoryHistory.set(3);
        n.memory.cyclesPerFrame.set(8);
        //n.initNAL9();

        n.onFrame((nn) -> {
           tape.setTime(nn.time()); 
        });

        
        int frames = 64;
        int delay = 10;
        for (int f = 0; f < frames; f++) {

            n.step();

            Util.pause(delay);
        }

        dump(n);
        

    }

    private void dump(Default n) {
        n.core.active.forEach(10, b -> {
            Concept c = b.get();
            if (c.hasBeliefs())
                c.beliefs().print(System.out);
        });
    }

    public static void main(String[] args) throws Exception {
        new GrammarTest2();
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
    public static class Tape {

        public final String buffer;

        int i;

        public final CharHashSet chars;

        public StringBuilder log;
        private int score;
        private long time;
        private final DoubleSupplier correctPredictionReward;
        int charRate = 2; //cycles per char

        public Tape(String tape) {
            this.buffer = tape;
            i = 0;
            score = 0;
            this.log = new StringBuilder(1024);

            chars = new CharHashSet();
            chars.addAll(tape.toCharArray());
            
            correctPredictionReward = new DoubleSupplier() {
                
                @Override public double getAsDouble() {
                    return score;
                }
                
            };
        }

        private Character[] chars() {
            TreeSet<Character> s = new TreeSet();
            chars.forEach(c -> s.add(c));
            return s.toArray(new Character[s.size()]);
        }

//        public char predict(char next) {
//            char actual = c(i++);
//            log.append(actual).append(next); //pairs of chars
//            System.out.println(next + " ?= " + actual + " " + score());
//            score += (actual == next) ? 1 : 0;
//            return actual;
//        }

        private char c(int p) {
            if (p < 0) return '?';
            return buffer.charAt(p % buffer.length() );
        }


        private void setTime(long t) {
            this.time = t;
            if (time % charRate == (charRate-1)) {
                i = (i+1) % buffer.length();             
                score = 0; //reset score for the next char
            }
        }

        public char prev() {
            return c(i-1);
        }
        
        public char current() {
            return c(i);
        }
        
        private char next() {
            return c(i+1);
        }


                
        /** time since last update to next char */
        public float phase() {
            return (i  % charRate) / ((float)(charRate));
        }



        private List<DoubleSupplier> getCharSensors(int dt) {
            List<DoubleSupplier> d = Global.newArrayList();
            for (char c : chars()) {
                d.add(new DoubleSupplier() {
                    
                    @Override public double getAsDouble() {
                        switch (dt) {
                            case 0: return current();
                            case -1: return prev();
                            default:
                                return 0; //invalid deltatime
                        }
                    }
                   
                });                
            }
            return d;
        }

        public void predict(String s) {
            if (s.isEmpty() || s.length()!=3) return;
            if ((s.charAt(0) == '\"') && (s.charAt(2) == '\"'))
                predict(s.charAt(1));   
        }
        void predict(char x) {
            
            //reward and punish more as it becomes sooner to next update
            float p = phase()/2f + 0.1f;
            
            float ds;
            if (x == next()) {
                ds = p;
            } else {
                ds = p;
            }
            
            System.out.println("score change: "+ ds);
            
            score += ds;
        }
        
        private Collection<NarQ.Action> getPredictActions(NAR n) {
            List<NarQ.Action> l = new ArrayList();
            for (char c : chars()) {
                l.add(
                    new InputGoal(n, n.term("Tape(predict, tape, (" + c + "), #1)"))
                );
            }            
            return l;
        }

    }
}
