package nars.op.java;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import nars.task.DerivedTask;
import org.junit.Ignore;
import org.junit.Test;

import static nars.Op.BELIEF;
import static org.junit.Assert.assertEquals;

public class OObjectsTest {

    static class SimpleClass {
        private int v;

        public void set(int x) {
            System.err.println("set: " + x);
            this.v = x;
        }

        public int get() {
            return v;
        }
    }


    @Test
    public void testSelfInvocation() throws Narsese.NarseseException {
        final NAR n = NARS.tmp();

        final OObjects objs = new OObjects(n);

        final SimpleClass x = objs.the("x", SimpleClass.class);
        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);

        n.input("SimpleClass(set,x,(1))! :|:");
        n.run(1);
        n.run(1);

        n.input("SimpleClass(get,x,(),#y)! :|:");
        n.run(1);
        n.run(1);

        assertEquals("$.50 SimpleClass(set,x,(1))! 0 %1.0;.90%$.50 SimpleClass(set,x,(1)). 1 %1.0;.90%$.50 SimpleClass(get,x,(),#1)! 2 %1.0;.90%$.50 SimpleClass(get,x,(),1). 3 %1.0;.90%", sb.toString());

    }

    @Test
    public void testExternalInvocation() {
        final NAR n = NARS.tmp();

        final OObjects objs = new OObjects(n);

        final SimpleClass x = objs.the("x", SimpleClass.class);
        StringBuilder sb = new StringBuilder();
        n.onTask(sb::append);
        n.run(1);
        {
            x.get();
        }
        n.run(1);
        {
            x.set(1);
        }
        n.run(1);
        assertEquals("$.50 SimpleClass(get,x,(),0). 1 %1.0;.90%$.50 SimpleClass(set,x,(1)). 2 %1.0;.90%$.09 (((get,x,(),0)-->$1) ==>+1 ((set,x,(1))-->$1)). 1 %1.0;.45%$.09 (SimpleClass(get,x,(),0) ==>+1 SimpleClass(set,x,(1))). 1 %1.0;.45%$.17 (((get,x,(),0)-->#1) &&+1 ((set,x,(1))-->#1)). 1⋈2 %1.0;.81%$.09 (SimpleClass(set,x,(1)) ==>-1 SimpleClass(get,x,(),0)). 2 %1.0;.45%", sb.toString());
    }

    @Ignore
    @Test
    public void learnMethodGoal() throws Narsese.NarseseException {
//         StringBuilder sb = new StringBuilder();
//        n.onTask(sb::append);
        Param.DEBUG = true;
        final NAR n = NARS.tmp();


        final OObjects objs = new OObjects(n);

        final SimpleClass x = objs.the("x", SimpleClass.class);


        n.priDefault(BELIEF, 0.5f);
        n.truthResolution.set(0.05f);
        n.logPriMin(System.out, 0.02f);
        n.time.dur(10);

//        n.onTask(xx -> {
//           if (xx instanceof DerivedTask && xx.isGoal()) {
//               System.out.println(xx);
//           }
//        });

        int N = 2;

        n.clear();

        while (x.v != 2) {

            for (int i = 0; i < 4; i++) {

                x.set(i % N);

                n.run(1);

                x.get();

                n.run(10);
            }

            n.clear();

            n.input("$1.0 SimpleClass(get,x,(),2)! :|:");
//            n.input("$1.0 SimpleClass(get,x,(),2)! :|:");
//            n.input("$1.0 SimpleClass(get,x,(),_)! :|:");
//        n.input("$1.0 --SimpleClass(get,x,(),0)! :|:");
//        n.input("$1.0 --SimpleClass(get,x,(),1)! :|:");
            n.run(500);
        }
//        n.input("$0.5 (0<->1)?");
//        n.input("$0.5 (1<->2)?");
//        n.input("$0.5 (2<->3)?");
//        n.input("$0.5 (3<->4)?");
//        n.input("$1.0 (SimpleClass(set,x,$x) ==> SimpleClass(get,x,(),$x))?");
        //n.run(100);


//        n.tasks().forEachOrdered(z -> {
//            System.out.println(z);
//        });

    }
}