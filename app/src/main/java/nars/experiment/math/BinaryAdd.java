package nars.experiment.math;

import nars.$;
import nars.NAR;
import nars.experiment.NAREnvironment;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.obj.Termject;
import nars.truth.Truth;
import nars.util.signal.MotorConcept;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

import static nars.util.Util.argmax;

/**
 * Created by me on 8/21/16.
 */
public class BinaryAdd extends NAREnvironment {

    private CharSensor a, b;
    private CharMotor c;

    public BinaryAdd(NAR nar) {
        super(nar);
    }

    public static class CharSensor {

        private final Term id;
        private final NAR nar;

        char[] data = {};

        public CharSensor(String id, NAR nar) {
            this($.the(id), nar);
        }
        public CharSensor(Term id, NAR nar) {
            this.id = id;
            this.nar = nar;
//            nar.onFrame(f -> {
//
//            });
        }

        public CharSensor input() {
            Term t = $.inh(
                $.p(data, c -> $.the(c)),
                id
            );
            nar.believe(t, Tense.Present, 1f);
            return this;
        }

        @NotNull public char[] get() {
            return data;
        }

        public CharSensor set(@NotNull char[] newData) {
            if (!Arrays.equals(data, newData)) {
                this.data = newData;
                input();
            }
            return this;
        }

    }

    public static class CharMotor {

        final char[] buffer;

        final MotorConcept[][] motor;
        final float[][] desire;

        private final char[] vocab;

        public CharMotor(Term id, NAREnvironment env, int length, char[] vocab /* characters it can choose from */) {

            this.buffer = new char[length];
            this.vocab = vocab;

            this.motor = new MotorConcept[length][vocab.length];
            this.desire = new float[length][vocab.length];

            for (int i = 0; i < vocab.length; i++) {
                for (int j = 0; j < length; j++) {
                    Compound t = $.p(id, new Termject.IntTerm(j), $.the(vocab[i]));
                    int jj = j;
                    int ii = i;
                    env.actions.add(new MotorConcept(t, env.nar, (Truth b, Truth d)->{
                        desire[jj][ii] = d!=null ? d.freq() : 0.5f;
                        return d;
                    }));
                }
            }
        }

        public char[] get() {
            update();
            return buffer;
        }

        protected void update() {
            for (int j = 0; j < buffer.length; j++) {
                buffer[j] = decide(j);
            }

        }

        private char decide(int j) {
            return vocab[argmax(desire[j])];
        }

        public String string() {
            return new String(get());
        }

    }

    @Override
    protected void init(NAR n) {
        a = new CharSensor("a", n);
        b = new CharSensor("b", n);
        c = new CharMotor($.the("c"), this, 4, new char[] { '0', '1' });
    }

    @Override
    protected float act() {
        System.out.println(c.string());
        return 0.5f;
    }

    public static void main(String[] args) {
        Default nar = new Default();

        new BinaryAdd(nar).run(1000);
    }


}
