package nars.task.util;

import jcog.tree.rtree.HyperRegion;
import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;

/**
 * dimensions:
 * 0: long time start..end
 * 1: float freq min..max
 * 2: float conf min..max
 */
public class TaskLinkRegion implements TaskRegion {

    /**
     * relative to time sameness (1)
     */
    static final float FREQ_SAMENESS_IMPORTANCE = 0.2f;
    /**
     * relative to time sameness (1)
     */
    static final float CONF_SAMENESS_IMPORTANCE = 0.05f;

    public final long start;
    public long end; //allow end to stretch for ongoing tasks

    public final float freqMin, freqMax, confMin, confMax;

    @Nullable
    public final Task task;

    @Override
    public final long start() {
        return start;
    }

    @Override
    public final long end() {
        return end;
    }

    @Override
    public boolean equals(Object obj) {
        return obj != null && (this == obj || (task != null && Objects.equals(task, ((TaskRegion) obj).task())));
    }

    @Override
    public int hashCode() {
        return task.hashCode();
    }

    @Override
    public String toString() {
        return task != null ? task.toString() : Arrays.toString(new double[]{start, end, freqMin, freqMax, confMin, confMax});
    }

    public TaskLinkRegion(long start, long end, float freqMin, float freqMax, float confMin, float confMax) {
        this.start = start;
        this.end = end;
        this.freqMin = freqMin;
        this.freqMax = freqMax;
        this.confMin = confMin;
        this.confMax = confMax;
        this.task = null;
    }

    public TaskLinkRegion(@NotNull Task task) {
        this.task = task;
        this.start = task.start();
        this.end = task.end();
        this.freqMin = this.freqMax = task.freq();
        this.confMin = this.confMax = task.conf();
    }

//        /**
//         * all inclusive time region
//         */
//        TaskRegion(long a, long b) {
//            this(a, b, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY);
//        }



    @Override
    public  double coord(boolean maxOrMin, int dimension) {
        switch (dimension) {
            case 0:
                return maxOrMin ? end : start;
            case 1:
                return maxOrMin ? freqMax : freqMin;
            case 2:
                return maxOrMin ? confMax : confMin;
        }
        throw new UnsupportedOperationException();
    }



    @Override
    public final @Nullable Task task() {
        return task;
    }

}
