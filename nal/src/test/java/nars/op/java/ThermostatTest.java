package nars.op.java;

import jcog.math.MutableInteger;
import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import nars.control.MetaGoal;
import org.junit.jupiter.api.Test;

import static nars.Op.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class ThermostatTest {

    public static class ThermostatTester {

        protected final MutableInteger x;
        protected final NAR n;

        public ThermostatTester() throws Exception {

            Param.DEBUG = true;

            n = NARS.tmp();


            OObjects objs = new OObjects(n);

            this.x =
                    objs.a("x", MutableInteger.class, 0);
                 //objs.the("x", new MyMutableInteger());
        }
    }

    @Test
    public void test1() throws Exception {
        new ThermostatTester() {

            {
                int period = 500;
                int subPeriods = 6;
                int subPeriod = period/subPeriods;

                n.log();
                n.priDefault(BELIEF, 0.2f);
                n.priDefault(QUESTION, 0.1f);
                n.priDefault(QUEST, 0.1f);
                n.truthResolution.set(0.02f);
                n.termVolumeMax.set(28);
                n.time.dur(subPeriod/2);
                //MetaGoal.Desire.want(n.want, 1.5f);

                for (int i = 0; i < 2; i++) {
                    x.set(3);
                    n.run(subPeriod);

                    x.intValue();
                    n.run(subPeriod);

                    x.set(4);
                    n.run(subPeriod);

                    x.intValue();
                    n.run(subPeriod);
                }

                assertEquals(4, x.intValue());

                n.run(1);

                n.onTask(x -> {
                    if (x.isGoal() && !x.isInput())
                        System.out.println(x.proof());
                });


                while (x.intValue() != 3 && n.time() < period) {
                    if (n.time() % (period/subPeriods) == 0) {
                        try {
                            n.input("$1.0 x(intValue, (), 3)! :|: %1.00;0.90%");
                        } catch (Narsese.NarseseException e) {
                            e.printStackTrace();
                        }
                        //n.input("$1.0 x(intValue, (), 4)! :|: %0.00;0.90%");
                        //n.input("$1.0 (set:?1 <-> intValue:?2)?");
                        //n.input("$1.0 x(set, 1)@ :|:");
                    }
                    n.run(1);
                }

                assertEquals(3, x.intValue());

                while (x.intValue() != 5 && n.time() < period*2) {
                    if (n.time() % (period/subPeriods) == 0) {
                        try {
                            n.input("$1.0 x(intValue, (), 5)! :|: %1.00;0.90%");
//                            n.input("$0.5 x(intValue, (), 3)! :|: %0.00;0.90%");
//                            n.input("$0.5 x(intValue, (), 4)! :|: %0.00;0.90%");
                            //n.input("$1.0 (set:?1 <-> intValue:?2)?");
                            //n.input("$1.0 x(set, 1)@ :|:");
                        } catch (Narsese.NarseseException e) {
                            e.printStackTrace();
                        }
                    }
                    n.run(1);
                }
                assertEquals(5, x.intValue());

                new MetaGoal.Report().add(n.causes).print(System.out);

            }
        };
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
