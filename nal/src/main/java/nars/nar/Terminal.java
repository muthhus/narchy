package nars.nar;

import jcog.data.random.XORShiftRandom;
import nars.NAR;
import nars.Param;
import nars.concept.Concept;
import nars.link.BLink;
import nars.nar.exe.SynchronousExecutor;
import nars.term.Termed;
import nars.time.FrameTime;
import nars.time.Time;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.eclipse.collections.api.tuple.primitive.ObjectFloatPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
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
        super(c, new Default.DefaultTermTermIndex(capacity), random, Param.defaultSelf(), new SynchronousExecutor());


    }


//    @Override
//    public Concept activate(@NotNull Termed termed, Activation overflow) {
//        return concept(termed, true); //ignore activation
//    }


    @Override
    public float priority(@NotNull Termed termed, float valueIfInactive) {
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
    public Concept concept(Termed term, float priToAdd) {
        return null;
    }

    @Override
    public Iterable<? extends BLink<Concept>> conceptsActive(int maxNodes) {
        return Collections.emptyList();
    }

    @Override
    public void activate(Iterable<ObjectFloatPair<Concept>> concepts, MutableFloat overflow) {
        //nothing
    }

    @Nullable
    @Override
    public NAR forEachActiveConcept(@NotNull Consumer<Concept> recip) {
        return null;
    }


}
