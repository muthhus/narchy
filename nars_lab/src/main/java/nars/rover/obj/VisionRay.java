package nars.rover.obj;

import nars.Global;
import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.SwingDraw;
import nars.rover.robot.AbstractPolygonBot;
import nars.rover.util.RayCastClosestCallback;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by me on 1/31/16.
 */
abstract public class VisionRay extends RayCastClosestCallback implements AbstractPolygonBot.Sense, SwingDraw.LayerDraw {

    protected final int resolution;
    protected final float arc;
    final Vec2 point; //where the retina receives vision at
    final float angle;
    protected float conf;

    final Color3f laserUnhitColor = new Color3f(0.25f, 0.25f, 0.25f);
    final Color3f laserHitColor = new Color3f(laserUnhitColor.x, laserUnhitColor.y, laserUnhitColor.z);
    final Color3f rayColor = new Color3f(); //current ray color

    private final AbstractPolygonBot bot;
    public Color3f sparkColor = new Color3f(0.4f, 0.9f, 0.4f);
    public Color3f normalColor = new Color3f(0.9f, 0.9f, 0.4f);
    protected float distance;

    List<Runnable> preDraw = Global.newArrayList();
    List<Runnable> toDraw =
            //new ConcurrentLinkedDeque<>();
            new CopyOnWriteArrayList();
    float biteDistanceThreshold = 0.03f;
    private boolean eats;
    protected Body hitNext;
    private Vec2 root = new Vec2();
    private Vec2 target = new Vec2();
    private String hitMaterial;
    private float hitDist;

    //final Sensor sensor;

    public VisionRay(Vec2 point, float angle, float arc, AbstractPolygonBot bot, float length, int resolution) {
        this.bot = bot;

        //this.sensor = new Sensor();

        this.point = point;
        this.angle = angle;
        this.arc = arc;
        this.distance = length;
        this.resolution = resolution;

        sparkColor = new Color3f(0.5f, 0.4f, 0.4f);
        normalColor = new Color3f(0.4f, 0.4f, 0.4f);
    }



    private static void drawIt(Vec2 start, float alpha, Vec2 finalEndPoint, float r, float g, float b, float thick, JoglAbstractDraw dd) {
        dd.drawSegment(start, finalEndPoint, r, g, b, alpha, 1f * thick);
    }

    public static String material(Body hit) {
        if (hit == null) return "nothing";
        Object d = hit.getUserData();
        return d != null ? d.toString() : "something";
    }


    public void step(boolean feel, boolean drawing) {

        final AbstractPolygonBot ap = this.bot;

        preDraw.clear();

        final float distance = getDistance();
        float minDist = distance * 1.1f; //far enough away
        float totalDist = 0;
        float dArc = arc / resolution;

        float angOffset = getLocalAngle(); //(float)Math.random() * (-arc/4f);

        JoglAbstractDraw dd = ((JoglAbstractDraw) ap.getDraw());

        root.set( point );
        root = bot.torso.getWorldPoint( root );

        Body hit = null;
        for (int r = 0; r < resolution; r++) {


            target.set( root );
            float da = (-arc / 2f) + dArc * r + angOffset;
            final float V = da + angle + bot.torso.getAngle();
            target.addLocal(distance * (float) Math.cos(V), distance * (float) Math.sin(V));

            init();

            try {
                ap.getWorld().raycast(this, root, target);
            } catch (Exception e) {
                System.err.println("Phys2D raycast: " + e + " " + root + " " + target);
                e.printStackTrace();
            }

            Vec2 endPoint;

            if (m_hit) {
                float d = m_point.sub(root).length() / distance;

                if (drawing) {
                    rayColor.set(laserHitColor);
                    rayColor.x = Math.min(1.0f, laserUnhitColor.x + 0.75f * (1.0f - d));
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
                totalDist += d;
                if (d < minDist) {
                    hit = body;
                    minDist = d;
                }

                endPoint = m_point;

            } else {
                rayColor.set(normalColor);
                totalDist += 1;
                endPoint = target;
            }

            if ((drawing) && (endPoint != null)) {

                //final float alpha = rayColor.x *= 0.2f + 0.8f * (senseActivity + conceptPriority)/2f;
                //rayColor.z *= alpha - 0.35f * senseActivity;
                //rayColor.y *= alpha - 0.35f * conceptPriority;


                updateColor(rayColor);
                Vec2 finalEndPoint = endPoint.clone();

                preDraw.add(() -> drawIt(root, 0.5f /* alpha */, finalEndPoint,
                        rayColor.x, rayColor.y, rayColor.z, 2f /* thickness */, dd));

            }
        }
        if (hit != null) {
            float meanDist = totalDist / resolution;
            float percentDiff = (float) Math.sqrt(Math.abs(meanDist - minDist));
            float conf = 0.8f + 0.2f * (1.0f - percentDiff);
            if (conf > 0.99f) {
                conf = 0.99f;
            }

            //perceiveDist(hit, conf, meanDist);
            perceiveDist(hit, conf, meanDist);

            //if (isEating())
              //  System.out.println(isEating() + " "  + hit + " " + meanDist + " " + biteDistanceThreshold + " eat?");

        } else {
            perceiveDist(hit, 0.9f, 1.0f);
        }

        toDraw.clear();
        toDraw.addAll(preDraw);





    }

    public float getLocalAngle() {
        return 0f;
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



    protected float getDistance() {
        return distance;
    }

    public void onTouch(Body touched, float di) {
        if (touched == null) return;

        if (isEating() && touched.getUserData() instanceof Sim.Edible) {

            //System.out.println(di + " " + isEating() + " " + touched);

            //if (eats) {


            if (di <= biteDistanceThreshold)
                bot.eat(touched);

                            /*} else if (di <= tasteDistanceThreshold) {
                                //taste(touched, di );
                            }*/
            //}
        }
    }

    protected boolean isEating() {
        return eats;
    }


    @Override
    public void drawGround(JoglAbstractDraw d, World w) {
        toDraw.forEach(Runnable::run);
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

    /**  (touching) 0..1.0 (max range) */
    public float distToHit() {
        return hitDist;
    }
}
