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

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import static nars.util.Texts.n2;
import static nars.util.Texts.n4;

/**
 * Autoencodes a vector of inputs and attempts to classify the current values to
 * an item. these are input representing summary beliefs. the semantics of the
 * autoencoding can also be input at some interval, since this can change, the
 * assocaitions will need some continous remapping in proportion.
 * these can be done through tensed similarity beliefs.
 */
public class AutoClassifier extends Autoencoder  {

    private static final Logger logger = LoggerFactory.getLogger(AutoClassifier.class);




    //private final Compound aeBase;

    //private int metaInterval = 100;

    public AutoClassifier(int input, int output, Random rng) {
        super(input, output, rng);
    }

    //    protected void input(int stride, Term which, float conf) {
//
//        GeneratedTask t = new GeneratedTask(
//                input(stride, which),
//
//
//                '.', $.t(1f, conf));
//        t.time(nar.time(), nar.time()).budget(nar.priorityDefault(Symbols.BELIEF), nar.durabilityDefault(Symbols.BELIEF));
//        nar.inputLater( t );
//
//
//    }
//
//    @NotNull
//    private static Term state(int which) {
//        //even though the state can be identified by an integer,
//        //it does not have the same meaning as integers used
//        //elsewhere. however once the autoencoder stabilizes
//        //these can be relied on as semantically secure in their context
//        //return $.p(aeBase, new Termject.IntTerm(which));
//        return $.the("X" + which);
//    }
//
//    @NotNull
//    private Compound input(int stride, Term state) {
//        Compound c = $.prop(stride(stride), state);
//        if (c == null)  {
//            $.prop(stride(stride), state);
//            throw new NullPointerException();
//        }
//        return c;
//        //return $.image(2, false, base, new Termject.IntTerm(stride), state);
//    }
//
//    private Term stride(int stride) {
//        return $.p(base, new IntTerm(stride));
//    }
//
//    /** input the 'metadata' of the autoencoder that connects the virtual concepts to their semantic inputs */
//    protected void meta() {
//        int k = 0;
//        int n = input.size();
//        //final Term unknown = $.varDep(2);
//        for (int i = 0; i < strides; i++) {
//            List<? extends SensorConcept> l = input.subList(k, Math.min(n, k + stride));
//            //TODO re-use the same eternal belief to reactivate itself
//            Compound x = $.inh(
//                    $.sete(
//                        l.stream().map(CompoundConcept::term).toArray(Term[]::new)
//                    ),
//                    //input(i, unknown).term(0) //the image internal
//                    stride(i)
//            );
//            nar.believe(x);
//            k+= stride;
//        }
//    }
}
