//package nars.rover.obj;
//
//import nars.$;
//import nars.NAR;
//import nars.bag.BLink;
//import nars.concept.Concept;
//import nars.nar.Default;
//import nars.rover.physics.gl.JoglDraw;
//import nars.term.Term;
//import nars.term.atom.Atom;
//import org.jbox2d.common.Color3f;
//import org.jbox2d.common.Vec2;
//import org.jbox2d.dynamics.Body;
//
//
///**
// * Created by me on 8/11/15.
// */
//@Deprecated public class NARVisionRay extends VisionRay {
//
//    private final NAR nar;
//    //new SometimesChangedTextInput(nar, minVisionInputProbability);
//
//    //private final String seenAngleTerm;
//
//    public final Term visionTerm;
//
//    public BLink<Concept> angleConcept;
//
//    float conceptPriority;
//    float conceptDurability;
//    float conceptQuality;
//
//
//    public NARVisionRay(String id, int num, NAR nar, Body base, Vec2 point, float angle, float arc, int resolution, float length) {
//        super(point, angle, arc, base, length, resolution);
//
//
//        this.nar = nar;
//        this.visionTerm =
//            //$.p(id, Integer.toString(num));
//            $.the(id + num);
//
//        //this.seenAngleTerm = //"see_" + sim.angleTerm(angle);
//    }
//
//
//    @Override public void drawRay(JoglDraw dd, RayDrawer r, Color3f c) {
//        float cp = this.conceptPriority;
//        dd.drawSegment(
//                r.from, r.to,
//                (cp * 0.75f + 0.25f) * c.x,
//                (cp * 0.75f + 0.25f) * c.y,
//                (cp * 0.75f + 0.25f) * c.z,
//                (cp * 0.75f + 0.25f) * 1f /* alpha */,
//                conceptDurability * 2f  + 1f /* width */,
//                conceptQuality * 0.5f + 1f /* z */);
//    }
//
//    @Override
//    public void step(boolean feel, boolean drawing) {
//
//        if (angleConcept == null) {
//            angleConcept = ((Default) nar).core.active.get(visionTerm);
//        }
//
//        if (angleConcept != null) {
//            conceptPriority = 0.5f + 0.5f * angleConcept.pri();
//            conceptDurability = 0.5f + 0.5f * angleConcept.dur();
//            conceptQuality = 0.5f + 0.5f * angleConcept.qua();
//
//            //System.out.println(angleConcept + " " + conceptPriority);
//
//            //sight.setProbability(Math.max(minVisionInputProbability, Math.min(1.0f, maxVisionInputProbability * conceptPriority)));
//            //sight.setProbability(minVisionInputProbability);
//        } else {
//            conceptPriority = 0.5f;
//            conceptDurability = 0.5f;
//            conceptQuality = 0.5f;
//        }
//
//        super.step(feel, drawing);
//
//
////        if ((hit == null) || (hitDist > 1.0f)) {
////            //inputVisionFreq(hitDist, "nothing");
////            return;
////        } else if (conf < 0.01f) {
////            inputVisionFreq(hitDist, "unknown");
////            return;
////        } else {
//        //float freq = 0.5f + 0.5f * (1f / (1f + dist) );
//        //String ss = "<(*," + angleTerm + "," + dist + ") --> " + material + ">. :|: %" + Texts.n1(freq) + ";" + Texts.n1(conf) + "%";
//        //String x = "<see_" + angleTerm + " --> [" + material + "]>. %" + freq + "|" + conf + "%";
//
//        //return Atom.the(Utf8.toUtf8(name));
//
//        //        int olen = name.length();
////        switch (olen) {
////            case 0:
////                throw new RuntimeException("empty atom name: " + name);
////
//////            //re-use short term names
//////            case 1:
//////            case 2:
//////                return theCached(name);
////
////            default:
////                if (olen > Short.MAX_VALUE/2)
////                    throw new RuntimeException("atom name too long");
//
//        //  }
//        //return Atom.the(Utf8.toUtf8(name));
//
//        //        int olen = name.length();
////        switch (olen) {
////            case 0:
////                throw new RuntimeException("empty atom name: " + name);
////
//////            //re-use short term names
//////            case 1:
//////            case 2:
//////                return theCached(name);
////
////            default:
////                if (olen > Short.MAX_VALUE/2)
////                    throw new RuntimeException("atom name too long");
//
//        //  }
//
//
//    }
//
//
////    @Deprecated
////    private String inputVisionDiscrete(float dist, String material) {
////        float freq = 1f;
////        String sdist = Sim.f(dist);
////        //String ss = "<(*," + angleTerm + "," + dist + ") --> " + material + ">. :|: %" + Texts.n1(freq) + ";" + Texts.n1(conf) + "%";
////        return "see:(" + material + ',' + angleTerm + ',' + sdist + "). :|: %" + freq + ';' + conf + '%';
////    }
//
//    //    public void setDistance(float d) {
////        this.distance = d;
////    }
//}
