package nars.util.signal;

import nars.Global;
import nars.NAR;
import nars.Symbols;
import nars.util.math.FloatSupplier;

import java.util.List;

import static nars.$.t;

/** accepts a numeric signal which has been normalized to 0..1.0 range */
public class FuzzyConceptSet {


    private final FloatSupplier input;
    public final List<SensorConcept> sensors;
    float conf;

    public FuzzyConceptSet(FloatSupplier input, NAR nar, String... states) {

        this.conf = nar.confidenceDefault(Symbols.BELIEF);
        this.input = input;

        int numStates = states.length;
        this.sensors = Global.newArrayList(numStates);
        float dw = 1f / numStates;
        float dr = 1f / (numStates-1);
        float r = 0;
        int i = 0;
        for (String s : states) {
            float c = r; //center of the range

            sensors.add( new SensorConcept(s, nar, this.input,
                    (v) -> {
                        float cdist = Math.abs(v - c);
                        if (cdist > dw) return t(0,conf);
                        else {
                            return t(0.5f + (cdist/dw)*0.5f, conf);
                        }
                    }
            ));
            r += dr;
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
    public FuzzyConceptSet conf(float c) {
        this.conf = c;
        return this;
    }




}
