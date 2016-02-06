package nars.nal.nal8;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.term.Term;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by me on 1/15/16.
 */
public class GoalPrecisionTest {

    /**
     * op -> (requested,
     * firstInvoked_ideally~=requested,
     * firstInvokeBudget
     * subsequentInvokeBudgetSums
     * subsequentInvokeNum
     * <p>
     * <p>
     * )
     */
    final Map<String, float[]> plan = new HashMap();

    protected void schedule(int when, String action) {
        plan.put(action, new float[]{when, -1, 0, 0, 0});
    }

    protected void print() {
        plan.forEach((k, v) -> {
            System.out.println(k + " " + Arrays.toString(v));
        });
    }

    protected void run(NAR n, int end) {

        Global.DEBUG = true;
        n.onExecution($.operator("x"), (Execution a) -> {
            Term[] aa = a.argArray();
            float pri = a.task.pri() * a.task.expectation();

            float[] d = plan.get(aa[0].toString());
            if (d == null) {
                throw new RuntimeException("unknown action: " + a);
            }


            if (d[1] == -1) {
                //first time
                d[1] = (int) n.time();
                d[2] = pri;
                System.out.println("OK " + a.task);
            } else {
                d[3] += pri;
                d[4]++;

                System.out.println();
                System.out.println(a.task);
                System.out.println(a.task.log());
                a.nar.concept(a.task.concept()).print();
                System.out.println(a.task.explanation());
            }

            //a.task.mulPriority(0);


        });

//        n.onExec("x", (Term[] aa) {
//            @Override
//            public void execute(Execution a) {
//
//                Term[] aa = Operator.opArgsArray(a.term());
//                float pri = a.task.pri() * a.task.expectation();
//
//                float[] d = plan.get(aa[0].toString());
//                if (d == null) {
//                    throw new RuntimeException("unknown action: " + a);
//                }
//
//
//
//                if (d[1] == -1) {
//                    //first time
//                    d[1] = (int) n.time();
//                    d[2] = pri;
//                    System.out.println("OK " + a.task);
//                } else {
//                    d[3] += pri;
//                    d[4]++;
//
//                    System.out.println();
//                    System.out.println(a.task);
//                    System.out.println(a.task.log());
//                    a.taskConcept().print();
//                    System.out.println(a.task.getExplanation());
//                }
//
//                //a.task.mulPriority(0);
//
//                a.noticeExecuted();
//            }
//        });

        //n.onExecTask("x", a -> {

        //});

        //input:
        plan.forEach((k, v) -> {
            n.inputAt((int) v[0], "x(" + k + ")! :|:");
        });

        n.run(end);

        print();

    }


    @Test
    public void testExecuteGoalsOnTimeWithoutRepeats() {

        schedule(1, "a");
        schedule(10, "b");
        schedule(15, "c");

        run(new Default(), 100);

    }

    @Test
    public void testExecuteGoalsResultingFromDerivation() {

        Default n = new Default(100, 2, 1, 3);
        //n.inputAt(1, "(x(b) ==>-10 x(a)).");
        n.inputAt(1, "(x(a) ==>+3 x(b)).");

        schedule(1, "a");
        schedule(9999, "b");

        run(n, 15);

    }
}
