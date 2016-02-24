package nars.concept.util;

import com.google.common.collect.Iterators;
import nars.Memory;
import nars.task.Task;
import org.apache.commons.math3.analysis.interpolation.BivariateGridInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.function.Consumer;

/**
 * holds a set of ranked question/quests tasks
 * top ranking items are stored in the lower indexes so they will be first iterated
 */
public interface TaskTable extends Iterable<Task> {

    int getCapacity();

    void setCapacity(int newCapacity);

    /**
     * number of items in this collection
     */
    int size();

    void clear();

    boolean isEmpty();





    @Nullable
    default BivariateGridInterpolator getWaveFrequencyConfidenceTime() {
        return null;
    }

    @Nullable
    default UnivariateInterpolator getWaveFrequencyConfidence() {
        return null;
    }

    @Nullable
    default UnivariateInterpolator getWaveConfidenceTime() {
        return null;
    }

    default void top(int maxPerConcept, @NotNull Consumer<Task> recip) {
        int s = size();
        if (s < maxPerConcept) maxPerConcept = s;
        for (Task t : this) {
            recip.accept(t);
            if (--maxPerConcept == 0) break;
        }
    }


    //boolean contains(Task t);

    public static QuestionTaskTable EMPTY = new QuestionTaskTable() {

        @Override
        public
        @Nullable
        Task add(Task t, Memory m) {
            return t;
        }

        @Override
        public
        @Nullable
        Task contains(Task t) {
            return null;
        }

        @Override
        public Iterator<Task> iterator() {
            return Iterators.emptyIterator();
        }

        @Override
        public int getCapacity() {
            return 0;
        }

        @Override
        public void setCapacity(int newCapacity) {

        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public void clear() {

        }

        @Override
        public boolean isEmpty() {
            return true;
        }
    };
}
