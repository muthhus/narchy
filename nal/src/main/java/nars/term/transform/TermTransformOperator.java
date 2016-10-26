package nars.term.transform;

import nars.$;
import nars.bag.Bag;
import nars.concept.AtomConcept;
import org.jetbrains.annotations.NotNull;


public abstract class TermTransformOperator extends AtomConcept implements TermTransform {


    public TermTransformOperator(@NotNull String name) {
        super( $.the(name), Bag.EMPTY, Bag.EMPTY );
    }

}
