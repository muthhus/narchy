package nars.table;


import jcog.list.FasterList;
import jcog.list.MultiRWFasterList;
import nars.NAR;
import nars.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.function.Consumer;

/**
 * implements a Task table suitable for Questions and Quests using an Array
 * uses a List and assumes that it is an ArrayList that can be
 * accessed by index.
 * <p>
 * TODO use a ring-buffer deque slightly faster than basic ArrayList modification
 */
public class ArrayQuestionTable extends MultiRWFasterList<Task> implements QuestionTable, Comparator<Task> {

    protected int capacity;
    static final Logger logger = LoggerFactory.getLogger(ArrayQuestionTable.class);



    public ArrayQuestionTable(int capacity) {
        super(new FasterList<Task>(capacity));

        this.capacity = capacity;
    }

    @Override
    public final int capacity() {
        return capacity;
    }

    @Override
    public final void capacity(int newCapacity, NAR n) {

        if (this.capacity != newCapacity) {

            this.capacity = newCapacity;

            withWriteLockAndDelegate(ll -> {

                int s = ll.size();
                int toRemove = s - capacity;
                while (toRemove > 0) {
                    remove(--s).delete(); //last element
                    toRemove--;
                }

            });
        }
    }

    @Override
    public boolean remove(Task x) {
        final boolean[] removed = new boolean[1];
        withWriteLockAndDelegate(l -> {
            removed[0] = l.remove(x);
        });
        return removed[0];
    }



    @Nullable
    @Override
    public final Task add(@NotNull Task question, @NotNull BeliefTable answers, @NotNull NAR n) {

        if (contains(question))
            return null; //already here

        float tp = question.pri();

        final Task[] trash = new Task[1];
        final Task[] result = new Task[1];

        withWriteLockAndDelegate(l -> {

            int sizeStart = l.size();
            if (sizeStart > 0) {

                l.sortThis(this);

                if (sizeStart >= capacity()) {
                    if (l.get(sizeStart - 1).qua() > tp) {
                        result[0] = null;
                        return;
                    } else {
                        Task weakest = l.remove(sizeStart - 1);
                        weakest.delete();
                        trash[0] = weakest;
//                        float removedPri = remove(list, sizeStart - 1, "Table Pop", n);
//                        if (removedPri == removedPri) //not deleted
//                            t.budget().setPriority(Math.max(t.pri(), removedPri)); //utilize at least its priority since theyre sorted by other factor
                    }
                }
            }

            //insert in sorted order by qua
            int i = 0;
            int sizeInsert = l.size();
            for (; i < sizeInsert - 1; i++) {
                if (l.get(i).qua() < tp)
                    break;
            }
            l.add(i, question);

            result[0] = question;
        });

        return result[0];
    }

    @Override public final void forEach(@NotNull Consumer<? super Task> action) {
        withWriteLockAndDelegate(l->{
            l.forEach(t -> {
                if (/*t != null && */!t.isDeleted())
                    action.accept(t);
            });
        });
    }

    @Override
    public int compare(@NotNull Task o1, @NotNull Task o2) {

        //return Float.compare(o2.qua(), o1.qua());
        return Float.compare(o2.priSafe(-1), o1.priSafe(-1));
    }

