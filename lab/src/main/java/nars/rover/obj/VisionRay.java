package nars.rover.obj;

import com.artemis.Component;
import com.artemis.Entity;
import com.gs.collections.api.block.procedure.primitive.FloatObjectProcedure;
import nars.$;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.gl.JoglDraw;
import nars.rover.physics.j2d.LayerDraw;
import nars.rover.util.RayCastClosestCallback;
import nars.term.Term;
import nars.util.data.Util;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World2D;

/**
 * Created by me on 1/31/16.
 */
public class VisionRay extends Component implements LayerDraw {

    protected int resolution;
    protected float arc;
    Vec2 point; //where the retina receives vision at
    public float angle;
    private Body base;
    private FloatObjectProcedure<Color3f> colorizer;

    public float seenDist;

    protected float distance;


    public RayDrawer[] rayDrawers;

    float biteDistanceThreshold = 0.03f;
    private boolean eats;
    protected Body hitNext;

    private Term hitMaterial = null;
    //public float hitDist;

    //final Sensor sensor;

    public VisionRay() {

    }
    public VisionRay(Vec2 point, float angle, float arc, Body base, float length, int resolution, FloatObjectProcedure<Color3f> colorizer) {
        this.base = base;


        this.point = point;
        this.angle = angle;
        this.arc = arc;
        this.distance = length;
        this.resolution = resolution;
        this.rayDrawers = new RayDrawer[resolution]; /** one for each sub-pixel */
        for (int i = 0; i < resolution; i++)
            rayDrawers[i] = new RayDrawer(i, angle, arc);


        this.colorizer = colorizer;
    }



    //TODO color(Body hit)

    public static Term material(Body hit) {
        if (hit == null) return VisionRay.nothing;
        Entity d = (Entity)hit.getUserData();
        Material m = d.getComponent(Material.class);
        if (m!=null)
            return m.term();
        return something;
    }

    public final static Term something = $.the("something");
    public final static Term nothing = $.the("nothing");


    protected void perceiveDist(Body hit, float hitDist) {

        if ((this.hitNext = hit)!=null) {
            this.hitMaterial = material(hit);
            this.seenDist = hitDist;
        } else {
            this.hitMaterial = null;
            this.seenDist = Float.POSITIVE_INFINITY;
        }

        //hitDist = (distMomentum * hitDist) + (1f - distMomentum) * nextHitDist;
        //conf = (confMomentum * conf) + (1f - confMomentum) * newConf;


        //System.out.println(angleTerm + " "+ hitDist + " " + conf);

        onTouch(hit, hitDist);

    }



    public void onTouch(Body touched, float di) {
        if (touched == null || !eats) return;

//        if (isEating() && touched.getUserData() instanceof Sim.Edible) {
//
//            //System.out.println(di + " " + isEating() + " " + touched);
//
//            //if (eats) {
//
//
            if (di <= biteDistanceThreshold) {
                //bot.eat(touched);
                Entity eaten = (Entity)(touched.getUserData());
                Edible e = eaten.getComponent(Edible.class);
                if (e!=null) {
                    Entity eater = (Entity) (base.getUserData());
                    Health h = eater.getComponent(Health.class);
                    h.ingest(e);
                }

            }
//
//                            /*} else if (di <= tasteDistanceThreshold) {
//                                //taste(touched, di );
//                            }*/
//            //}
//        }
    }



    public final class RayDrawer extends RayCastClosestCallback implements RayCastCallback {

        /** STATE definitely exploiting mutability here */
        public final Vec2 from = new Vec2();
        public final Vec2 to = new Vec2();
        public final Color3f color = new Color3f(0.5f, 0.5f, 0.5f); //current ray color

        private final int id;
        private final float targetAngle;
        private float hitDist;


        public RayDrawer(int id, float baseAngle, float dArc) {
            this.id = id;
            float da = /*(-arc / 2f) +*/ dArc * id;
            this.targetAngle = da + angle + baseAngle;
        }

        @Override
        public final float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
            Body body = fixture.getBody();

            //System.out.println(body + " " + base);

            //ignore self:
            if (body == base) return 1;
            //Object userData = body.getUserData();

//            if (userData!=null && (userData instanceof Being.BeingMaterial) && userData.toString().equals(bot.id))
//                return -1;

            return super.reportFixture(fixture, point, normal, fraction);
        }

        public void update(World2D world) {
            base.getWorldPointToOut(point, from);//getWorldCenter());


            to.set(from);
            float angle = targetAngle + base.getAngle();
            to.addLocal(distance * (float) Math.cos(angle), distance * (float) Math.sin(angle));


            m_hit = false;

            try {
                world.raycast(this, from, to);
            } catch (Exception e) {
                System.err.println("Phys2D raycast: " + e + " " + from + " " + to);
                e.printStackTrace();
            }


            if (m_hit) {
                this.hitDist = super.m_point.euclideanDistance(from);

                float d = hitDist / distance;

                to.set(m_point);

                perceiveDist(body, d);
            } else {
                m_hit = false;
                body = null;
                perceiveDist(null, Float.POSITIVE_INFINITY);

            }


            colorizer.value(seenDist, color);
            color.x = Util.clamp(color.x);
            color.y = Util.clamp(color.y);
            color.z = Util.clamp(color.z);

        }

//
//        @Override
//        public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
//            return 0;
//        }
    }

    @Override
    public void drawGround(JoglAbstractDraw d, World2D w) {

    }

    @Override
    public void drawSky(JoglAbstractDraw d, World2D w) {
        JoglDraw dd = (JoglDraw)d;
        for (RayDrawer r : rayDrawers) {

            Color3f c = r.color;
            drawRay(dd, r, c);

        }
    }

    public void drawRay(JoglDraw dd, RayDrawer r, Color3f c) {
        dd.drawSegment(
                r.from, r.to,
                c.x, c.y, c.z,
                0.85f /* alpha */, 2f /* width */,
                2f /* z */);
    }

    public void setEats(boolean b) {
        this.eats = b;
    }

    public final boolean hit(Term material) {
        Term m = hitMaterial;
        return m!=null && material.equals(m);
    }

//    /**  (touching) 0..1.0 (max range) */
//    public float distToHit() {
//        return hitDist;
//    }
}
