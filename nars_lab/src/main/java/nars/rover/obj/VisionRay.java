package nars.rover.obj;

import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import nars.rover.robot.AbstractPolygonBot;
import nars.rover.robot.Being;
import nars.rover.util.RayCastClosestCallback;
import org.jbox2d.callbacks.RayCastCallback;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.Fixture;
import org.jbox2d.dynamics.World;

/**
 * Created by me on 1/31/16.
 */
abstract public class VisionRay extends RayCastClosestCallback implements AbstractPolygonBot.Sense, LayerDraw {

    protected final int resolution;
    protected final float arc;
    final Vec2 point; //where the retina receives vision at
    public final float angle;
    private final Body base;
    private final Being bot;
    protected float conf;

    final Color3f laserUnhitColor = new Color3f(0.25f, 0.25f, 0.25f);
    final Color3f laserHitColor = new Color3f(laserUnhitColor.x, laserUnhitColor.y, laserUnhitColor.z);


    public Color3f sparkColor = new Color3f(0.4f, 0.9f, 0.4f);
    public Color3f normalColor = new Color3f(0.9f, 0.9f, 0.4f);
    protected float distance;


    final RayDrawer[] rayDrawers;

    float biteDistanceThreshold = 0.03f;
    private boolean eats;
    protected Body hitNext;

    private String hitMaterial;
    public float hitDist;

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

        sparkColor = new Color3f(0.5f, 0.4f, 0.4f);
        normalColor = new Color3f(0.4f, 0.4f, 0.4f);
    }


    public static String material(Body hit) {
        if (hit == null) return "nothing";
        Object d = hit.getUserData();
        return d != null ? d.toString() : "something";
    }


    public void step(boolean feel, boolean drawing) {

        //final Robotic ap = this.bot;


//        root.set( point );
//        root = base.getWorldPoint( root );

        for (int r = 0; r < resolution; r++) {

            RayDrawer rd = rayDrawers[r];
            if (rd!=null)
                rd.update();
        }



    }

    protected void perceiveDist(Body hit, float newConf, float nextHitDist) {

        this.hitNext = hit;
        this.hitMaterial = material(hit);
        this.hitDist = nextHitDist;
        

        //hitDist = (distMomentum * hitDist) + (1f - distMomentum) * nextHitDist;
        //conf = (confMomentum * conf) + (1f - confMomentum) * newConf;
        conf = newConf;

        //System.out.println(angleTerm + " "+ hitDist + " " + conf);

        onTouch(hit, nextHitDist);

    }

    abstract protected void updateColor(Color3f rayColor);


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


    public class RayDrawer extends RayCastClosestCallback implements RayCastCallback {

        /** STATE definitely exploiting mutability here */
        public final Vec2 from = new Vec2();
        public final Vec2 to = new Vec2();
        public final Color3f color = new Color3f(0.5f, 0.5f, 0.5f); //current ray color

        private final float baesAngle;
        private final float dArc;
        private final World world;
        private final int id;
        private final float targetAngle;
        private float hitDist;


        public RayDrawer(World world, int id, float baseAngle, float dArc) {
            this.baesAngle = baseAngle;
            this.dArc = dArc;
            this.world = world;
            this.id = id;
            float da = (-arc / 2f) + dArc * id;
            this.targetAngle = da + angle + baseAngle;
        }


        public void update() {
            to.set(from);
            to.addLocal(distance * (float) Math.cos(targetAngle), distance * (float) Math.sin(targetAngle));


            init();

            try {
                world.raycast(this, from, to);
            } catch (Exception e) {
                System.err.println("Phys2D raycast: " + e + " " + from + " " + to);
                e.printStackTrace();
            }




            float minDist = distance * 1.1f; //far enough away

            Vec2 endPoint;
            float hitDist = super.m_point.euclideanDistance(from);
            if (hitDist > minDist) {
                body = null;
                this.hitDist = minDist; //anything has to be at least this far away
            } else {
                //retain the body it touched
                this.hitDist = hitDist;
            }

            if (m_hit) {
                float d = m_point.sub(from).length() / distance;

                /*if (drawing)*/ {
                    color.set(laserHitColor);
                    color.z = Math.min(1.0f, laserUnhitColor.x + 0.75f * (1.0f - d));
                    //Vec2 pp = ccallback.m_point.clone();
//                        toDraw.add(new Runnable() {
//                            @Override public void run() {
//
//                                getDraw().drawPoint(pp, 5.0f, sparkColor);
//
//                            }
//                        });

                }

                //pooledHead.set(ccallback.m_normal);
                //pooledHead.mulLocal(.5f).addLocal(ccallback.m_point);
                //draw.drawSegment(ccallback.m_point, pooledHead, normalColor, 0.25f);
                if (d < minDist) {
                    minDist = d;
                } else {
                    m_hit = false; //didnt actually hit
                }

                endPoint = m_point;

            } else {
                color.set(normalColor);
                endPoint = to;
            }

            if (/*(drawing)&&*/  (endPoint != null)) {

                //final float alpha = rayColor.x *= 0.2f + 0.8f * (senseActivity + conceptPriority)/2f;
                //rayColor.z *= alpha - 0.35f * senseActivity;
                //rayColor.y *= alpha - 0.35f * conceptPriority;


                updateColor(color);



            }

//            if (m_hit) {
//                float meanDist = totalDist / resolution;
//                float percentDiff = (float) Math.sqrt(Math.abs(meanDist - minDist));
//                float conf = 0.8f + 0.2f * (1.0f - percentDiff);
//                if (conf > 0.99f) {
//                    conf = 0.99f;
//                }
//
//                //perceiveDist(hit, conf, meanDist);
//                perceiveDist(body, conf, meanDist);
//
//                //if (isEating())
//                //  System.out.println(isEating() + " "  + hit + " " + meanDist + " " + biteDistanceThreshold + " eat?");
//
//            } else {
            perceiveDist(body, 0.9f, 1.0f);
//            }

        }


        @Override
        public float reportFixture(Fixture fixture, Vec2 point, Vec2 normal, float fraction) {
            return 0;
        }
    }

    @Override
    public void drawGround(JoglAbstractDraw d, World w) {
        for (RayDrawer r : rayDrawers) {

            Color3f c = r.color;
            d.drawSegment(
                    r.from, r.to,
                    c.x, c.y, c.z,
                    0.5f, 1f * 2f);

        }
    }

    @Override
    public void drawSky(JoglAbstractDraw d, World w) {

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
