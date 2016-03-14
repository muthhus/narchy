package nars.rover.obj;

import nars.$;
import nars.NAR;
import nars.bag.BLink;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.atom.Atom;
import org.jbox2d.common.Color3f;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;


/**
 * Created by me on 8/11/15.
 */
public class NARVisionRay extends VisionRay {

    private final NAR nar;
    //new SometimesChangedTextInput(nar, minVisionInputProbability);

    //private final String seenAngleTerm;

    public final Atom visionTerm;

    public BLink<Concept> angleConcept;
    float pri;

    float conceptPriority;
    float conceptDurability;
    float conceptQuality;
    public float seenDist;


    public NARVisionRay(String id, NAR nar, Body base, Vec2 point, float angle, float arc, int resolution, float length, float pri) {
        super(point, angle, arc, base, length, resolution);


        this.nar = nar;
        this.pri = pri;
        this.visionTerm = $.the(id);
        //this.seenAngleTerm = //"see_" + sim.angleTerm(angle);
    }

    @Override
    protected void updateColor(Color3f rayColor) {
        rayColor.x = conceptPriority;
        rayColor.y = conceptDurability;
        rayColor.z = conceptQuality;
//        float alpha = Math.min(
//                (0.4f * conceptPriority * conceptDurability * conceptQuality) + 0.1f,
//                1f
//        );
        rayColor.x = Math.min(rayColor.x * 0.9f + 0.1f, 1f);
        rayColor.y = Math.min(rayColor.y * 0.9f + 0.1f, 1f);
        rayColor.z = Math.min(rayColor.z * 0.9f + 0.1f, 1f);
        rayColor.x = Math.max(rayColor.x, 0f);
        rayColor.y = Math.max(rayColor.y, 0f);
        rayColor.z = Math.max(rayColor.z, 0f);

    }

    @Override
    protected void perceiveDist(Body hit, float newConf, float hitDist) {
        this.seenDist = hitDist;
        super.perceiveDist(hit, newConf, hitDist);
    }

    @Override
    public void step(boolean feel, boolean drawing) {

        if (angleConcept == null)
            angleConcept = ((Default)nar).core.active.get(nar.concept(visionTerm));

        if (angleConcept != null) {
            conceptPriority = 0.5f + 0.5f * angleConcept.pri();
            conceptDurability = 0.5f + 0.5f * angleConcept.dur();
            conceptQuality = 0.5f + 0.5f * angleConcept.qua();

            //sight.setProbability(Math.max(minVisionInputProbability, Math.min(1.0f, maxVisionInputProbability * conceptPriority)));
            //sight.setProbability(minVisionInputProbability);
        } else {
            conceptPriority = 0.5f;
            conceptDurability = 0.5f;
            conceptQuality = 0.5f;
        }

        super.step(feel, drawing);


//        if ((hit == null) || (hitDist > 1.0f)) {
//            //inputVisionFreq(hitDist, "nothing");
//            return;
//        } else if (conf < 0.01f) {
//            inputVisionFreq(hitDist, "unknown");
//            return;
//        } else {
        //float freq = 0.5f + 0.5f * (1f / (1f + dist) );
        //String ss = "<(*," + angleTerm + "," + dist + ") --> " + material + ">. :|: %" + Texts.n1(freq) + ";" + Texts.n1(conf) + "%";
        //String x = "<see_" + angleTerm + " --> [" + material + "]>. %" + freq + "|" + conf + "%";

        //return Atom.the(Utf8.toUtf8(name));

        //        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
        //return Atom.the(Utf8.toUtf8(name));

        //        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }


    }


//    @Deprecated
//    private String inputVisionDiscrete(float dist, String material) {
//        float freq = 1f;
//        String sdist = Sim.f(dist);
//        //String ss = "<(*," + angleTerm + "," + dist + ") --> " + material + ">. :|: %" + Texts.n1(freq) + ";" + Texts.n1(conf) + "%";
//        return "see:(" + material + ',' + angleTerm + ',' + sdist + "). :|: %" + freq + ';' + conf + '%';
//    }

    //    public void setDistance(float d) {
//        this.distance = d;
//    }
}
