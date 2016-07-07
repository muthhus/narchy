package nars.util.signal;

import com.google.common.collect.TreeMultimap;
import com.google.common.collect.TreeRangeMap;
import nars.NAR;
import nars.bag.impl.SortedListTable;
import nars.concept.table.DefaultBeliefTable;
import nars.concept.table.EternalTable;
import nars.concept.table.TemporalBeliefTable;
import nars.task.Task;
import nars.truth.Truth;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * customized belief table for sensor and motor concepts
 */
public class SensorBeliefTable extends DefaultBeliefTable {

    public SensorBeliefTable(int eCap, int tCap) {
        super(eCap, tCap);
    }

    @Override
    protected @NotNull Task addTemporal(@NotNull Task goal, @NotNull NAR nar) {

        if (!goal.isInput() && nar.taskPast.test(goal)) {
            //this is a goal for a past time, reject
            return null;
        }

        if (!isEmpty()) {
            TemporalBeliefTable temporals = temporal;
            if (temporals.isFull()) {
                //remove any past goals
                temporals.removeIf(nar.taskPast);
            }
        }

        return super.addTemporal(goal, nar);
    }

//    @Override
//    protected TemporalBeliefTable newTemporalBeliefTable(Map<Task, Task> mp, int initialTemporalCapacity) {
//        return new SensorTemporalBeliefTable(mp, initialTemporalCapacity);
//    }

    public static class SensorTemporalBeliefTable implements TemporalBeliefTable {

        //TODO


        private int capacity;
        private final TreeMultimap<Long, Task> map;

        public SensorTemporalBeliefTable(@Deprecated Map<Task, Task> outerMap, int capacity) {
            map = TreeMultimap.create(Long::compare, (Task ta, Task tb) -> {
                return Float.compare(ta.conf(), tb.conf());
            });
            this.capacity = capacity;

        }

        @Override
        public void clear() {
            map.clear();
        }

        @Nullable
        @Override
        public Task get(Object key) {
            throw new UnsupportedOperationException();
        }

        @Nullable
        @Override
        public Object remove(Task key) {
            return map.remove(key.occurrence(), key);
        }

        @Nullable
        @Override
        public Task put(Task task, Task task2) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public void forEachKey(Consumer<? super Task> each) {
            map.values().forEach(each);
        }

        @Override
        public int capacity() {
            return capacity;
        }

        @Override
        public void setCapacity(int i) {
            this.capacity = i;
        }

        @Override
        public void topWhile(Predicate<? super Task> each, int n) {
            //TODO choose a radius of n around the current nar.time()


            for (Long aLong : map.keySet()) {
                for (Task task : map.get(aLong)) {
                    if (!each.test(task))
                        break;
                    if (n-- == 0)
                        break;
                }

            }
        }

        public int compare(Task o1, Task o2) {
            return Long.compare(o1.occurrence(), o2.occurrence());
        }

        @Override
        public @Nullable Task strongest(long when, long now, Task against) {
            //map.keySet().higher(when);
            return null;
        }

        @Override
        public @Nullable Truth truth(long when, long now, EternalTable eternal) {
             return strongest(when, now, null).projectTruth(when, now, false);
        }

        @Override
        public @Nullable Task add(Task input, EternalTable eternal, NAR nar) {
            return null;
        }

        @Override
        public void removeIf(Predicate<Task> o) {

        }

        @Override
        public Iterator<Task> iterator() {
            return null;
        }
    }
}
