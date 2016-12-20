package nars.task;

/**
 * interface for the temporal information about the
 * task to which this refers to.  used to separate
 * temporal tasks from non-temporal tasks
 */
public interface Temporal extends Tasked {


    long creation();
    long occurrence();




//    default boolean concurrent(Task s, int duration) {
//        return Tense.concurrent(s.getOccurrenceTime(), getOccurrenceTime(), duration);
//    }

//    default int tDelta(@NotNull Temporal other/*, int perceptualDuration*/) {
//        long start = start();
//        long other_end = other.end();
//        return (int)(start - other_end); //TODO long/int
//    }



//    default long getLifespan(Memory memory) {
//        long createdAt = getCreationTime();
//
//        return createdAt >= Tense.TIMELESS ? memory.time() - createdAt : -1;
//
//    }



//    default void setOccurrenceTime(@NotNull Tense tense, int duration) {
//        setOccurrenceTime(creation(), tense, duration);
//    }

//    default void setOccurrenceTime(long creation, @NotNull Tense tense, int duration) {
//        setOccurrenceTime(
//            Tense.getRelativeOccurrence(
//                    creation,
//                    tense,
//                    duration));
//    }
}
