package nars.op.java;

import jcog.data.MutableInteger;
import nars.NAR;
import nars.NARS;
import nars.Param;
import nars.control.MetaGoal;

import static nars.Op.*;
import static org.junit.Assert.assertEquals;

public class ThermostatTest {

    static final float speed = 0.02f;
    final float tolerance = 0.15f;
    private final MutableInteger x;
    long cyclePause;

    NAR n;
    float targetX;

    final int amp = 2; //discrete range

//    void move() {
//        //targetX = Util.unitize( targetX + (float)(Math.random()-0.5f)*2*driftRate );
//        targetX = Math.round(
//                (float) (Math.sin(n.time() / 10.0f) + 1f) * 0.5f * amp
//        );
//    }


    public ThermostatTest() throws Exception {

        Param.DEBUG = true;

        n = NARS.tmp();
        //Deriver.deriver(0, "list.nal").apply(n);

        //n.log();
        n.priDefault(BELIEF, 0.2f);
        n.priDefault(QUESTION, 0.1f);
        n.priDefault(QUEST, 0.1f);


        OObjects objs = new OObjects(n);

        this.x =
                //objs.the("x", MutableInteger.class, 0);
                objs.the("x", new MyMutableInteger());

        n.time.dur(4);
        MetaGoal.Desire.want(n.want, 0.5f);

        for (int i = 0; i < 3; i++) {
            x.set(3);
            n.run(1);
            x.intValue();
            n.run(1);

            n.run(10);

            x.set(4);
            n.run(1);
            x.intValue();
            n.run(1);

            n.run(10);
        }

        assertEquals(4, x.intValue());

        n.run(1);

        n.onTask(x -> {
            if (x.isGoal() && !x.isInput())
                System.out.println(x.proof());
        });

        //n.truthResolution.set(0.1f);
        n.termVolumeMax.set(24);


        while (x.intValue()!=3 && n.time() < 7000) {
            if (n.time() % 400 == 0) {
                n.input("$1.0 x(intValue, (), 3)! :|: %1.00;0.90%");
                //n.input("$1.0 x(intValue, (), 4)! :|: %0.00;0.90%");
                //n.input("$1.0 (set:?1 <-> intValue:?2)?");
                //n.input("$1.0 x(set, 1)@ :|:");
            }
            n.run(1);
        }

        assertEquals(3, x.intValue());

        while (x.intValue()!=5 && n.time() < 14000) {
            if (n.time() % 400 == 0) {
                n.input("$1.0 x(intValue, (), 5)! :|: %1.00;0.90%");
                n.input("$0.5 x(intValue, (), 3)! :|: %0.00;0.90%");
                n.input("$0.5 x(intValue, (), 4)! :|: %0.00;0.90%");
                //n.input("$1.0 (set:?1 <-> intValue:?2)?");
                //n.input("$1.0 x(set, 1)@ :|:");
            }
            n.run(1);
        }
        assertEquals(5, x.intValue());

        new MetaGoal.Report().add(n.causes).print(System.out);


    }


    public static void main(String[] args) throws Exception {
        new ThermostatTest();
    }

    public static class MyMutableInteger extends MutableInteger {


        @Override
        public void set(int value) {
            super.set(value);
        }
    }

//    public void train() {
//
//        n.input("UnitValTaskInc(inc, h,((--,true)),#x)! :|:  %1.0;0.55%");
//        n.input("UnitValTaskInc(inc, h,(true),#x)! :|: %1.0;0.55%");
//        n.input("UnitValTaskInc(compare,h,?p,0)! :|:");
//        n.input("(--, UnitValTaskInc(compare,h,?p, -1))! :|:");
//        n.input("(--, UnitValTaskInc(compare,h,?p, 1))! :|:");
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

//   public static class UnitValTaskInc extends UnitVal {
//
//        @Override
//        public Truth inc(boolean positive) {
//            Task cTask = MethodOperator.invokingTask();
//            float exp;
//
//            //System.out.println(cTask.getExplanation());
//            exp = cTask.expectation();// * cTask.getPriority();
//
//            return _inc(positive, speed * exp);
//        }
//    }
}
