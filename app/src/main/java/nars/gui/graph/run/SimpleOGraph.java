package nars.gui.graph.run;

import jcog.O;
import jcog.random.XORShiftRandom;
import nars.nar.Default;
import nars.time.CycleTime;
import nars.util.exe.BufferedSynchronousExecutor;
import org.jetbrains.annotations.Nullable;

public class SimpleOGraph {

    public static void main(String[] args) {
        O o = O.of(
                BufferedSynchronousExecutor.class,
                CycleTime.class,
                new Default.DefaultTermIndex(1024),
                XORShiftRandom.class
        );
        try {
            @Nullable Default x = o.the(Default.class);
        } catch (Exception e) { }

        new SimpleGraph1(15).commit(o.how).show(1200,600);


    }
}