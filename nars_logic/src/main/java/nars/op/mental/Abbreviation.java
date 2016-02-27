package nars.op.mental;

import nars.$;
import nars.Memory;
import nars.NAR;
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
 */
public class Abbreviation implements Consumer<Task> {

    public static final Logger logger = LoggerFactory.getLogger(Abbreviation.class);

    private static final AtomicInteger currentTermSerial = new AtomicInteger(1);
    //when a concept is important and exceeds a syntactic complexity, let NARS name it:
    public final MutableInt abbreviationVolMin = new MutableInt(7);
    public final MutableInt abbreviationVolMax = new MutableInt(15);
    public final MutableFloat abbreviationQualityMin = new MutableFloat(0.8f);

    //TODO different parameters for priorities and budgets of both the abbreviation process and the resulting abbreviation judgment
    //public AtomicDouble priorityFactor = new AtomicDouble(1.0);
    /**
     * generated abbreviation belief's confidence
     */
    public final MutableFloat abbreviationConfidence = new MutableFloat(0.95f);
    public final MutableFloat abbreviationProbability = new MutableFloat(Inperience.INTERNAL_EXPERIENCE_RARE_PROBABILITY);
    @NotNull
    private final NAR nar;
    private final String termPrefix;


    public Abbreviation(@NotNull NAR n, String termPrefix) {

        this.nar = n;
        this.termPrefix = termPrefix;

        n.eventTaskProcess.on(this);

    }

    @Nullable
    static final Compound newAbbreviation(@NotNull Concept abbreviated, Term id) {
        return (Compound) $.sim(abbreviated.term(), id);

        /*
        //old 1.6 pattern:
        Operation compound = Operation.make(
            Product.make(termArray(termAbbreviating)), abbreviate);*/
    }

    @NotNull
    public Term newSerialTerm() {
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

    final boolean canAbbreviate(@NotNull Task task) {
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

                    Concept abbreviation = nar.concept(newAbbreviation(abbreviated, id));
                    if (abbreviation != null) {

                        abbreviation.put(Abbreviation.class, abbreviation); //abbreviated by itself
                        abbreviated.put(Abbreviation.class, id); //abbreviated by the serial

                        logger.info("Abbreviation " + abbreviation);

                        nar.input(
                                new MutableTask(abbreviation)
                                        .judgment()
                                        .truth(1, abbreviationConfidence.floatValue())
                                        .present(nar)
                        );

                    }
                }
            }
        }
    }


}
