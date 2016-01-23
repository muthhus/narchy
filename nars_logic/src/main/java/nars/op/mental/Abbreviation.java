package nars.op.mental;

import nars.$;
import nars.NAR;
import nars.Op;
import nars.Symbols;
import nars.concept.Concept;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.compound.Compound;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 1-step abbreviation, which calls ^abbreviate directly and not through an added Task.
 * Experimental alternative to Abbreviation plugin.
 */
public class Abbreviation implements Consumer<Task> {

    private static final float abbreviationProbability = Inperience.INTERNAL_EXPERIENCE_PROBABILITY;


    final float abbreviationConfidence = 0.95f;

    //these two are AND-coupled:
    //when a concept is important and exceeds a syntactic complexity, let NARS name it:
    public final MutableInt abbreviationVolMin = new MutableInt(15);
    public final MutableInt abbreviationVolMax = new MutableInt(30);
    public final MutableFloat abbreviationQualityMin = new MutableFloat(0.75f);
    @NotNull
    private final NAR nar;

    //TODO different parameters for priorities and budgets of both the abbreviation process and the resulting abbreviation judgment
    //public AtomicDouble priorityFactor = new AtomicDouble(1.0);


    public Abbreviation(@NotNull NAR n) {

        n.memory.eventInput.on(this);
        nar = n;

    }

    private static final AtomicInteger currentTermSerial = new AtomicInteger(1);

    public static Term newSerialTerm() {
        //TODO base64
        return Atom.the(Symbols.TERM_PREFIX + Integer.toHexString(currentTermSerial.incrementAndGet()));
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
                (!Op.isOperation(t)) &&
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
            if ((nar.memory.random.nextFloat() <= abbreviationProbability)) {


                Compound termAbbreviating = task.term();

            /*Operation compound = Operation.make(
                    Product.make(termArray(termAbbreviating)), abbreviate);*/

                Concept concept = nar.concept(termAbbreviating);

                if (concept != null && concept.get(Abbreviation.class) == null) {

                    Term atomic = newSerialTerm();

                    concept.put(Abbreviation.class, atomic); //abbreviated by the serial

                    Compound c = (Compound) $.sim(termAbbreviating, atomic);
                    if (c != null) {

                        Concept cc = nar.concept(c);
                        if (cc!=null) {
                            cc.put(Abbreviation.class, cc); //abbreviated by itself

                            nar.logger.info("Abbreviation " + cc);

                            nar.input(
                                new MutableTask(c)
                                    .judgment().truth(1, abbreviationConfidence)
                                    .present(nar.memory)
                            );
                        }
                    }
                }
            }
        }
    }


}
