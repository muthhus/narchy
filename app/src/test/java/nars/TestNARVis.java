package nars;

import nars.nal.nal1.NAL1Test;
import nars.test.TestNAR;
import nars.util.AbstractNALTest;
import spacegraph.Surface;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestNARVis extends Surface {

    public TestNARVis(AbstractNALTest a, String method) throws NoSuchMethodException {
        Method m = a.getClass().getMethod(method);
        m.trySetAccessible();

        TestNAR n = a.test;

        try {
            m.invoke(a);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }

        //test has been setup, but not run
        n.succeedsIfAll.forEach(s -> {
            System.out.println("+" + s);
        });
        n.failsIfAny.forEach(s -> {
            System.out.println("-" + s);
        });

        //run test
        n.run();

        //after state ---

    }

    public static void main(String[] args) throws NoSuchMethodException {
        new TestNARVis(new NAL1Test(), "deduction");
    }
}
