package nars.data;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.nal.Tense;
import nars.task.GeneratedTask;
import nars.term.Compound;
import nars.term.Term;
import nars.term.obj.Termject;
import nars.util.signal.Autoencoder;
import nars.util.signal.SensorConcept;

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

    private final NAR nar;
    private final List<SensorConcept> input;
    private final float alpha;
    private final Term base;
    private final float epsilon;
    private final int strides;
    private final int stride;
    private final float conf;

    public AutoClassifier(Term base, NAR nar, List<SensorConcept> input, int stride, int output, float alpha) {
        super(stride, output, nar.random );
        this.nar = nar;
        this.input = input;
        this.alpha = alpha;
        this.base = base;
        this.epsilon = 0.01f;
        this.conf = nar.confidenceDefault(Symbols.BELIEF);
        this.stride = stride;
        this.strides = (int)Math.ceil(((float)input.size())/stride);

        assert(!input.isEmpty());


        nar.onFrame(this);
    }

    @Override
    public void accept(NAR nar) {
        float[] x = new float[stride];
        int k = 0;
        float errorSum = 0;
        int n = input.size();
        for (int s = 0; s < strides; s++) {
            for (int i = 0; i < stride; i++) {
                x[i] = k < n ? input.get(k++).asFloat() : 0f /* pad with zero if stride doesnt cleanly divide the input vector */;
            }

            float error = train(x, alpha, epsilon, 0, true);
            int y = max();

            input(s, y, error);
            errorSum += error;
        }
    }

    protected void input(int stride, int which, float error) {

        GeneratedTask t = new GeneratedTask(
                $.instprop($.p(base, new Termject.IntTerm(stride)), $.the("X" + which)),
                '.', $.t(1f, conf * (1f-error)));
        t.time(nar.time(), nar.time()).budget(0.5f, 0.5f);
        nar.inputLater( t );

        //System.out.println(t + "\t" + error);

    }
}
