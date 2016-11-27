package nars.nar;

import nars.NAR;
import nars.Param;
import nars.budget.Budgeted;
import nars.concept.Concept;
import nars.nar.exe.SingleThreadExecutioner;
import nars.term.Termed;
import nars.time.FrameTime;
import nars.time.Time;
import nars.util.data.random.XORShiftRandom;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectFloatHashMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.function.Consumer;

/**
 * Terminal only executes commands and does not
 * reason.  however it will produce an event
 * stream which can be delegated to other
 * components like other NAR's
 */
public class Terminal extends NAR {

    //final Predicate<Task> taskFilter = Task::isCommand;

    public Terminal() {
        this(1024);
    }

    public Terminal(int capacity) {
        this(capacity, new XORShiftRandom(1), new FrameTime());
    }
    public Terminal(int capacity, @NotNull Random random, @NotNull Time c) {
        super(c, new Default.DefaultTermTermIndex(capacity), random, Param.defaultSelf(), new SingleThreadExecutioner());


    }


//    @Override
//    public Concept activate(@NotNull Termed termed, Activation overflow) {
//        return concept(termed, true); //ignore activation
//    }


    @Override
    public float priority(@NotNull Termed termed) {
        return 0;
    }

    //    @Override
//    protected Concept doConceptualize(Term term, Budget b, float scale) {
//        Concept exists = memory.concept(term);
//        if (exists!=null) {
//            return exists;
//        }
//        else {
//            Concept c = apply(term);
//            memory.index.put(term, c);
//            return c;
//        }
//
//    }


    @Nullable
    @Override
    public Concept concept(Termed term, float boost) {
        return null;
    }

    @Override
    public void activationAdd(ObjectFloatHashMap<Concept> concepts, Budgeted in, float activation, MutableFloat overflow) {
        //nothing
    }

    @Nullable
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        return null;
    }


}
