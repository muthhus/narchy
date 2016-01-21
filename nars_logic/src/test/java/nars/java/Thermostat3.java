package nars.java;

import nars.nar.Default;
import nars.task.Task;
import nars.util.data.UnitVal;
import nars.util.data.Util;

import java.io.PrintStream;

/**
 * Created by me on 1/20/16.
 */
public class Thermostat3 {

    final float speed = 0.05f;
    final float tolerance = 0.1f;
    final float driftRate = 0.1f;
    private final UnitVal h;

    Default n;
    float targetX, targetY;


    void move() {
        targetX = Util.clamp( targetX + (float)(Math.random()-0.5f)*2*driftRate );
        //targetY = (float)Math.random();
        //h.random();
    }

//    String pos(String var, float value) {
//        if (Math.abs(value) < tolerance) {
//            return "<" + var + " --> zero>. :|:";
//        }
//        String dir = value > 0 ? "plus" : "neg";
//
//        return "<" + var + " --> " + dir + ">. :|: %1;" + Math.abs(value) + "%";
//    }

//    String feedback(boolean h, boolean v) {
//        String s = "";
//        if (h) {
//            cx = Util.clamp(cx);
//            float dx = targetX - cx;
//            String t = pos("dx", dx);
//            s += t;
//            d.input(t);
//        }
//
//        if (v) {
//            cy = Util.clamp(cy);
//            float dy = targetY - cy;
//            String t = pos("dy", dy);
//            s += t;
//            d.input(t);
//        }
//
//        return s;
//    }

    public static class UnitValTaskInc extends UnitVal {

        @Override
        public void inc(boolean positive) {
            Task cTask = MethodOperator.getCurrentTask();
            float exp;
            if (cTask == null) {
                //exp = 0.1f;
                return;
            } else {
                exp = cTask.getExpectation() * cTask.getPriority();
            }

            _inc(positive, exp);
        }
    }

    public Thermostat3() throws Exception {

        //Global.DEBUG = true;

        n = new Default(1000, 15, 1, 3);
        //n.log();
        n.memory.executionExpectationThreshold.setValue(0.6f);


        NALObjects objs = new NALObjects(n);
        this.h = objs.wrap("h", UnitValTaskInc.class /* new UnitVal(0.5f, speed)*/);

        h.setInc(speed);

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
                n.frame();
            } catch (Throwable e) {
                //e.printStackTrace();
                //System.exit(1);
                System.err.println(e);
                n.stop();
            }


            if (n.time() % 10 == 0) {
                int cols = 40;
                int target = Math.round(targetX * cols);
                int current = Math.round(h.isTrue().getFrequency() * cols);

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


            if (Math.random() < 0.01) {
                train();
            }
            if (Math.random() < 0.02) {
                move();
            }
        }


    }

    public static void main(String[] args) throws Exception {
        new Thermostat3();
    }

    public void train() {
        //d.input("v(1)! :|: %0.75%");
        //d.input("v(0)! :|: %0.75%");

        h.inc(true);
        h.inc(false);

        ////(-1-->(/,^UnitVal_compare,h,(0.61368304,0.1),_)).
        n.input("(0-->(/,^UnitVal_compare,h,#p,_))! %1%");
        n.input("(-1-->(/,^UnitVal_compare,h,#p,_))! %0%");
        n.input("(1-->(/,^UnitVal_compare,h,#p,_))! %0%");


        //d.input("h(1)! :|: %0.65%");
        //d.input("h(0)! :|: %0.65%");
        //d.input("<dx --> zero>! :|:");
        //d.input("<dy --> zero>! :|:");
    }
}
