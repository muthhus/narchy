package spacegraph.layout;

import spacegraph.*;

/**
 * Created by me on 6/21/16.
 */
public class Spiral<O> implements SpaceTransform<O> {

    float nodeSpeed = 0.3f;
    private int order;


    @Override
    public void update(SpaceGraph<O> g, AbstractSpace<O, ?> src, float dt) {
        this.order = 0;
        src.forEach(this::update);
    }


    protected void update(Spatial v) {
        //TODO abstract
        //int hash = v.hash;
        //int vol = v.key.volume();

        //float ni = n / (float) Math.E;
        //final float bn = 1f;

        float baseRad = 10f;
        float angleRate = 0.2f;
        //float p = v.pri;

        //float nodeSpeed = (this.nodeSpeed / (1f + v.pri));

        int o = order++;


        float angle = o * angleRate;
        float r = baseRad + o * angleRate * 1.6f /* ~phi */ ;
        ((SimpleSpatial)v).move(
            (float) (Math.sin(angle) * r),
            (float) (Math.cos(angle) * r),
            0,
            nodeSpeed
        );


        //1f/(1f+v.lag) * (baseRad/2f);
                //v.budget.qua() * (baseRad + rad)
                //v.tp[2] = act*10f;
                //nodeSpeed);

    }

}
