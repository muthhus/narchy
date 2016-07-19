package nars.util.signal;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.util.math.FloatSupplier;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;

import static nars.$.t;

/** accepts a numeric signal which has been normalized to 0..1.0 range */
public class FuzzyConceptSet implements Iterable<SensorConcept> {


    private final FloatSupplier input;
    @NotNull
    public final List<SensorConcept> sensors;
    @NotNull
    public final NAR nar;
    float conf;


    @NotNull float[] centerPoints;

    public float calculate(int index) {
        float v = input.asFloat();

        int n = centerPoints.length;
        float nearness[] = new float[n];
        float s = 0;
        float dr = 1f / (n-1);
        for (int i = 0; i < n; i++) {
            float nn;
            float dist = Math.abs(centerPoints[i] - v);
            nearness[i] = nn = Math.max(0, dr-dist);
            s += nn;
        }
        return nearness[index] /= s;
    }

    public FuzzyConceptSet(MutableFloat input, @NotNull NAR nar, @NotNull String... states) {
        this(input::floatValue, nar, states);
    }

    public FuzzyConceptSet(FloatSupplier input, @NotNull NAR nar, @NotNull String... states) {


        this.conf = nar.confidenceDefault(Symbols.BELIEF);
        this.input = input;
        this.nar = nar;

        int numStates = states.length;
        centerPoints = new float[numStates];
        this.sensors = Global.newArrayList(numStates);

        if (states.length > 1) {
            float dr = 1f / (numStates-1);
            float center = 0;
            int i = 0;
            for (String s : states) {

                centerPoints[i] = center;
                int ii = i;

                sensors.add( new SensorConcept(s, nar, this.input,
                        (x) -> t(calculate(ii), conf)
                ));
                center += dr;
                i++;
            }
        } else {
            sensors.add( new SensorConcept(states[0], nar, this.input,
                    (x) -> t(x, conf)
            ));
        }



    }

//		private Truth biangular(float v) {
//			if (v < 0.5f) return t(0, conf);
//			else {
//				//return t(1f, conf * Math.min(1f,(v-0.5f)*2f));
//				return t(v, conf);
//			}
//		}
//
//		private Truth triangular(float v) {
//			float f, c;
//			if (v < 0.66f && v > 0.33f) {
//				f = 0.5f;
//				c = (0.33f-Math.abs(v-0.5f)) * 3f;
//			} else {
//				f = (v > 0.5f) ? 1 : 0;
//				if (v > 0.5f) {
//					c = Math.abs(v - 0.66f) * 3f;
//				} else {
//					c = Math.abs(v - 0.33f) * 3f;
//				}
//			}
//			c = Util.clamp(c);
//
//			return t(f, c * conf);
//		}

    @NotNull
    public FuzzyConceptSet pri(float p) {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            sensors.get(i).pri(p);
        }
        return this;
    }
    @NotNull
    public FuzzyConceptSet resolution(float r) {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            sensors.get(i).resolution(r);
        }
        return this;
    }

    @NotNull
    public FuzzyConceptSet conf(float c) {
        this.conf = c;
        return this;
    }


    @NotNull
    @Override
    public String toString() {
        return Joiner.on("\t").join(Iterators.transform(
                sensors.iterator(), s -> {
                    return s.term() + " " + s.beliefs().truth(nar.time()).toString();
                }
        ));
    }

    /** clear all sensor's belief state */
    public void clear() {
        sensors.forEach(s -> s.beliefs().clear());
    }


    @NotNull
    @Override
    public Iterator<SensorConcept> iterator() {
        return sensors.iterator();
    }
}
