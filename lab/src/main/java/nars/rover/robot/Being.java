package nars.rover.robot;

import nars.NAR;
import nars.rover.Material;
import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import nars.util.data.list.FasterList;
import org.jbox2d.common.Color3f;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import java.awt.*;
import java.util.*;

/**
 * Human, Animal, Machine, Alien, Ghost
 */
abstract public class Being {

    public Body torso;
    //public class ChangedNumericInput //discretizer
    public Sim sim;
    public final String id;

    float mass = 1f;

    public Being(String id) {
        this.id = id;
    }

    public void init(Sim p) {
        this.sim = p;

        this.torso = newTorso();
    }


    public String getID() {
        return id;
    }

    public abstract BeingMaterial getMaterial();

    /** create the body and return its central component */
    protected abstract Body newTorso();

    public World getWorld() {
        return sim.getWorld();
    }

    public void step(int i) {

    }

    @Deprecated public void eat(Body touched) {


    }


    public static class BeingMaterial extends Material {


        protected final Color3f color;
        public final Being robot;
        public final java.util.List<LayerDraw> layers = new FasterList<>();

        public BeingMaterial(Being r) {
            super();
            this.robot = r;

            float h = (getID().hashCode() % 10) / 10f;
            Color c = Color.getHSBColor(h, 0.5f, 0.95f);
            color = new Color3f(c.getRed()*255f, c.getGreen()*255f, c.getBlue()*255f);
        }

        public BeingMaterial clone() {
            return new BeingMaterial(robot);
        }

        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {
//            color.set(color.x,
//                    color.y,
//                    color.z);


            World w = b.m_world;

            java.util.List<LayerDraw> layers = this.layers;
            int layersSize = layers.size();

            for (int i = 0; i < layersSize; i++) {
                layers.get(i).drawGround(d, w);
            }
            d.setFillColor(color);
        }

        @Override
        public void after(Body b, JoglAbstractDraw d, float time) {
            java.util.List<LayerDraw> layers = this.layers;
            int layersSize = layers.size();
            World w = b.m_world;
            for (int i = 0; i < layersSize; i++) {
                layers.get(i).drawSky(d, w);
            }
        }

        @Override
        public String toString() {
            return getID();
        }

        public String getID() {
            return robot.getID();
        }
    }

    public static class NARRoverMaterial extends BeingMaterial {

        private final NAR nar;
        float tone;

        public NARRoverMaterial(Being r, NAR nar) {
            super(r);
            this.nar = nar;
            tone = 1f;
        }

        public NARRoverMaterial clone(float withTone) {
            NARRoverMaterial m = new NARRoverMaterial(robot, nar);
            m.tone = withTone;
            return m;
        }

        @Override
        public void before(Body b, JoglAbstractDraw d, float time) {
            float bb = /*nar.memory.emotion.busy()* */  0.5f + 0.5f;
            //color.set(c.getRed()/256.0f * bb, c.getGreen()/256.0f * bb, c.getBlue()/256.0f * bb);
            float hh = /*nar.memory.emotion.happy() * */  0.5f + 0.3f;

            color.set(bb, hh, (tone) * 0.3f);

            d.setFillColor(color);

        }


    }
}
