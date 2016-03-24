package nars;

import nars.concept.Concept;
import nars.nal.Level;
import nars.nal.Tense;
import nars.task.Task;
import nars.task.Tasked;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static nars.nal.Tense.ETERNAL;

/**
 * Defines the conditions used in an instance of a derivation
 */
public interface Premise extends Level, Tasked {

    @NotNull
    Concept concept();

    @NotNull
    @Override
    Task task();

    @Nullable
    Task belief();

    NAR nar();

//    default public void emit(final Class c, final Object... o) {
//        nar().emit(c, o);
//    }


    /**
     * curent maximum allowed NAL level the reasoner is configured to support
     */
    @Override
    default int nal() {
        return nar().nal();
    }




//    /**
//     * produces a cropped and filtered stack trace (list of methods called)
//     */
//    static List<String> getStack() {
//        StackTraceElement[] s = Thread.currentThread().getStackTrace();
//
//        String prefix = "";
//
//        boolean tracing = false;
//        //String prevMethodID;
//
//        List<String> path = new ArrayList();
//        for (StackTraceElement e : s) {
//            String className = e.getClassName();
//            String methodName = e.getMethodName();
//
//
//            if (tracing) {
//
//                //Filter conditions
//                if (className.contains("reactor."))
//                    continue;
//                if (className.contains("EventEmitter"))
//                    continue;
//                if (("NAL".equals(className) || "Memory".equals(className)) && "emit".equals(methodName))
//                    continue;
//
//                int cli = className.lastIndexOf('.') + 1;
//                if (cli != -1)
//                    className = className.substring(cli, className.length()); //class's simpleName
//
//                String methodID = className + '_' + methodName;
//
//                String sm = prefix + methodID + '_' + e.getLineNumber();
//
//
//                path.add(sm);
//
//                //prevMethodID = methodID;
//
//
//                //Termination conditions
//                if (className.contains("ConceptFireTask") && "accept".equals(methodName))
//                    break;
//                if (className.contains("ImmediateProcess") && "rule".equals(methodName))
//                    break;
//                if (className.contains("ConceptFire") && "rule".equals(methodName))
//                    break;
//            } else if (className.endsWith(".NAL") && "deriveTask".equals(methodName)) {
//                tracing = true; //begins with next stack element
//            }
//
//        }
//
//
//        return path;
//
//    }

//
//    default int duration() {
//        return memory().duration();
//    }

//    default public CyclesInterval newInterval(final long cycles) {
//        //return Interval.intervalSequence(Math.abs(timeDiff), Global.TEMPORAL_INTERVAL_PRECISION, nal.memory);
//        return CyclesInterval.make(cycles, duration());
//
//    }



//    @Deprecated
//    public static long inferOccurrenceTime(Stamp t, Stamp b) {
//
//
//        if ((t == null) && (b == null))
//            throw new RuntimeException("Both sentence parameters null");
//        if (t == null)
//            return b.getOccurrenceTime();
//        else if (b == null)
//            return t.getOccurrenceTime();
//
//
//        final long tOc = t.getOccurrenceTime();
//        final boolean tEternal = (tOc == Stamp.ETERNAL);
//        final long bOc = b.getOccurrenceTime();
//        final boolean bEternal = (bOc == Stamp.ETERNAL);
//
//        /* see: https://groups.google.com/forum/#!searchin/open-nars/eternal$20belief/open-nars/8KnAbKzjp4E/rBc-6V5pem8J) */
//
//        final long oc;
//        if (tEternal && bEternal) {
//            /* eternal belief, eternal task => eternal conclusion */
//            oc = Stamp.ETERNAL;
//        } else if (tEternal /*&& !bEternal*/) {
//            /*
//            The task is eternal, while the belief is tensed.
//            In this case, the conclusion will be eternal, by generalizing the belief
//            on a moment to the general situation.
//            According to the semantics of NARS, each truth-value provides a piece of
//            evidence for the general statement, so this inference can be taken as a
//            special case of abduction from the belief B<f,c> and G==>B<1,1> to G<f,c/(c+k)>
//            where G is the eternal form of B."
//            */
//            oc = Stamp.ETERNAL;
//        } else if (bEternal /*&& !tEternal*/) {
//            /*
//            The belief is eternal, while the task is tensed.
//            In this case, the conclusion will get the occurrenceTime of the task,
//            because an eternal belief applies to every moment
//
//            ---
//
//            If the task is not tensed but the belief is,
//            then an eternalization rule is used to take the belief as
//            providing evidence for the sentence in the task.
//            */
//            oc = tOc;
//        } else {
//            /*
//            Both premises are tensed.
//            In this case, the truth-value of the belief B<f,c> will be "projected" from
//            its previous OccurrenceTime t1 to the time of the task t2 to become B<f,d*c>,
//            using the discount factor d = 1 - |t1-t2| / (|t0-t1| + |t0-t2|), where t0 is
//            the current time.
//            This formula is cited in https://code.google.com/p/open-nars/wiki/OpenNarsOneDotSix.
//            Here the idea is that if a tensed belief is projected to a different time
//            */
//            /*
//            If both premises are tensed, then the belief is "projected" to the occurrenceTime of the task. Ideally, temporal inference is valid only when
//            the premises are about the same moment, i.e., have the same occurrenceTime or no occurrenceTime (i.e., eternal). However, since
//            occurrenceTime is an approximation and the system is adaptive, a conclusion about one moment (that of the belief) can be projected to
//            another (that of the task), at the cost of a confidence discount. Let t0 be the current time, and t1 and t2 are the occurrenceTime of the
//            premises, then the discount factor is d = 1 - |t1-t2| / (|t0-t1| + |t0-t2|), which is in [0,1]. This factor d is multiplied to the confidence of a
//            promise as a "temporal discount" to project it to the occurrence of the other promise, so as to derive a conclusion about that moment. In
//            this way, if there are conflicting conclusions, the temporally closer one will be preferred by the choice rule.
//             */
//            oc = tOc;
//        }
//
//
//        /*
//        //OLD occurence code:
//        if (currentTaskSentence != null && !currentTaskSentence.isEternal()) {
//            ocurrence = currentTaskSentence.getOccurenceTime();
//        }
//        if (currentBelief != null && !currentBelief.isEternal()) {
//            ocurrence = currentBelief.getOccurenceTime();
//        }
//        task.sentence.setOccurrenceTime(ocurrence);
//        */
//
//        return oc;
//    }

    /** true if both task and (non-null) belief are temporal events */
    default boolean isEvent() {
        /* TODO This part is used commonly, extract into its own precondition */
        Task b = belief();
        if (b == null) return false;
        return (!Tense.isEternal(task().occurrence()) &&
                (!Tense.isEternal(b.occurrence())));
    }

    /** true if task and belief (if not null) are eternal */
    default boolean isEternal() {
        if (task().isEternal()) {
            Task b = belief();
            return (b == null) || (b.isEternal());
        }
        return false;
    }

    boolean cyclic();


    @NotNull
    Termed beliefTerm();


    @FunctionalInterface
    interface OccurrenceSolver {
        long compute(long taskOcc, long beliefOcc);
    }

    default long occurrenceTarget(@NotNull OccurrenceSolver s) {
        long tOcc = task().occurrence();
        Task b = belief();
        if (b == null) return tOcc;
        else {
            long bOcc = b.occurrence();
            return s.compute(tOcc, bOcc);

//            //if (bOcc == ETERNAL) {
//            return (tOcc != ETERNAL) ?
//                        whenBothNonEternal.compute(tOcc, bOcc) :
//                        ((bOcc != ETERNAL) ?
//                            bOcc :
//                            ETERNAL
//            );
        }
    }


}
