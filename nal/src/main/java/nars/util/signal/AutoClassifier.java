package nars.util.signal;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.concept.CompoundConcept;
import nars.concept.SensorConcept;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.obj.IntTerm;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Consumer;

/**
 * Autoencodes a vector of inputs and attempts to classify the current values to
 * an item. these are input representing summary beliefs. the semantics of the
 * autoencoding can also be input at some interval, since this can change, the
 * assocaitions will need some continous remapping in proportion.
 * these can be done through tensed similarity beliefs.
 */
public class AutoClassifier extends Autoencoder implements Consumer<NAR> {

    private static final Logger logger = LoggerFactory.getLogger(AutoClassifier.class);
    public static final @NotNull Atom AE = $.the("ae");

    private final NAR nar;
    private final List<? extends SensorConcept> input;
    private final float alpha;
    private final Term base;
    private final float epsilon;
    private final int strides;
    private final int stride;
    private final float conf;
    //private final Compound aeBase;

    //private int metaInterval = 100;

    public AutoClassifier(Term base, NAR nar, List<? extends SensorConcept> input, int stride, int output, float alpha) {
        super(stride, output, nar.random );
        this.nar = nar;
        this.input = input;
        this.alpha = alpha;
        this.base = base;
        //this.aeBase = $.p(base, AE);
        this.epsilon = 0.01f;
        this.conf = nar.confidenceDefault(Symbols.BELIEF);
        this.stride = stride;
        this.strides = (int)Math.ceil(((float)input.size())/stride);

        assert(!input.isEmpty());


        meta();

        nar.onFrame(this);
    }

    @Override
    public void accept(NAR nar) {
        float[] x = new float[stride];
        int k = 0;
        float errorSum = 0;
        int n = input.size();
        float cMin = nar.confMin.floatValue();
        for (int s = 0; s < strides; s++) {
            for (int i = 0; i < stride; i++) {
                x[i] = k < n ? input.get(k++).asFloat() : 0f /* pad with zero if stride doesnt cleanly divide the input vector */;
            }

            float error = train(x, alpha, epsilon, 0, true);
            int y = max();

            float c = conf * (1f - error);
            if (c > cMin) {
                input(s, state(y), c);
            }

            errorSum += error;
        }



//        if (nar.time() % metaInterval == 0) {
//            logger.info("{} errorAvg={}", base, errorSum/strides);
//            meta();
//        }

    }

    protected void input(int stride, Term which, float conf) {

        GeneratedTask t = new GeneratedTask(
                input(stride, which),


                '.', $.t(1f, conf));
        t.time(nar.time(), nar.time()).budget(nar.priorityDefault(Symbols.BELIEF), nar.durabilityDefault(Symbols.BELIEF));
        nar.inputLater( t );


    }

    @NotNull
    private static Term state(int which) {
        //even though the state can be identified by an integer,
        //it does not have the same meaning as integers used
        //elsewhere. however once the autoencoder stabilizes
        //these can be relied on as semantically secure in their context
        //return $.p(aeBase, new Termject.IntTerm(which));
        return $.the("X" + which);
    }

    @NotNull
    private Compound input(int stride, Term state) {
        Compound c = $.prop(stride(stride), state);
        if (c == null)  {
            $.prop(stride(stride), state);
            throw new NullPointerException();
        }
        return c;
        //return $.image(2, false, base, new Termject.IntTerm(stride), state);
    }

    private Term stride(int stride) {
        return $.p(base, new IntTerm(stride));
    }

    /** input the 'metadata' of the autoencoder that connects the virtual concepts to their semantic inputs */
    protected void meta() {
        int k = 0;
        int n = input.size();
        //final Term unknown = $.varDep(2);
        for (int i = 0; i < strides; i++) {
            List<? extends SensorConcept> l = input.subList(k, Math.min(n, k + stride));
            //TODO re-use the same eternal belief to reactivate itself
            Compound x = $.inh(
                    $.sete(
                        l.stream().map(CompoundConcept::term).toArray(Term[]::new)
                    ),
                    //input(i, unknown).term(0) //the image internal
                    stride(i)
            );
            nar.believe(x);
            k+= stride;
        }
    }
}
