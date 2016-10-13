package nars.term.transform;

import nars.$;
import nars.bag.Bag;
import nars.concept.AtomConcept;


public abstract class TermTransformOperator extends AtomConcept implements TermTransform {


    public TermTransformOperator(String name) {
        super( $.the(name), Bag.EMPTY, Bag.EMPTY );
    }

}
