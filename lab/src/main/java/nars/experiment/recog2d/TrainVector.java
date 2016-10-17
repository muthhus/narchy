package nars.experiment.recog2d;

import nars.NAR;
import nars.NAgent;
import nars.concept.SensorConcept;
import nars.term.Compound;
import nars.term.Termed;
import nars.truth.Truth;
import org.eclipse.collections.api.block.function.primitive.IntToFloatFunction;

import java.util.LinkedHashMap;
import java.util.function.IntFunction;
import java.util.stream.IntStream;

/**
 * Created by me on 10/15/16.
 */
public class TrainVector {

    public double errorSum() {
        return out.values().stream().mapToDouble(x -> x.error).map(x -> x==x ? x : 0).sum();
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

        public void expect(float expected, long when) {
            this.expected = expected ;
            update();
        }

        public void actual(float f, float c, long when) {
            this.actual = f;
            this.actualConf = c;
            update();
        }

        protected void update() {
            float a = this.actual;
            float e = this.expected;
            if (e!=e) {
                this.error = Float.NaN;
            } else if (a != a) {
                this.error = 1f;
            } else {
                this.error = Math.abs(a - e) * this.actualConf;
            }
        }
    }

    final LinkedHashMap<SensorConcept,Neuron> out;
    private SensorConcept[] outVector;

    final int states;

    private final NAR nar;
    boolean reset = true;
    boolean train = true;
    boolean verify = false;


    public TrainVector(IntFunction<Compound> namer, int maxStates, NAgent a) {
        this.nar = a.nar;
        this.states = maxStates;
        this.out = new LinkedHashMap<>(maxStates);
        this.outVector = IntStream.range(0, maxStates).mapToObj(i ->
                        a.sense(namer.apply(i), () -> {
                            if (train) {
                                return out.get(outVector[i]).expected;// ? 1f : 0.5f - (1f / states);
                            } else {
                                return Float.NaN; //no prediction
                            }
                        })
                            ///.timing(0, 1) //synchronous feed

        ).peek(c -> out.put(c, new Neuron())).toArray(SensorConcept[]::new);



        a.nar.onFrame(nn -> {
            long now = nar.time();
            out.forEach((cc, nnn) -> {
                Truth t = cc.belief(now);
                float f, c;
                if (t == null) {
                    f = Float.NaN;
                    c = Float.NaN;
                } else {
                    f = t.freq();
                    c = t.conf();
                }
                nnn.actual(f, c, now);
            });

        });
    }


    public float actual(int state, long when) {
        return actual(outVector[state], when);
    }
    public float actual(Termed<Compound> state, long when) {
        return nar.concept(state).beliefFreq(when);
    }

    public void expect(IntToFloatFunction stateValue) {
        long now = nar.time();
        for (int i = 0; i < states; i++)
            out.get(outVector[i]).expect( stateValue.valueOf(i), now );
    }

    public void expect(int onlyStateToBeOn) {
        float offValue =
                //0.5f - (1f/states)*0.5f;
                0f;

        expect(ii -> ii == onlyStateToBeOn ? 1f : offValue);
    }

    public void train() {
        train = true;
        verify = false;
    }

    public void verify() {
        verify = true;
        train = false;

    }

    public float error(Compound c) {
        return out.get(c).error;
    }
}