    //    @Override
//    public final boolean contains(Task t) {
//        //        //equality:
////        //  1. term (given because it is looking up in concept)
////        //  2. truth
////        //  3. occurrence time
////        //  4. evidential set
//
//        if (isEmpty()) return false;
//
//        Truth taskTruth = t.getTruth();
//        long taskOccurrrence = t.getOccurrenceTime();
//        long[] taskEvidence = t.getEvidence();
//
//        Task[] aa = getCachedNullTerminatedArray();
//        for (Task x : aa) {
//
//            if (x == null) return false;
//
//            if (
//
//                //different truth value
//                (x.getTruth().equals(taskTruth)) &&
//
//                //differnt occurence time
//                (x.getOccurrenceTime() == taskOccurrrence) &&
//
//                //differnt evidence
//                (Arrays.equals(x.getEvidence(), taskEvidence))
//            )
//                return true;
//        }
//
//        return false;
//
//    }


//    @Override
//    public void answer(@NotNull Task a, Concept answerConcept, @NotNull NAR nar, List<Task> displ) {
//
//        withWriteLockAndDelegate(l -> {
//            int size = l.size();
//
//            //each question is only responsible for 1/N of the effect on the answer
//            //TODO calculate this based on fraction of each question's priority of the total
//            //TODO weaken the match based on dt discrepencies between question and answer. this will discriminate according to unique dt patterns of questions vs answers
//
//            boolean affected = false;
//            for (int i = 0; i < size && !a.isDeleted(); i++) {
//                Task q = l.get(i);
//                if (q.isDeleted()) {
//                    l.remove(i);
//                    nar.tasks.remove(q);
//                    size--;
//                } else {
//                    if (answer(q, a, 1f / size, answerConcept, nar)) {
//                        affected = true;
//                    }
//                }
//            }
//
//            if (affected && size > 1)
//                l.sortThis(this);
//
//        });
//
//
//    }
//
//    /**
//     * returns false if the question should be removed after retuning
//     */
//    private static boolean answer(@NotNull Task q, @NotNull Task a, float scale, @Nullable Concept answerConcept, @NotNull NAR nar) {
////        if (Stamp.overlapping(q.evidence(), a.evidence()))
////            return;
//
//
//        //float qBudget = q.pri();
//
//        boolean aEtern = a.isEternal();
//        boolean qEtern = q.isEternal();
//        float factor = scale;
//        float aConf = a.conf();
//        if (aEtern) {
//            if (qEtern)
//                factor = scale * (1f - aConf);
//        } else {
//            if (!qEtern) {
//                //TODO BeliefTable.rankTemporalByConfidence()
//                factor = scale * (1f - aConf);
//            }
//        }
//
//
//        @NotNull Budget qBudget = q.budget();
//        @NotNull Budget aBudget = a.budget();
//        if (!qBudget.isDeleted() && !aBudget.isDeleted() ) {
//
//            q.onAnswered(a, nar, novel);
//
//            BudgetFunctions.transferPri(qBudget, aBudget, factor);
////            if (!qEtern) {
////                //if temporal question, also affect the quality so that it will get unranked by more relevant questions in the future
////                qBudget.quaMult(1 - factor);
////            }
//
//            boolean sameConcept;
//            sameConcept = !(answerConcept != null && answerConcept.crossLink(a, q, scale * aConf, nar)!=null);
//
//            if (Param.DEBUG_ANSWERS && !sameConcept) {
//                //if (q.term().equals(a.term()))
//                //if (!sameConcept) {
//                logger.debug("Q&A: {}\t{}", q, a);
//                //} else {
//                //  nar.logger.debug("Q&A: {}\t{}", q, a.truth());
//                //}
//            }
//
//            //amount boosted will be in proportion to the lack of quality, so that a high quality q will survive longer by not being drained so quickly
//            //BudgetFunctions.transferPri(q.budget(), a.budget(), (1f - q.qua()) * aConf);
//            return true;
//
//        } else {
//            //the qustion self-destructed
//            return false;
//        }
//
//
////        //generate a projected answer
//        //WARNING this creates a huge amount of useless answers
////        if (!qEtern && !aEtern && q.occurrence()!=a.occurrence()) {
////            Concept ac = nar.concept(a);
////            if (ac != null) { //??
////                Task ap = ac.merge(q, a, q.occurrence(), nar);
////                if (ap != null && !ap.isDeleted()) {
////                    if (Stamp.overlapping(q.evidence(), a.evidence()))
////                        nar.inputLater(ap); //avoid infinite loop
////                    else
////                        nar.input(ap);
////
////                    return;
////                }
////            }
////        }
//
//    }

}

