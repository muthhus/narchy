package nars.java;

import nars.concept.Concept;
import nars.nar.Default;
import nars.task.Task;
import nars.truth.Truth;
import nars.util.data.UnitVal;
import nars.util.data.Util;

import java.io.PrintStream;

/**
 * Created by me on 1/20/16.
 */
public class Thermostat4 {

    static final float speed = 0.02f;
    final float tolerance = 0.15f;
    private final UnitValTaskInc h;
    long cyclePause = 0;

    Default n;
    static float targetX;


    void chase() {
        //targetX = Util.clamp( targetX + (float)(Math.random()-0.5f)*2*driftRate );

        targetX = Util.clamp(
                (float)(Math.sin( n.time() / 1000.0f )+1f)*0.5f
        );
    }



    public static class UnitValTaskInc extends UnitVal {

        @Override
        public Truth inc(boolean positive) {
            Task cTask = MethodOperator.invokingTask();
            float exp;

            //System.out.println(cTask.getExplanation());
            exp = cTask.expectation();// * cTask.getPriority();
            //exp = 1f;

            return _inc(positive, speed * exp);
        }

        public int compare(float tolerance) {
            if (_equals(targetX, tolerance)) return 0;
            if (v < targetX) return -1;
            return 1;
        }
    }

    public Thermostat4() throws Exception {

        //Global.DEBUG = true;

        n = new Default(1000, 15, 2, 3);
        //n.log();
        n.memory.activationRate.setValue(0.05f);
        n.memory.executionExpectationThreshold.setValue(0.55f);
        n.core.confidenceDerivationMin.setValue(0.01f);
        n.memory.shortTermMemoryHistory.set(3);
        n.memory.cyclesPerFrame.set(3);
        n.initNAL9();


        NALObjects objs = new NALObjects(n) {

//            @Override
//            protected Term number(Number o) {
//                //HACK:
//                if (o instanceof Integer) {
//                    return super.number(o);
//                } else {
//                    return Atom.the(0);//Atom.the("float"); //$.varDep((o.hashCode());
//                }
//            }
        };

        this.h = objs.the("h", UnitValTaskInc.class /* new UnitVal(0.5f, speed)*/);

        //h.setInc(speed);

//        d.onExec("h", t-> {
//            float exp = t.task.getExpectation();
//            Term[] aa = t.argArray();
//            if (aa.length!=1) return;
//            String aas = aa[0].toString();
//            if (aas.equals("0"))
//                exp = -exp;
//            else if (aas.equals("1")) {
//
//            } else {
//                return;
//            }
//
//            cx += exp * speed;
//
//            //System.out.println(t.task + " " + t.task.getLogLast() + " cx=" + cx);
//
//            String f = feedback(true, false);
//
//            //System.out.println(f);
//
//        });

//        d.onExec("v", t-> {
//            float exp = t.task.getExpectation();
//            cy += exp * speed;
//            feedback();
//            System.out.println(t.task + " " + t.task.getLogLast() + " cy=" + cy);
//        });

        chase();

        train();

        while (true) {



            try {
                n.step();
            } catch (Throwable e) {
                //e.printStackTrace();
                //System.exit(1);
                System.err.println(e);
                n.stop();
            }


            if (n.time() % 5 == 0) {
                int cols = 60;
                int target = Math.round(targetX * cols);
                int current = Math.round(h.isTrue().freq() * cols);

                PrintStream out = System.out;
                for (int i = 0; i <= cols; i++) {
                    char c;
                    if ((target == current) && (target == i)) c = '+';
                    else if (i == target) c = 'x';
                    else if (i == current) c = '|';
                    else c = '-';

                    out.print(c);
                }

                //reduce the number of different terms being created:
                targetX = Util.round(targetX, tolerance/2f);

                int c = h.compare(tolerance);
                out.print("\t" + c);
                int c1 = h.compare(0.25f);
                out.print("\t" + c1);
                int c2 = h.compare(0.5f);
                out.print("\t" + c2);
                int c3 = h.compare(0.75f);
                out.print("\t" + c3);
                out.println();


            }


            if (Math.random() < 0.05) {
                train();
            }
            if (Math.random() < 0.02) {
                chase();
            }

            if (n.time() % 3000 == 0) {
                n.core.active.forEach(10, b-> {
                    Concept c = b.get();
                    if (c.hasBeliefs())
                        c.beliefs().print(System.out);
                });
            }

            Util.pause(cyclePause);
        }


    }

    public static void main(String[] args) throws Exception {
        new Thermostat4();
    }

    public void train() {

        n.input("UnitValTaskInc_inc(h,((--,true)),#x)! :|:  %1.0;0.55%");
        n.input("UnitValTaskInc_inc(h,(true),#x)! :|: %1.0;0.55%");
        n.input("(0-->(/,^UnitValTaskInc_compare,h,(#p),_))! :|:");
        n.input("(--, (-1-->(/,^UnitValTaskInc_compare,h,(#p),_)))! :|:");
        n.input("(--, (1-->(/,^UnitValTaskInc_compare,h,(#p),_)))! :|:");




//        if (Math.random() < 0.15f) {
            //h.inc(Math.random() < 0.5);
//        }
        ////(-1-->(/,^UnitVal_compare,h,(0.61368304,0.1),_)).
        //n.input("(-1-->(/,^UnitVal_compare,h,#p,_))! %0%");
        //n.input("(1-->(/,^UnitVal_compare,h,#p,_))! %0%");


        //d.input("h(1)! :|: %0.65%");
        //d.input("h(0)! :|: %0.65%");
        //d.input("<dx --> zero>! :|:");
        //d.input("<dy --> zero>! :|:");
    }
}
