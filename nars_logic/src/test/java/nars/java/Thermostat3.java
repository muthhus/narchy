package nars.java;

import nars.nar.Default;
import nars.util.data.UnitVal;

/**
 * Created by me on 1/20/16.
 */
public class Thermostat3 {

    final float speed = 0.05f;
    final float tolerance = 0.1f;
    private final UnitVal h;

    Default d;
    float targetX, targetY;


    void reset() {
        targetX = (float)Math.random();
        targetY = (float)Math.random();
        h.random();
    }

    String pos(String var, float value) {
        if (Math.abs(value) < tolerance) {
            return "<" + var + " --> zero>. :|:";
        }
        String dir = value > 0 ? "plus" : "neg";

        return "<" + var + " --> " + dir + ">. :|: %1;" + Math.abs(value) + "%";
    }

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

    public Thermostat3() throws Exception {

        d = new Default(1000, 10, 2, 3);
        d.log();


        NALObjects objs = new NALObjects(d);
        this.h = objs.wrap("h", UnitVal.class /* new UnitVal(0.5f, speed)*/);

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

        reset();

        train();

        while (true) {



            try {
                d.frame();
            } catch (Exception e) {
                System.err.println(e);
                d.stop();
            }


            if (d.time() % 32 == 0) {
                int range = 40;
                int target = Math.round(targetX * range);
                int current = Math.round(h.isTrue().getFrequency() * range);
                for (int i = 0; i < range; i++) {
                    char c;
                    if ((target == current) && (target == i)) c = '+';
                    else if (i == target) c = 'x';
                    else if (i == current) c = '|';
                    else c = '-';
                    System.out.print(c);
                }
                System.out.println();
            }


            if (Math.random() < 0.01) {
                train();
            }
            if (Math.random() < 0.0001) {
                reset();
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

        //d.input("h(1)! :|: %0.65%");
        //d.input("h(0)! :|: %0.65%");
        d.input("<dx --> zero>! :|:");
        //d.input("<dy --> zero>! :|:");
    }
}
