//package nars;
//
//import jcog.Util;
//import jcog.math.FloatNormalized;
//import jcog.math.FloatSupplier;
//import nars.nar.Default;
//
//import static nars.$.func;
//import static nars.$.p;
//
///**
// * meta-controller of an NAgent
// *      essentially tunes runtime parameters in response to feedback signals
// *      can be instantiated in a NAR 'around' any agent
// */
//public class MetaAgent extends NAgent {
//
//    /** agent controlled by this */
//    private final NAgent agent;
//
//    /** colocated with the agent's NAR */
//    public MetaAgent(NAgent agent) {
//        this(agent, agent.nar);
//    }
//
//    public MetaAgent(NAgent agent, NAR metaNAR) {
//        super(func("meta", agent.id), metaNAR);
//        this.agent = agent;
//        NAR agentNAR = agent.nar;
//
//        senseNumber(p("happy"), new FloatNormalized(agent.happy));
//        //senseNumberNormalized(p("sad"), agentNAR.emotion::sad);
//        FloatSupplier v1 = ()->(float)agentNAR.emotion.busyPri.getSum();
//        senseNumber(p("busyPri"), new FloatNormalized(v1));
//        FloatSupplier v = ()->(float)agentNAR.emotion.busyVol.getSum();
//        senseNumber(p("busyVol"), new FloatNormalized(v));
//        senseNumber(p("lernPri") /*$.func($.the("lern"),$.the("pri"))*/, agentNAR.emotion::learningPri);
//        senseNumber(p("lernVol") /*$.func($.the("lern"),$.the("vol"))*/, agentNAR.emotion::learningVol);
//        senseNumber(p("dext"), agent::dexterity);
//
////        actionLerp(p("curiConf"), (c) -> {
////            agent.curiosityConf.setValue(Util.unitize(c));
////        }, -0.02f /* non-zero deadzone */, 0.25f);
//        actionLerp(p("curi"), (c) -> {
//            agent.curiosity().setValue(Util.unitize(c));
//        }, -0.02f /* non-zero deadzone */, 0.1f);
//
//        actionLerp(p("activationRate"), (c) -> {
//            ((Default)nar).focus.activationRate.setValue(Util.unitize(c));
//        }, 0f /* non-zero deadzone */, 1f);
//
//        //actionLerp(p("quaMin"), agentNAR.quaMin::setValue, 0f, 0.5f);
//
////        int dur = nar.dur();
////        actionLerp($.p("dur"), (d) -> agentNAR.time.dur(d),
////                 Math.max(1,dur *0.5f) /* 0 might cause problems with temporal truthpolation, examine */,
////                dur * 2f /* multiple of the originl duration of the input NAR */);
//    }
//
//    @Override
//    protected float act() {
//        //TODO other qualities to maximize: runtime speed, memory usage, etc..
//        float agentHappiness = agent.happy.asFloat();
//        //float narHappiness = agent.nar.emotion.happy();
//        //float narSadness = agent.nar.emotion.sad();
//
//        //return /*agentHappiness + */narHappiness - narSadness;
//        return agentHappiness;
//    }
//
//}
