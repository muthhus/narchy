package nars.gui.graph.run;

import jcog.O;
import nars.nar.Default;
import nars.time.CycleTime;
import nars.util.exe.BufferedSynchronousExecutor;

public class SimpleOGraph {

    public static void main(String[] args) {
        O o = O.of(
                BufferedSynchronousExecutor.class,
                CycleTime.class,
                new Default.DefaultTermIndex(1024)
        );
        SimpleGraph1 cs = new SimpleGraph1(5);
        cs.show(800, 600);
        cs.commit(o.how);

    }
}
