package nars.derive.meta;

/**
 * Created by me on 7/11/16.
 */
@FunctionalInterface
public interface OccurrenceSolver {
    long compute(long taskOcc, long beliefOcc);
}
