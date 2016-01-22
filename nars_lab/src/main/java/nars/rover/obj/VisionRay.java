package nars.rover.obj;

import nars.$;
import nars.bag.BLink;
import nars.concept.Concept;
import nars.nar.Default;
import nars.rover.Sim;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.SwingDraw;
import nars.rover.robot.AbstractPolygonBot;
import nars.rover.util.RayCastClosestCallback;
import nars.task.MutableTask;
import nars.term.Termed;
import nars.term.atom.Atom;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.World;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Created by me on 8/11/15.
 */
public class VisionRay implements AbstractPolygonBot.Sense, SwingDraw.LayerDraw {

    private final AbstractPolygonBot abstractPolygonBot;
    final Vec2 point; //where the retina receives vision at
    final float angle;
    protected float distance;
    //final ChangedTextInput sight =
            //new SometimesChangedTextInput(nar, minVisionInputProbability);
            //new ChangedTextInput(abstractPolygonBot.nar);
    //private final String seenAngleTerm;

    RayCastClosestCallback ccallback = new RayCastClosestCallback();
    private final Body body;
    private final int resolution;
    private final float arc;
    final Color3f laserUnhitColor = new Color3f(0.25f, 0.25f, 0.25f);
    final Color3f laserHitColor = new Color3f(laserUnhitColor.x, laserUnhitColor.y, laserUnhitColor.z);
    public Color3f sparkColor = new Color3f(0.4f, 0.9f, 0.4f);
    public Color3f normalColor = new Color3f(0.9f, 0.9f, 0.4f);
    final Color3f rayColor = new Color3f(); //current ray color
    public final Atom angleTerm;
    private final float distMomentum = 0f;
    private float hitDist;
    private Body hit;
    private final float confMomentum = 0;
    private float conf;
    private BLink<Concept> angleConcept;


    public VisionRay(AbstractPolygonBot abstractPolygonBot, Body body, Vec2 point, float angle, float arc, int resolution, float length) {
        this.abstractPolygonBot = abstractPolygonBot;
        this.body = body;
        this.point = point;
        this.angle = angle;
        this.angleTerm = $.the(Sim.angleTerm(angle));
        //this.seenAngleTerm = //"see_" + sim.angleTerm(angle);
        this.arc = arc;
        this.resolution = resolution;
        this.distance = length;
    }


    Collection<Runnable> toDraw =
            //new ConcurrentLinkedDeque<>();
            new CopyOnWriteArrayList();


    public void step(boolean feel, boolean drawing) {
        toDraw.clear();

        float conceptPriority;
        float conceptDurability;
        float conceptQuality;


        if (angleConcept == null)
            angleConcept = ((Default)abstractPolygonBot.nar).core.active.get(angleTerm);

        if (angleConcept != null) {
            conceptPriority = 0.5f + 0.5f * angleConcept.getPriority();
            conceptDurability = 0.5f + 0.5f * angleConcept.getDurability();
            conceptQuality = 0.5f + 0.5f * angleConcept.getQuality();

            //sight.setProbability(Math.max(minVisionInputProbability, Math.min(1.0f, maxVisionInputProbability * conceptPriority)));
            //sight.setProbability(minVisionInputProbability);
        } else {
            conceptPriority = 0.5f;
            conceptDurability = 0.5f;
            conceptQuality = 0.5f;
        }

        abstractPolygonBot.point1 = body.getWorldPoint(point);
        Body hit = null;

        final float distance = getDistance();
        float minDist = distance * 1.1f; //far enough away
        float totalDist = 0;
        float dArc = arc / resolution;

        float angOffset = 0; //(float)Math.random() * (-arc/4f);

        final AbstractPolygonBot ap = this.abstractPolygonBot;

        for (int r = 0; r < resolution; r++) {
            float da = (-arc / 2f) + dArc * r + angOffset;
            final float V = da + angle + body.getAngle();

            ap.d.set(distance * (float) Math.cos(V), distance * (float) Math.sin(V));
            ap.point2.set(ap.point1);
            ap.point2.addLocal(ap.d);
            ccallback.init();

            try {
                ap.getWorld().raycast(ccallback, ap.point1, ap.point2);
            } catch (Exception e) {
                System.err.println("Phys2D raycast: " + e + " " + ap.point1 + " " + ap.point2);
                e.printStackTrace();
            }

            Vec2 endPoint = null;
            if (ccallback.m_hit) {
                float d = ccallback.m_point.sub(ap.point1).length() / distance;
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

                    endPoint = ccallback.m_point;
                }

                //pooledHead.set(ccallback.m_normal);
                //pooledHead.mulLocal(.5f).addLocal(ccallback.m_point);
                //draw.drawSegment(ccallback.m_point, pooledHead, normalColor, 0.25f);
                totalDist += d;
                if (d < minDist) {
                    hit = ccallback.body;
                    minDist = d;
                }
            } else {
                rayColor.set(normalColor);
                totalDist += 1;
                endPoint = ap.point2;
            }

            if ((drawing) && (endPoint != null)) {

                //final float alpha = rayColor.x *= 0.2f + 0.8f * (senseActivity + conceptPriority)/2f;
                //rayColor.z *= alpha - 0.35f * senseActivity;
                //rayColor.y *= alpha - 0.35f * conceptPriority;

                rayColor.x = conceptPriority;
                rayColor.y = conceptDurability;
                rayColor.z = conceptQuality;
                float alpha = Math.min(
                        (0.4f * conceptPriority * conceptDurability * conceptQuality) + 0.1f,
                        1f
                );
                rayColor.x = Math.min(rayColor.x * 0.9f + 0.1f, 1f);
                rayColor.y = Math.min(rayColor.y * 0.9f + 0.1f, 1f);
                rayColor.z = Math.min(rayColor.z * 0.9f + 0.1f, 1f);
                rayColor.x = Math.max(rayColor.x, 0f);
                rayColor.y = Math.max(rayColor.y, 0f);
                rayColor.z = Math.max(rayColor.z, 0f);
                final Vec2 finalEndPoint = endPoint.clone();
                Color3f rc = new Color3f(rayColor.x, rayColor.y, rayColor.z);
                final float thick = 2f;

                JoglAbstractDraw dd = ((JoglAbstractDraw) ap.getDraw());
                toDraw.add(() -> dd.drawSegment(ap.point1, finalEndPoint, rc.x, rc.y, rc.z, alpha, 1f * thick));

            }
        }
        if (hit != null) {
            float meanDist = totalDist / resolution;
            float percentDiff = (float) Math.sqrt(Math.abs(meanDist - minDist));
            float conf = 0.50f + 0.2f * (1.0f - percentDiff);
            if (conf > 0.9f) {
                conf = 0.9f;
            }

            //perceiveDist(hit, conf, meanDist);
            perceiveDist(hit, conf, meanDist);
        } else {
            perceiveDist(hit, 0.5f, 1.0f);
        }

