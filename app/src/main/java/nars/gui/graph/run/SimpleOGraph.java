package nars.gui.graph.run;

import jcog.O;
import jcog.random.XORShiftRandom;
import nars.concept.Concept;
import nars.gui.graph.ConceptSpace;
import nars.gui.graph.ConceptWidget;
import nars.nar.Default;
import nars.term.Term;
import nars.time.CycleTime;
import nars.util.exe.BufferedSynchronousExecutor;
import spacegraph.Surface;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.Draw;
import spacegraph.render.JoglPhysics;

public class SimpleOGraph {

    public static void main(String[] args) {
        O o = O.of(
                BufferedSynchronousExecutor.class,
                CycleTime.class,
                new Default.DefaultTermIndex(1024),
                XORShiftRandom.class
        );

        new SimpleGraph1(15).commit(o.how).show(1200,600);


    }
}