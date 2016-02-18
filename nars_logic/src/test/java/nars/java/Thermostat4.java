package nars.java;

import nars.concept.Concept;
import nars.nar.Default;
import nars.learn.HaiQ;
import nars.task.Task;
import nars.util.data.UnitVal;
import nars.util.data.Util;

import java.io.PrintStream;

import static nars.java.Thermostat4.UnitValTaskInc.cols;

/**
 * Created by me on 1/20/16.
 */
public class Thermostat4 {

    static final float speed = 0.001f;
    //final static float tolerance = 0.1f;
    static float targetX;
    private final UnitValTaskInc h;
    long cyclePause = 0;
    Default n;


    public Thermostat4() throws Exception {

        //Global.DEBUG = true;


        n = new Default(1000, 1, 2, 3);
        //n.log();
        n.memory.activationRate.setValue(0.05f);
        n.premiser.confMin.setValue(0.01f);
        n.memory.shortTermMemoryHistory.set(3);
        n.memory.cyclesPerFrame.set(4);
        //n.initNAL9();


        Lobjects objs = new Lobjects(n) {

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

        HaiQ hai = new HaiQ(cols, 9, 3) {
            @Override
            protected int perceive(float[] input) {
                //TODO
                return 0;
            }
        };
        n.onFrame(nn -> {
            int[] x = h.see();
            float[] ff = new float[x.length];
            for (int i = 0; i < x.length; i++) {
                int xx = x[i];
                switch ((char)xx) {
                    case '|': ff[i] = 1f; break;
                    case 'x': ff[i] = -1f; break;
                }
            }
            int a = hai.act(ff, -.5f + 2f/(1f+Math.abs(targetX - h.get()) ));
            switch (a) {
                case 0: h.move(true); break;
                case 1: h.move(false); break;
                case 2: /* nothing */ break;
            }
            //System.out.println(Arrays.toString(ff) + " => " + a);
        });

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


                PrintStream out = System.out;

                int[] cc = h.see();
                for (int i = 0; i < cc.length; i++) {
                    char c = (char) cc[i];
                    if (c == 0) c = '-';
                    out.print(c);
                }


                //reduce the number of different terms being created:
//                targetX = Util.round(targetX, tolerance/2f);

//                int c = h.compare(tolerance);
//                out.print("\t" + c);
//                int c1 = h.compare(0.25f);
//                out.print("\t" + c1);
//                int c2 = h.compare(0.5f);
//                out.print("\t" + c2);
//                int c3 = h.compare(0.75f);
//                out.print("\t" + c3);
                out.println();


            }


            if (Math.random() < 0.05) {
                train();
            }
            if (Math.random() < 0.02) {
                chase();
            }

            if (n.time() % 3000 == 0) {
                n.core.active.forEach(10, b -> {
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

    void chase() {
        //targetX = Util.clamp( targetX + (float)(Math.random()-0.5f)*2*driftRate );

        targetX = Util.clamp(
                (float) (Math.sin(n.time() / 1000.0f) + 1f) * 0.5f
        );
    }

    public void train() {

        //n.input("UnitValTaskInc(move,h,((--,true)),#x). :|:  %0.0;0.75%");
        n.input("UnitValTaskInc(move,h,((--,true)),#x)! :|:  %1.0;0.75%");
        //n.input("UnitValTaskInc(move,h,(true),#x). :|:  %0.0;0.75%");
        n.input("UnitValTaskInc(move,h,(true),#x)! :|: %1.0;0.75%");
        n.input("(true -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");
        n.input("((--,true) -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");
        n.input("((--,true) -->(/,^UnitValTaskInc,move,h,(?p),_))! :|:");


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

    public static class UnitValTaskInc extends UnitVal {
        final static int cols = 16;

        public int[] see() {

            int target = Math.round(targetX * cols);
            int current = Math.round(v * cols);
            int[] ss = new int[cols + 1];

            for (int i = 0; i <= cols; i++) {
                char c;

                if ((target == current) && (target == i)) c = '+';
                else if (i == target) c = 'x';
                else if (i == current) c = '|';
                else c = 0; //'-';

                ss[i] = c;
            }

            return ss;
        }

        public boolean move(boolean positive) {
            Task cTask = MethodOperator.invokingTask();
            float exp;

            //System.out.println(cTask.getExplanation());
            exp = cTask.expectation();// * cTask.getPriority();
            //exp = 1f;


            float da = Math.abs(targetX - v);

            _inc(positive, speed * exp);

            float db = Math.abs(targetX - v);

            return db < da;
        }

//        public int compare(float tolerance) {
//            if (_equals(targetX, tolerance)) return 0;
//            if (v < targetX) return -1;
//            return 1;
//        }
    }
}
