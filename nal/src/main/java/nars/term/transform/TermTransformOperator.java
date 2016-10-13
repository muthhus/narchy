package nars.term.transform;

import nars.$;
import nars.Op;
import nars.bag.Bag;
import nars.concept.AtomConcept;
import nars.term.atom.AtomicStringConstant;
import org.jetbrains.annotations.NotNull;


public abstract class TermTransformOperator extends AtomConcept implements TermTransform {


    public TermTransformOperator(String name) {
        super( $.the(name), Bag.EMPTY, Bag.EMPTY );
    }

}
