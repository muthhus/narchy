package nars.control;

import jcog.Util;
import jcog.data.FloatParam;
import jcog.math.FloatNormalized;
import jcog.pri.Priority;
import jcog.pri.mix.control.MixAgent;
import jcog.pri.mix.control.MixContRL;
import jcog.tensor.ArrayTensor;
import jcog.tensor.Tensor;
import nars.$;
import nars.NAR;
import nars.NAgent;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;

import java.util.Arrays;

import static nars.$.p;

/**
 * embeds an RL controller NAR "homonculus", ideally single-thread
 * meta-controller of an NAgent
 * tunes runtime parameters in response to feedback signals
 * can be instantiated in a NAR 'around' any agent
 */
public class NARMixAgent<X extends Priority> extends NAgent implements MixAgent {

    float speed = 0.5f;

    public final FloatParam reward = new FloatParam(0, -1f, 1f);
    private final float[] outs;
    private final FloatArrayList ins = new FloatArrayList();
    private final NAR controlled;

    private final int cyclesPerFrame = 4;

//    public NARMixAgent(NAR controller, NAR controlled) {
//        this(controller, ()->controlled.emotion.happy());
//    }

    @Override
    public void act(Tensor in, float score, ArrayTensor out) {

        if (in.volume() != ins.size()) {
            throw new RuntimeException();
        }

        in.forEach(ins::set);

        nar.time.dur(1);

        for (int i = 0; i < cyclesPerFrame; i++)
            nar.exe.cycle(nar);
        next();
        nar.time.cycle();

        for (int i = 0, outsLength = outs.length; i < outsLength; i++) {
            out.set(outs[i], i);
        }
    }

    public NARMixAgent(NAR controller, MixContRL<X> mix, NAR controlled) {
        super("", controller);

        controller.truthResolution.setValue(0.05f);
        controller.termVolumeMax.setValue(18);
        controller.DEFAULT_QUEST_PRIORITY = controller.DEFAULT_QUESTION_PRIORITY = 0.25f;

        this.controlled = controlled;
//        senseNumber(p("happy"),
//                new FloatPolarNormalized(controlled.emotion::happy));
//        senseNumber(p("busyPri"),
//                new FloatNormalized(controlled.emotion.busyPri::getSum));
        senseNumber(p("busyVol"),
                new FloatNormalized(controlled.emotion.busyVol::getSum));

        //controller.log();

        int iv = mix.agentIn.volume();
        for (int s = 0; s < iv; s++) {
            ins.add(0);
            int ss = s;
            senseNumber($.inh($.p( Integer.toString(ss) ), $.the("I") ), () -> ins.get(ss));
        }

        int d = mix.dim;
        outs = new float[d];
        Arrays.fill(outs, 0.5f);
        for (int a = 0; a < d; a++) {
            int aa = a;
            actionBipolar($.inh( $.p(mix.mix[a].id), $.the("O")), (v) -> {
                if (v == v) {
                    //outs[aa] = v;
                    outs[aa] = Util.unitize(outs[aa] + v * speed);
                }
                return v;
            });
        }
//        senseNumber(p("lernPri") /*$.func($.the("lern"),$.the("pri"))*/, agentNAR.emotion::learningPri);
//        senseNumber(p("lernVol") /*$.func($.the("lern"),$.the("vol"))*/, agentNAR.emotion::learningVol);
//        senseNumber(p("dext"), agent::dexterity);

//        actionLerp(p("curiConf"), (c) -> {
//            agent.curiosityConf.setValue(Util.unitize(c));
//        }, -0.02f /* non-zero deadzone */, 0.25f);

//        actionLerp(p("curi"), (c) -> {
//            agent.curiosity().setValue(Util.unitize(c));
//        }, -0.02f /* non-zero deadzone */, 0.1f);
//
//        actionLerp(p("activationRate"), (c) -> {
//            ((Default)nar).focus.activationRate.setValue(Util.unitize(c));
//        }, 0f /* non-zero deadzone */, 1f);

        //actionLerp(p("quaMin"), agentNAR.quaMin::setValue, 0f, 0.5f);

//        int dur = nar.dur();
//        actionLerp($.p("dur"), (d) -> agentNAR.time.dur(d),
//                 Math.max(1,dur *0.5f) /* 0 might cause problems with temporal truthpolation, examine */,
//                dur * 2f /* multiple of the originl duration of the input NAR */);
    }


    @Override
    protected float act() {
        //TODO other qualities to maximize: runtime speed, memory usage, etc..
        return controlled.emotion.happy();
        //float narHappiness = agent.nar.emotion.happy();
        //float narSadness = agent.nar.emotion.sad();

        //return /*agentHappiness + */narHappiness - narSadness;
    }

}
