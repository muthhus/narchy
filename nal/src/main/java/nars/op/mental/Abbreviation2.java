package nars.op.mental;

import nars.$;
import nars.NAR;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.ProxyCompoundConcept;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 the proxy concepts present a bidirectional facade between a referenced and an alias term (alias term can be just a serial # atom wrapped in a product).

 it replaces the index entry for the referenced with itself and also adds itself so from its start it intercepts all references to itself or the aliased (abbreviated) term whether this occurrs at the top level or as a subterm in another term (or as a subterm in another abbreviation, etc..)

 the index is usually a weakmap or equivalent in which abbreviations can be forgotten as well as any other concept.

 seen from a superterm containing one, it appears as a simple volume=2 concept meanwhile it could be aliasing a concept much larger than it. common "phrase" concepts with a volume >> 2 are good candidates for abbreviation. but when printed, the default toString() method is proxied so it will automatically decompress on output (or other serialization).
 */
public class Abbreviation2 extends Abbreviation {

    static final Logger logger = LoggerFactory.getLogger(Abbreviation2.class);

    public Abbreviation2(@NotNull NAR n, String termPrefix) {
        super(n, termPrefix);
    }

    @NotNull
    protected Compound newSerialTerm() {
        return $.p(super.newSerialTerm());
    }

    @Override
    protected void abbreviate(Concept abbreviated, Term alias) {
        ProxyCompoundConcept C = new ProxyCompoundConcept((Compound)alias, (CompoundConcept)abbreviated, nar);
        logger.info(C.toStringActual());
    }

    @Override
    protected boolean canAbbreviate(@NotNull Task task) {
        return (super.canAbbreviate(task) &&
                !(task.concept(nar) instanceof ProxyCompoundConcept)); //not already an abbreviation itself
    }
}