        updatePerception();
    }

    protected float getDistance() {
        return distance;
    }

    protected void perceiveDist(Body hit, float newConf, float nextHitDist) {

        hitDist = nextHitDist;
        //hitDist = (distMomentum * hitDist) + (1f - distMomentum) * nextHitDist;
        //conf = (confMomentum * conf) + (1f - confMomentum) * newConf;
        conf = newConf;

        //System.out.println(angleTerm + " "+ hitDist + " " + conf);
        if (hit != null)
            this.hit = hit;

    }

    protected void updatePerception() {
        onTouch(hit, hitDist);


        if ((hit == null) || (hitDist > 1.0f)) {
            inputVisionFreq(hitDist, "confusion");
            return;
        } else if (conf < 0.01f) {
            inputVisionFreq(hitDist, "unknown");
            return;
        } else {
            String material = hit.getUserData() != null ? hit.getUserData().toString() : "sth";
            inputVisionFreq(hitDist, material);

        }


    }

    @Deprecated
    private String inputVisionDiscrete(float dist, String material) {
        float freq = 1f;
        String sdist = Sim.f(dist);
        //String ss = "<(*," + angleTerm + "," + dist + ") --> " + material + ">. :|: %" + Texts.n1(freq) + ";" + Texts.n1(conf) + "%";
        return "see:(" + material + ',' + angleTerm + ',' + sdist + "). :|: %" + freq + ';' + conf + '%';
    }

    private void inputVisionFreq(float dist, String material) {
        float freq = 0.5f + 0.5f * (1f / (1f + dist) );
        //String ss = "<(*," + angleTerm + "," + dist + ") --> " + material + ">. :|: %" + Texts.n1(freq) + ";" + Texts.n1(conf) + "%";
        //String x = "<see_" + angleTerm + " --> [" + material + "]>. %" + freq + "|" + conf + "%";

        Termed tt =
                $.inh(
                        $.p(angleTerm, Atom.the(material)),
                        $.the("see")
                );


        abstractPolygonBot.nar.input(
                new MutableTask(tt).belief().present(abstractPolygonBot.nar.memory).
                        truth(freq, conf));
    }

    public void onTouch(Body hit, float di) {
    }

    @Override
    public void drawGround(JoglAbstractDraw d, World w) {
        for (Runnable r : toDraw) {
            r.run();
        }
    }

    @Override
    public void drawSky(JoglAbstractDraw d, World w) {

    }

    public void setDistance(float d) {
        this.distance = d;
    }
}
