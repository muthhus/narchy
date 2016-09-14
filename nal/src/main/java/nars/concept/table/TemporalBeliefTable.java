package nars.concept.table;

import nars.NAR;
import nars.Task;
import nars.concept.Concept;
import nars.concept.TruthDelta;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

/**
 * Created by me on 5/7/16.
 */
public interface TemporalBeliefTable extends TaskTable {

    /** finds the strongest match to the specified parameters. Task against is an optional argument which can be used to compare internal temporal dt structure for similarity */
    @Nullable Task match(long when, long now, @Nullable Task against);

    @Nullable Truth truth(long when, long now, EternalTable eternal);

    /** return null if wasnt added */
    @Nullable TruthDelta add(@NotNull Task input, EternalTable eternal, List<Task> displ, Concept concept, @NotNull NAR nar);

    boolean removeIf(@NotNull Predicate<? super Task> o, List<Task> displ);



    void capacity(int c, long now, List<Task> removed);

    boolean isFull();

    //void range(long[] t);
}
