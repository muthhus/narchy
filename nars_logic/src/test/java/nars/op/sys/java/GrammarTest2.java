package nars.op.sys.java;

import com.gs.collections.impl.set.mutable.primitive.CharHashSet;
import nars.Global;
import nars.Memory;
import nars.NAR;
import nars.concept.Concept;
import nars.nar.Default;
import nars.task.Task;
import nars.util.data.Util;
import nars.op.sys.NarQ;
import nars.op.sys.NarQ.InputTask;
import org.apache.commons.lang3.mutable.MutableFloat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.DoubleSupplier;

/**
 * Tool for measuring ability to learn (predict) symbol
 * pattern sequences (ie. grammars)
 *
 * Two modes:
 *      synchronous - where the symbol advances at a regular scheduler
 *      asynch - where NARS invokes a 'next' method to trigger the next
 */
public class GrammarTest2 {

    int frames = 2048;
    int delay = 0;

    public GrammarTest2() throws Exception {

        //Global.DEBUG = true;

        int charRate = 16;
        Default n = new Default(1000, 3, 2, 3);
        Lobjects o = new Lobjects(n);
        final Tape tape = o.the("tape", Tape.class, "ababcdcdeeee", charRate);
        

        NarQ q = new NarQ(n);
        
        //INPUTS
        q.input.add(new DoubleSupplier() {
            @Override public double getAsDouble() {
                return tape.phase();
            }                        
        });
        q.input.addAll(tape.getCharSensors(-1));
        q.input.addAll(tape.getCharSensors(0));
        
        q.goal.put(tape.correctPredictionReward, new MutableFloat(1f));
        
        q.output.addAll(tape.getPredictActions(n));
        
        //n.logSummaryGT(System.out, 0.2f);


        //n.memory.executionThreshold.setValue(0.55f);
        n.premiser.confMin.setValue(0.01f);

        n.DEFAULT_JUDGMENT_PRIORITY = 0.5f;
        n.DEFAULT_GOAL_PRIORITY = 0.5f;
        n.activationRate.setValue(0.5f);

        n.duration.set(2);
        n.shortTermMemoryHistory.set(4);
        n.cyclesPerFrame.set(1);
        
        //n.initNAL9();

        
        n.onFrame((nn) -> {
           tape.setTime(nn.time(), this::onStep); 
        });

        tape.charSet();
        

        for (int f = 0; f < frames; f++) {

            n.step();

            Util.pause(delay);
        }

        //dump(n);
        

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

    protected void onStep() {
        
    }
    
    /** Grammar Trainer */
    public static class Tape {

        public final String buffer;

        public final CharHashSet chars;

        public StringBuilder log;
        private float score, totalScore;
        private long time;
        private final DoubleSupplier correctPredictionReward;
        int charPeriod; //cycles per char
        private char current, prev;

        public Tape(String tape, int charRate) {
            this.buffer = tape;
            this.charPeriod = charRate;
            time = 0;
            score = 0;
            this.log = new StringBuilder(1024);

            chars = new CharHashSet();
            chars.addAll(tape.toCharArray());
            
            correctPredictionReward = new DoubleSupplier() {
                
                @Override public double getAsDouble() {
                    return score;
                }
               
            };
            
            setTime(0, null);
        }

        private Character[] chars() {
            TreeSet<Character> s = charSet();
            return s.toArray(new Character[s.size()]);
        }

        public TreeSet<Character> charSet() {
            TreeSet<Character> s = new TreeSet();
            chars.forEach(s::add);
            return s;
        }

//        public char predict(char next) {
//            char actual = c(i++);
//            log.append(actual).append(next); //pairs of chars
//            System.out.println(next + " ?= " + actual + " " + score());
//            score += (actual == next) ? 1 : 0;
//            return actual;
//        }

        private char c(long p) {
            if (p < 0) return '?';
            return buffer.charAt( (int)(p % buffer.length()) );
        }


        private void setTime(long t, Runnable onStep) {
            this.time = t;
            //System.out.println("time=" + t + ", " + (time % charRate));
            if (time % charPeriod == (charPeriod-1)) {               
                step();
                if (onStep!=null)
                    onStep.run();
            }

        }
        
        private void step() {
            totalScore += score;
            score = 0; //reset score for the next char
            current = c(0);
            prev = c(1);
        }

        public char prev() {
            return prev;
        }
        
        public char current() {
            return current;
        }
        
        private char next() {
            return c(time+1);
        }


                
        /** time since last update to next char */
        private float phase() {
            return (time  % charPeriod) / ((float)(charPeriod));
        }



        private List<DoubleSupplier> getCharSensors(int dt) {
            List<DoubleSupplier> d = Global.newArrayList();
            for (char c : chars()) {
                d.add(new DoubleSupplier() {
                    
                    @Override public double getAsDouble() {
                        switch (dt) {
                            case 0: return current == c ? 1 : -1;
                            case -1: return prev == c ? 1 : -1;
                            default:
                                return 0; //invalid deltatime
                        }
                    }
                   
                });                
            }
            return d;
        }

        public void predict(String s) {
            if (s.length()==3)                  {
                if ((s.charAt(0) == '\"') && (s.charAt(2) == '\"'))
                   predict(s.charAt(1));   
            } else if (s.length()==1) {
                predict(s.charAt(0));
            }
            
            
        }
        
        void predict(char next) {
            final Task tt = MethodOperator.invokingTask();
            
            float strength = (tt.freq())-0.5f * tt.conf();
            
            //reward and punish more as it becomes sooner to next update
            float p = (1f+phase()/2f)/charPeriod * strength;
            
            float ds;
            if (next == next()) {
                ds = p;
            } else {
                ds = -p;
            }
            
//            System.out.println("@" + time + " " + prev + "," + current + "," + next + 
//                    "  ... score+="+ ds + ", total=" + totalScore 
//                    //+ "\n\t" + tt
//            );
            System.out.println(time + "," + prev + "," + current + "," + next + 
                    ","+ ds + "," + totalScore 
                    //+ "\n\t" + tt
            );
            
            score += ds;
        }
        
        private Collection<NarQ.Action> getPredictActions(NAR n) {
            List<NarQ.Action> l = new ArrayList();
            for (char c : chars()) {
                l.add(
                    new InputTask(n, n.term("Tape(predict, tape, (\"" + c + "\"), #1)"))
                );
            }            
            return l;
        }

    }
}
