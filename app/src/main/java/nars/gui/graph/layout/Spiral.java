package nars.gui.graph.layout;

import nars.gui.graph.Atomatter;
import nars.gui.graph.GraphSpace;
import nars.gui.graph.GraphTransform;

import java.util.List;

/**
 * Created by me on 6/21/16.
 */
public class Spiral<O> implements GraphTransform<O> {

    float nodeSpeed = 0.05f;

    @Override
    public void update(GraphSpace<O> g, List<Atomatter<O>> verts, float dt) {
        verts.forEach(this::update);
    }


    protected void update(Atomatter v) {
        //TODO abstract
        //int hash = v.hash;
        //int vol = v.key.volume();

        //float ni = n / (float) Math.E;
        //final float bn = 1f;

        float baseRad = 5f;
        //float p = v.pri;

        float nodeSpeed = (this.nodeSpeed / (1f + v.pri));

        int o = v.order;
        float theta = o;

        v.move(
                (float) Math.sin(theta / 10f) * (baseRad + 0.2f * (theta)),
                (float) Math.cos(theta / 10f) * (baseRad + 0.2f * (theta)),
                0);
                //1f/(1f+v.lag) * (baseRad/2f);
                //v.budget.qua() * (baseRad + rad)
                //v.tp[2] = act*10f;
                //nodeSpeed);

    }

}
