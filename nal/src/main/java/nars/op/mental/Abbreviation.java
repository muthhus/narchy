package nars.op.mental;

import nars.$;
import nars.NAR;
import nars.Symbols;
import nars.budget.UnitBudget;
import nars.concept.Concept;
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
 * @param S serial term type
 */
public class Abbreviation/*<S extends Term>*/ implements Consumer<Task> {

    static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);

    private static final AtomicInteger currentTermSerial = new AtomicInteger(1);
    //when a concept is important and exceeds a syntactic complexity, let NARS name it:
    public final MutableInt abbreviationVolMin = new MutableInt(6);
    public final MutableInt abbreviationVolMax = new MutableInt(1000);
    public final MutableFloat abbreviationQualityMin = new MutableFloat(0.75f);

    //TODO different parameters for priorities and budgets of both the abbreviation process and the resulting abbreviation judgment
    //public AtomicDouble priorityFactor = new AtomicDouble(1.0);
    /**
     * generated abbreviation belief's confidence
     */
    public final MutableFloat abbreviationConfidence = new MutableFloat(0.95f);
    public final MutableFloat abbreviationProbability = new MutableFloat(Inperience.INTERNAL_EXPERIENCE_RARE_PROBABILITY);
    @NotNull
    protected final NAR nar;
    private final String termPrefix;
    private final nars.budget.Budgeted NewAbbreviationBudget = UnitBudget.Full.cloneMult(0.9f, 0.5f, 0.5f);


    public Abbreviation(@NotNull NAR n, String termPrefix) {

        this.nar = n;
        this.termPrefix = termPrefix;

        n.eventTaskProcess.on(this);

    }

    @Nullable
    static final Compound newAbbreviation(@NotNull Concept abbreviated, @NotNull Term id) {
        return (Compound) $.sim(abbreviated.term(), id);

        /*
        //old 1.6 pattern:
        Operation compound = Operation.make(
            Product.make(termArray(termAbbreviating)), abbreviate);*/
    }

    @NotNull
    protected Term newSerialTerm() {
        //TODO base64
        //return Atom.the(Utf8.toUtf8(name));

        return $.the(termPrefix + Integer.toString(currentTermSerial.incrementAndGet(), 36));

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
    }

    protected boolean canAbbreviate(@NotNull Task task) {
        Term t = task.term();

        /*if (t instanceof Similarity) {
            Similarity s = (Similarity)t;
            if (Operation.isA(s.getSubject(), abbreviate)) return false;
            if (Operation.isA(s.getPredicate(), abbreviate)) return false;
        }*/
        int v = t.volume();
        return
                //(!Op.isOperation(t)) &&
                (v >= abbreviationVolMin.floatValue()) &&
                        (v <= abbreviationVolMax.floatValue()) &&
                        (task.qua() > abbreviationQualityMin.intValue());
    }

    /**
     * To create a judgment with a given statement
     *
     * @return Immediate results as Tasks
     */
    @Override
    public void accept(@NotNull Task task) {

        //is it complex and also important? then give it a name:
        if (canAbbreviate(task)) {
            if (nar.random.nextFloat() <= abbreviationProbability.floatValue()) {

                Concept abbreviated = task.concept(nar);
                if (abbreviated != null && abbreviated.get(Abbreviation.class) == null) {

                    Term id = newSerialTerm();

                    abbreviate(abbreviated, id);
                }
            }
        }
    }

    protected  void abbreviate(Concept abbreviated, Term alias) {
        Concept abbreviation = nar.conceptualize(newAbbreviation(abbreviated, alias), NewAbbreviationBudget);
        if (abbreviation != null) {

            abbreviation.put(Abbreviation.class, abbreviation); //abbreviated by itself
            abbreviated.put(Abbreviation.class, alias); //abbreviated by the serial

            logger.info("Abbreviation " + abbreviation);

            nar.input(
                    new MutableTask(abbreviation, Symbols.BELIEF,
                            $.t(1, abbreviationConfidence.floatValue()))
            );

        }
    }


}
