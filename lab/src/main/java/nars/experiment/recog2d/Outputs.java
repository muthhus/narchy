package nars.experiment.recog2d;

import nars.$;
import nars.NAR;
import nars.NAgent;
import nars.concept.CompoundConcept;
import nars.concept.GoalActionConcept;
import nars.term.Compound;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.FloatToFloatFunction;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

import static nars.Op.BELIEF;


/**
 * Created by me on 10/15/16.
 */
public class Outputs {

    public double errorSum() {
        return out.values().stream().mapToDouble(x -> x.error).map(x -> x == x ? x : 1f).sum();
    }

    static class Neuron {

        public float actual, actualConf;

        public float expected;

        public float error;

        public Neuron() {
            clear();
        }

        public void clear() {
            expected = Float.NaN;
            error = 0;
        }

        public void expect(float expected) {
            this.expected = expected;
            update();
        }

        public void actual(float f, float c) {
            this.actual = f;
            this.actualConf = c;
            update();
        }

        protected void update() {
            float a = this.actual;
            float e = this.expected;
            if (e != e) {
                this.error = Float.NaN;
            } else if (a != a) {
                this.error = 1f;
            } else {
                this.error = (Math.abs(a - e))
                //* ( this.actualConf )
                ;
            }
        }
    }

    public float[] expected(float[] output) {
        output = sized(output);
        for (int i = 0; i < outVector.length; i++)
            output[i] = expected(i);
        return output;
    }

    public float[] actual(float[] output) {
        output = sized(output);
        for (int i = 0; i < outVector.length; i++)
            output[i] = actual(i);
        return output;
    }

    float[] sized(float[] output) {
        if (output == null || output.length != states) {
            output = new float[states];
        }
        return output;
    }


    final LinkedHashMap<CompoundConcept, Neuron> out;
    CompoundConcept[] outVector;

    final int states;

    private final NAR nar;

    boolean verify = false;


    public Outputs(IntFunction<Compound> namer, int maxStates, NAgent a, FloatToFloatFunction transferFunction) {
        this.nar = a.nar;
        this.states = maxStates;
        this.out = new LinkedHashMap<>(maxStates);
        this.outVector = IntStream.range(0, maxStates).mapToObj((int i) -> {
                    Compound tt = namer.apply(i);
                    @NotNull GoalActionConcept aa = a.action(tt, (b, d) -> {
//                        if (train) {
//                            float ee = expected(i);
//
//                            float thresh = 0.1f;
//                            if (d==null || Math.abs(ee-d.freq())>thresh) {
//                                //correction
//                                a.nar.goal(tt, Tense.Present, ee, a.gamma);
//                                //return null;
//                            }
//
                        //return $.t(ee, a.alpha() );
//                            //return null;
//                        }

//                        if (b!=null && d!=null) {
//                            return d.confMult(0.5f + 0.5f * Math.abs(d.freq()-b.freq()));
//                        } else {
                        //return d!=null ? d.confWeightMult(0.5f) : null;
                        //}

                        return $.t(transferFunction.valueOf(d!=null ? d.freq() : 0), nar.confDefault(BELIEF));

                        //return d!=null ? new PreciseTruth(d.freq(), d.conf()goalInfluence.d.eviMult(goalInfluence, a.nar.dur()) : null;
                    });
                    aa.resolution.setValue(1f);
                    return aa;
                }
//                        a.sense(namer.apply(i), () -> {
//                            if (train) {
//                                return out.get(outVector[i]).expected;// ? 1f : 0.5f - (1f / states);
//                            } else {
//                                return Float.NaN; //no prediction
//                            }
//                        }, 0.01f, (v) -> $.t(v, a.alpha/2f))
//                            .pri(0.9f)
//                            //.timing(0, 1) //synchronous feed

        ).peek(c -> out.put(c, new Neuron()))
                .toArray(CompoundConcept[]::new);


        a.nar.onCycle(nn -> {
            long now = nar.time();
            int dur = nar.dur();
            out.forEach((cc, nnn) -> {

                Truth t =
                        //cc.belief(now, dur);
                        cc.goal(now, now, dur, nar);

                float f, c;
                if (t == null) {
                    f = Float.NaN;
                    c = Float.NaN;
                } else {
                    f = t.freq();
                    c = t.conf();
                }
                nnn.actual(f, c);
            });

        });
    }

    public float expected(int i) {
        return out.get(outVector[i]).expected;
    }


    public float actual(int state) {

        //return actual(outVector[state], when);
        return out.get(outVector[state]).actual;
    }
//    public float actual(Termed<Compound> state, long when) {
//        return nar.concept(state).beliefFreq(when);
//    }

    void expect(IntToFloatFunction stateValue) {
        //long now = nar.time();
        for (int i = 0; i < states; i++)
            out.get(outVector[i]).expect(stateValue.valueOf(i));
    }

    public void expect(int onlyStateToBeOn) {
        float offValue =
                0f;
        //0.5f - (1f/states)*0.5f;
        //1f/states * 0.5f;
        //0.5f;

        expect(ii -> ii == onlyStateToBeOn ? 1f : offValue);
    }

//    public void train() {
//        train = true;
//        verify = false;
//    }

//    public void verify() {
//        verify = true;
//        train = false;
//    }

    public float error(Compound c) {
        return out.get(c).error;
    }
}
