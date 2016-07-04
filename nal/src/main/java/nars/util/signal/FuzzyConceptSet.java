package nars.util.signal;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.truth.Truth;
import nars.util.Util;
import nars.util.math.FloatSupplier;

import java.util.Iterator;
import java.util.List;

import static nars.$.t;

/** accepts a numeric signal which has been normalized to 0..1.0 range */
public class FuzzyConceptSet implements Iterable<SensorConcept> {


    private final FloatSupplier input;
    public final List<SensorConcept> sensors;
    private final NAR nar;
    float conf;

    public FuzzyConceptSet(FloatSupplier input, NAR nar, String... states) {

        this.conf = nar.confidenceDefault(Symbols.BELIEF);
        this.input = input;
        this.nar = nar;

        int numStates = states.length;
        this.sensors = Global.newArrayList(numStates);
        float dw = 1f / numStates;
        float dr = 1f / (numStates-1);
        float center = 0;
        int i = 0;
        for (String s : states) {

            float dd = (i==0 || i==numStates-1) ?
                    dr  : //endpoints
                    dw; //middle points

            float fCenter = center;
            sensors.add( new SensorConcept(s, nar,
                    this.input,
                    (x) -> {
                        float cdist = Math.abs(x - fCenter);
                        Truth y;
                        //if (cdist > dd) {
                            //y = t(0, conf);
                        //} else {
                        float f = (1f - (cdist / dd));

                        ///sharpen the curve:

                        y = t(f, conf);
                            //y = t(Util.clamp(1f-(cdist/dd)), conf);
                        //}
                        //System.out.println(x + " ==(" + fCenter + "|" + cdist + ")==> " + y);
                        return y;
                    }
            ));
            center += dr;
            i++;
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

    public FuzzyConceptSet pri(float p) {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            sensors.get(i).pri(p);
        }
        return this;
    }
    public FuzzyConceptSet resolution(float r) {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            sensors.get(i).resolution(r);
        }
        return this;
    }

    public FuzzyConceptSet conf(float c) {
        this.conf = c;
        return this;
    }


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


    @Override
    public Iterator<SensorConcept> iterator() {
        return sensors.iterator();
    }
}
