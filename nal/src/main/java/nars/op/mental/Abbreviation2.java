package nars.op.mental;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.budget.UnitBudget;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.ProxyCompoundConcept;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 1-step abbreviation, which calls ^abbreviate directly and not through an added Task.
 * Experimental alternative to Abbreviation plugin.
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
        logger.info("Aliased: {}", C.toStringActual());

    }
}
