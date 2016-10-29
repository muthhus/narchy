package nars.gui;

import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap;
import com.jogamp.newt.opengl.GLWindow;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.nar.Default;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import spacegraph.*;
import spacegraph.layout.Flatten;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.broad.Broadphase;
import spacegraph.phys.constraint.BroadConstraint;
import spacegraph.phys.shape.CollisionShape;
import spacegraph.phys.shape.SphereShape;
import spacegraph.phys.util.OArrayList;
import spacegraph.render.Draw;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static nars.Symbols.BELIEF;

/**
 * Created by me on 10/28/16.
 */
public class NARTime {


    public static void main(String[] args) {

        Default n = new Default(512, 4, 1, 3);
        n.nal(6);

        newTimelineWindow(n,  128, 5);

        n.log();
        n.input("(a-->b).", "(b-->c)."
        //        ,"(c-->d).", "(d-->e).", "(e --> f)."
        );

        //n.loop(15f);
        n.run(128);

    }

    public static class TaskNode extends ConceptWidget {

        final Task task;

        public TaskNode(Task x, NAR nar) {
            super($.quote(x.toStringWithoutBudget(nar)), nar);
            this.task = x;
        }

    }
    public static class EvidenceNode extends ConceptWidget {

        public EvidenceNode(long e, NAR nar) {
            super($.the("ev(" + e + ")"), nar);
        }

    }

    public static GLWindow newTimelineWindow(NAR nar, int maxNodes, int maxEdges) {

        SpaceGraph s = new SpaceGraph<>();

        LongObjectHashMap<EvidenceNode> evi = new LongObjectHashMap();
        Map<Term, ConceptWidget> con = new HashMap();

        ListSpace<Term,Spatial<Term>> space = new ListSpace();
        space.with(new Flatten());
        nar.onTask(t -> {
            TaskNode tn = new TaskNode(t, nar);
            space.active.add(tn);

            Term ttt = t.concept(nar).term();
            con.computeIfAbsent(ttt, (tttt)->{
                ConceptWidget cw = new ConceptWidget(tttt, nar) {
                    @Override
                    protected CollisionShape newShape() {
                        return new SphereShape(0.1f);
                    }
                };

                space.active.add(cw);
                return cw;
            }).addEdge(tn.task.budget(), tn).attraction = 1;

            for (long x : t.evidence()) {
                if (x==Long.MAX_VALUE /* cyclic */)
                    continue;

                EvidenceNode en = evi.getIfAbsentPut(x, () -> {
                    EvidenceNode enn = new EvidenceNode(x, nar);
                    enn.scale(0.5f, 0.5f, 0.5f);
                    space.active.add(enn);
                    return enn;
                });
                en.shapeColor[0] = 0.25f;
                en.shapeColor[1] = 0.25f;
                en.shapeColor[2] = 0.25f;


                EDraw e = en.addEdge(tn.task.budget(), tn );

                float[] f = Draw.hsb(((((x * 31) ^ 117)) % 64) / 64f, 0.5f, 0.5f, 0f, new float[4]);
                e.r = f[0];
                e.g = f[1];
                e.b = f[2];
                e.a = tn.pri;
                e.width = 1;
                e.attraction = 0.1f;

            }
        });


        s.add(space);



        s.dyn.addBroadConstraint((broadphase, objects, timeStep) -> objects.forEach(x -> {

            Object data = x.data();
            if (data instanceof TaskNode) {
                TaskNode ss = (TaskNode) data;
                Task tt = ss.task;
                float pp = 0.5f + 5f * tt.pri();
                ss.scale(pp, pp, pp);
                float r = 0.5f, g = 0.5f, b = 0.5f, a = 1f;
                switch (tt.punc()) {
                    case BELIEF:
                        float freq = tt.freq();
                        g = freq;
                        r = 1f - freq;
                        b = 0.25f * tt.qua();
                        a = 0.1f + tt.conf() * 0.9f;
                        break;
                }
                ss.shapeColor[0] = r;
                ss.shapeColor[1] = g;
                ss.shapeColor[2] = b;
                ss.shapeColor[3] = a;
                ss.moveX(tt.creation(), 0.1f);
            }
//            if (data instanceof EvidenceNode) {
//                ((SimpleSpatial)data).moveZ(-10f, 0.1f);
//            }


        }));

        s.dyn.addBroadConstraint(new ForceDirected());

        return s.show(1300, 900);
    }

}
