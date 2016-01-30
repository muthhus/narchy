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
public class Thermostat3 {

    static final float speed = 0.02f;
    final float tolerance = 0.15f;
    private final UnitVal h;
    long cyclePause = 0;

    Default n;
    float targetX;


    void move() {
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

            return _inc(positive, speed * exp);
        }
    }

    public Thermostat3() throws Exception {

        //Global.DEBUG = true;

        n = new Default(1000, 15, 2, 3);
        //n.log();
        n.memory.executionExpectationThreshold.setValue(0.65f);
        n.core.confidenceDerivationMin.setValue(0.02f);


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

        move();

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

                int c = h.compare(targetX, tolerance);
                out.print("\t" + c);
                int c1 = h.compare(targetX, 0.25f);
                out.print("\t" + c1);
                int c2 = h.compare(targetX, 0.5f);
                out.println("\t" + c2);


            }


            if (Math.random() < 0.1) {
                train();
            }
            if (Math.random() < 0.02) {
                move();
            }

            if (n.time() % 1000 == 0) {
                n.core.active.forEach(20, b-> {
                    Concept c = b.get();
                    if (c.hasBeliefs())
                        c.beliefs().print(System.out);
                });
            }

            Util.pause(cyclePause);
        }


    }

    public static void main(String[] args) throws Exception {
        new Thermostat3();
    }

    public void train() {

        n.input("UnitValTaskInc(inc, h,((--,true)),#x)! :|:  %1.0;0.55%");
        n.input("UnitValTaskInc(inc, h,(true),#x)! :|: %1.0;0.55%");
        n.input("UnitValTaskInc(compare,h,?p,0)! :|:");
        n.input("(--, UnitValTaskInc(compare,h,?p, -1))! :|:");
        n.input("(--, UnitValTaskInc(compare,h,?p, 1))! :|:");




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
