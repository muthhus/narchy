package nars.concept;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterators;
import jcog.math.FloatSupplier;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.term.Compound;
import nars.term.ProxyTerm;
import nars.term.Term;
import nars.truth.Truth;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * manages a set of concepts whose beliefs represent components of an
 * N-ary (N>=1) discretization of a varying scalar (32-bit floating point) signal.
 * expects values which have been normalized to 0..1.0 range (ex: use NormalizedFloat) */
public class FuzzyScalarConcepts extends ProxyTerm {

    private final FloatSupplier input;

    @NotNull
    @Deprecated public final List<SensorConcept> sensors;

    @NotNull
    public final NAR nar;

    float conf;


    public final static FuzzyModel Hard = (v, i, indices, n) -> {

        float vv = v * indices;

        int which = (int)Math.floor(vv);
        float f;
        if (i < which) {
            f = 1f;
        } else if ( i > which) {
            f = 0f;
        } else {
            f = vv - which;
        }

        return $.t( f, n.confDefault(Op.BELIEF) );

    };

    /** TODO need to analyze the interaction of the produced frequency values being reported by all concepts. */
    public final static FuzzyModel FuzzyTriangle = (v, i, indices, n) -> {

        float dr = 1f / (indices - 1);

        return $.t( Math.max(0, (1f - Math.abs((i * dr) - v) / dr)), n.confDefault(Op.BELIEF) ) ;
    };

    /** TODO not quite working yet. it is supposed to recursively subdivide like a binary number, and each concept represents the balance corresponding to each radix's progressively increasing sensitivity */
    public final static FuzzyModel FuzzyBinary = (v, i, indices, n) -> {

        //float nearness[] = new float[n];

        float b = v;
        float dv = 1f;
        for (int j =  0; j < i; j++) {
            dv /= 2f;
            b = Math.max(0, b - dv);
        }

        //System.out.println(v + " " + b + "/" + dv + " = " + (b/dv));

        Truth tt = $.t( b/(dv), n.confDefault(Op.BELIEF) ) ;
        return tt;
    };


    public FuzzyScalarConcepts(@NotNull MutableFloat input, @NotNull NAR nar, @NotNull Compound... states) {
        this(input, nar, FuzzyTriangle, states);
    }

    public FuzzyScalarConcepts(@NotNull MutableFloat input, @NotNull NAR nar, FuzzyModel truther,  @NotNull Compound... states) {
        this(input::floatValue, nar, truther, states);
    }

    public Term get(int i) {
        return sensors.get(i).term();
    }

    public FuzzyScalarConcepts resolution(float r) {
        for (SensorConcept s: sensors)
            s.resolution(r);
        return this;
    }

    @FunctionalInterface  public interface FuzzyModel {
        Truth truth(float valueNormalized, int conceptIndex, int maxConcepts, NAR nar);
    }

    public FuzzyScalarConcepts(FloatSupplier input, @NotNull NAR nar, FuzzyModel truther, @NotNull Compound... states) {
        super($.func("FuzzyScalarConcepts", states));

        this.conf = nar.confDefault(Op.BELIEF);
        this.input = input;
        this.nar = nar;

        int num = states.length;
        int numStates = num;
        this.sensors = $.newArrayList(numStates);

        if (num > 1) {
            int i = 0;
            for (Compound s : states) {
                final int ii = i++;
                sensors.add( new SensorConcept(s, nar, this.input,
                        (x) -> truther.truth(x, ii, num, nar)
                ));
            }
        } else {
           throw new RuntimeException("should be >1 states");
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

//    @NotNull
//    public FuzzyScalar pri(float p) {
//        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
//            sensors.get(i).pri(p);
//        }
//        return this;
//    }


//    @Override
//    public void forEach(Consumer<? super SensorConcept> action) {
//        sensors.forEach(action);
//    }

    @NotNull
    public FuzzyScalarConcepts resolution() {
        for (int i = 0, sensorsSize = sensors.size(); i < sensorsSize; i++) {
            sensors.get(i);
        }
        return this;
    }

    @NotNull
    public FuzzyScalarConcepts conf(float c) {
        this.conf = c;
        return this;
    }


    @NotNull
    public String summary() {
        return Joiner.on("\t").join(Iterators.transform(
                sensors.iterator(), s -> {
                    if (s == null)
                        return "?";
                    else {
                        long when = nar.time();
                        return s.term() + " " + s.beliefs().truth(when, nar);
                    }
                }
        ));
    }

//    /** clear all sensor's belief state */
//    public void clear() {
//        sensors.forEach(s -> s.beliefs().clear());
//    }

}
///**
// * SensorConcept which wraps a MutableFloat value
// */
//public class FloatConcept extends SensorConcept {
//
//
//    @NotNull
//    private final MutableFloat value;
//
//    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n) throws Narsese.NarseseException {
//        this(compoundTermString, n, Float.NaN);
//    }
//
//    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n, float initialValue) throws Narsese.NarseseException {
//        this(compoundTermString, n, new MutableFloat(initialValue));
//    }
//
//    public FloatConcept(@NotNull String compoundTermString, @NotNull NAR n, @NotNull MutableFloat v) throws Narsese.NarseseException {
//        super(compoundTermString, n, v::floatValue,
//                (vv) -> new DefaultTruth(vv, n.confidenceDefault(Symbols.BELIEF) )
//        );
//        this.value = v;
//    }
//
//    public float set(float v) {
//        value.setValue(v);
//        return v;
//    }
//
//}