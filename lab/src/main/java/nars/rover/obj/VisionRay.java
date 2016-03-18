package nars.rover.obj;

import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.gl.JoglDraw;
import nars.rover.physics.j2d.LayerDraw;
import nars.rover.robot.AbstractPolygonBot;
import nars.rover.robot.Being;
import nars.rover.util.RayCastClosestCallback;
import nars.util.data.Util;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;

/**
 * Created by me on 1/31/16.
 */
abstract public class VisionRay implements AbstractPolygonBot.Sense, LayerDraw {

    protected final int resolution;
    protected final float arc;
    final Vec2 point; //where the retina receives vision at
    public final float angle;
    private final Body base;
    private final Being bot;

    public float seenDist;

    ///final Color3f laserUnhitColor = new Color3f(0.25f, 0.25f, 0.25f);
    final Color3f laserHitColor = new Color3f(0.25f, 0.25f, 0.25f);


    public Color3f sparkColor;
    public Color3f normalColor;
    protected float distance;


    final RayDrawer[] rayDrawers;

    float biteDistanceThreshold = 0.025f;
    private boolean eats;
    protected Body hitNext;

    private String hitMaterial;
    //public float hitDist;

    //final Sensor sensor;

    public VisionRay(Vec2 point, float angle, float arc, Body base, float length, int resolution) {
        this.base = base;

        bot = ((Being.BeingMaterial)base.getUserData()).robot;
        //this.sensor = new Sensor();

        this.point = point;
        this.angle = angle;
        this.arc = arc;
        this.distance = length;
        this.resolution = resolution;
        this.rayDrawers = new RayDrawer[resolution]; /** one for each sub-pixel */
        for (int i = 0; i < resolution; i++)
            rayDrawers[i] = new RayDrawer(base.getWorld(), i, angle, arc/resolution);

        sparkColor = new Color3f(0.5f, 0.4f, 0.4f);
        normalColor = new Color3f(0.2f, 0.2f, 0.2f);

    }



    //TODO color(Body hit)

    public static String material(Body hit) {
        if (hit == null) return "nothing";
        Object d = hit.getUserData();
        return d != null ? d.toString() : "something";
    }


    public void step(boolean feel, boolean drawing) {

        //final Robotic ap = this.bot;


//        root.set( point );
//        root = base.getWorldPoint( root );

        for (RayDrawer r : rayDrawers)
            r.update();

    }

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
        if (touched == null) return;

        if (isEating() && touched.getUserData() instanceof Sim.Edible) {

            //System.out.println(di + " " + isEating() + " " + touched);

            //if (eats) {


            if (di <= biteDistanceThreshold) {
                bot.eat(touched);
            }

                            /*} else if (di <= tasteDistanceThreshold) {
                                //taste(touched, di );
                            }*/
            //}
        }
    }

    protected boolean isEating() {
        return eats;
    }


    public final class RayDrawer extends RayCastClosestCallback implements RayCastCallback {

        /** STATE definitely exploiting mutability here */
        public final Vec2 from = new Vec2();
        public final Vec2 to = new Vec2();
        public final Color3f color = new Color3f(0.5f, 0.5f, 0.5f); //current ray color

        private final float baseAngle;
        private final float dArc;
        private final World world;
        private final int id;
        private final float targetAngle;
        private float hitDist;


        public RayDrawer(World world, int id, float baseAngle, float dArc) {
            this.baseAngle = baseAngle;
            this.dArc = dArc;
            this.world = world;
            this.id = id;
            float da = (-arc / 2f) + dArc * id;
            this.targetAngle = da + angle + baseAngle;
        }

        @Override
        public final float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
            Body body = fixture.getBody();

            //System.out.println(body + " " + base);

            //ignore self:
            if (body == base) return 1;
            Object userData = body.getUserData();
            if (userData!=null && (userData instanceof Being.BeingMaterial) && userData.toString().equals(bot.id))
                return 1;

            return super.reportFixture(fixture, point, normal, fraction);
        }

        public void update() {
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

                    color.set(laserHitColor);
                    color.z = Math.min(1.0f, color.z + 0.75f * (1.0f - d));

                perceiveDist(body, d);
            } else {
                m_hit = false;
                body = null;
                perceiveDist(null, Float.POSITIVE_INFINITY);
                color.set(normalColor);
            }

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
    public void drawGround(JoglAbstractDraw d, World w) {

    }

    @Override
    public void drawSky(JoglAbstractDraw d, World w) {
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
                0.75f /* alpha */, 2f /* width */,
                1f /* z */);
    }

    public void setEats(boolean b) {
        this.eats = b;
    }

    public boolean hit(String material) {
        String m = hitMaterial;       
        return m!=null && material.equals(m);
    }

//    /**  (touching) 0..1.0 (max range) */
//    public float distToHit() {
//        return hitDist;
//    }
}
