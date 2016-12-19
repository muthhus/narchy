package nars.table;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

/**
 * Created by me on 5/7/16.
 */
public interface TemporalBeliefTable extends TaskTable {

    /** finds the strongest match to the specified parameters. Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    @Nullable Task match(long when, long now, @Nullable Task against);

    @Nullable Truth truth(long when, long now, EternalTable eternal);

    /** return null if wasnt added */
    @Nullable TruthDelta add(@NotNull Task input, EternalTable eternal, Concept concept, @NotNull NAR nar);

    //boolean removeIf(@NotNull Predicate<? super Task> o, NAR nar);



    void capacity(int c, NAR nar);

    boolean isFull();

    void clear(NAR nar);

    boolean removeIf(Predicate<Task> o, @NotNull NAR nar);


    //void range(long[] t);
}
