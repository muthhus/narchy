package nars.op.java;

import jcog.data.MutableInteger;
import nars.NAR;
import nars.NARS;
import nars.Param;
import org.jetbrains.annotations.NotNull;

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

        n.log();

        OObjects objs = new OObjects(n);

        this.x = objs.the("x", MutableInteger.class, 0);

        x.set(0);
        n.run(1);
        x.intValue();
        n.run(1);

        x.set(1);
        n.run(1);
        x.intValue();
        n.run(1);

        x.set(0);
        n.run(1);

        n.time.dur(10);
        n.truthResolution.set(0.1f);

        while (x.intValue()!=1) {
            n.input("x(intValue, (), 1)! :|:");
            n.run(10);
        }



    }


    public static void main(String[] args) throws Exception {
        new ThermostatTest();
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
