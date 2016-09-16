package nars.concept.table;


import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import nars.$;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.budget.Activation;
import nars.budget.Budget;
import nars.budget.BudgetFunctions;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.task.AnswerTask;
import nars.util.data.list.FasterList;
import org.apache.commons.collections4.ListUtils;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.list.mutable.MultiReaderFastList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * implements a Task table suitable for Questions and Quests using an ArrayList.
 * uses a List and assumes that it is an ArrayList that can be
 * accessed by index.
 * <p>
 * TODO use a ring-buffer deque slightly faster than basic ArrayList modification
 */
public class ArrayQuestionTable  implements QuestionTable, Comparator<Task> {

    protected int capacity;

    @NotNull
    private final MultiReaderFastList<Task> list;

    public ArrayQuestionTable(int capacity) {
        super();

        this.list = //$.newArrayList(capacity);
                MultiReaderFastList.newList(capacity);

        this.capacity = capacity;
        //setCapacity(capacity);
    }

    @Override
    public final int capacity() {
        return capacity;
    }

    @Override
    public final void capacity(int newCapacity, @NotNull List<Task> displ) {

        if (this.capacity != newCapacity) {

            this.capacity = newCapacity;

            list.withWriteLockAndDelegate(ll -> {
                if (size() > newCapacity) {
                    int s = ll.size();
                    int toRemove = s - capacity;
                    while (toRemove > 0) {
                        displ.add(list.remove(--s)); //last element
                        toRemove--;
                    }
                }
            });
        }
    }


    @Override
    public final int size() {
        return list.size();
    }


    @Override
    public final boolean isEmpty() {
        return list.isEmpty();
    }


    @Override
    public void answer(@NotNull Task a, Concept answerConcept, @NotNull NAR nar, List<Task> displ) {

        list.withWriteLockAndDelegate(l -> {
            int size = l.size();

            //each question is only responsible for 1/N of the effect on the answer
            //TODO calculate this based on fraction of each question's priority of the total
            //TODO weaken the match based on dt discrepencies between question and answer. this will discriminate according to unique dt patterns of questions vs answers

            for (int i = 0; i < size && !a.isDeleted(); ) {
                Task q = l.get(i);
                if (!q.isDeleted()) {
                    if (answer(q, a, 1f / size, answerConcept, nar)) {
                        i++;
                        continue;
                    }
                }

                remove(q, null, displ);
                size--;
            }
        });


    }

    /**
     * returns false if the question should be removed after retuning
     */
    private static boolean answer(@NotNull Task q, @NotNull Task a, float scale, @Nullable Concept answerConcept, @NotNull NAR nar) {
//        if (Stamp.overlapping(q.evidence(), a.evidence()))
//            return;


        //float qBudget = q.pri();

        boolean aEtern = a.isEternal();
        boolean qEtern = q.isEternal();
        float factor = scale;
        float aConf = a.conf();
        if (aEtern) {
            if (qEtern)
                factor = scale * (1f - aConf);
        } else {
            if (!qEtern) {
                //TODO BeliefTable.rankTemporalByConfidence()
                factor = scale * (1f - aConf);
            }
        }


        @NotNull Budget qBudget = q.budget();
        @NotNull Budget aBudget = a.budget();
        if (!qBudget.isDeleted() && !aBudget.isDeleted() && q.onAnswered(a)) {

            BudgetFunctions.transferPri(qBudget, aBudget, factor);
            if (!qEtern) {
                //if temporal question, also affect the quality so that it will get unranked by more relevant questions in the future
                qBudget.quaMult(1 - factor);
            }

            boolean sameConcept;
            if (answerConcept != null && answerConcept.crossLink(a, q, scale * aConf, nar)) {
                //check if different concepts; ex: if there is a reduction in variables, etc
                sameConcept = false;
            } else {
                sameConcept = true;
            }

            if (Param.DEBUG_ANSWERS && !sameConcept) {
                //if (q.term().equals(a.term()))
                //if (!sameConcept) {
                nar.logger.debug("Q&A: {}\t{}", q, a);
                //} else {
                //  nar.logger.debug("Q&A: {}\t{}", q, a.truth());
                //}
            }

            //amount boosted will be in proportion to the lack of quality, so that a high quality q will survive longer by not being drained so quickly
            //BudgetFunctions.transferPri(q.budget(), a.budget(), (1f - q.qua()) * aConf);
            return true;

        } else {
            //the qustion self-destructed
            return false;
        }


//        //generate a projected answer
        //WARNING this creates a huge amount of useless answers
//        if (!qEtern && !aEtern && q.occurrence()!=a.occurrence()) {
//            Concept ac = nar.concept(a);
//            if (ac != null) { //??
//                Task ap = ac.merge(q, a, q.occurrence(), nar);
//                if (ap != null && !ap.isDeleted()) {
//                    if (Stamp.overlapping(q.evidence(), a.evidence()))
//                        nar.inputLater(ap); //avoid infinite loop
//                    else
//                        nar.input(ap);
//
//                    return;
//                }
//            }
//        }

    }

    @Nullable
    @Override
    public final Task add(@NotNull Task question, @NotNull BeliefTable answers, List<Task> displ, @NotNull NAR n) {

        Task questioned = insert(question, displ);

        //inserted if questioned!=null
        if (questioned != null && !answers.isEmpty()) {
            Task a = answers.top(questioned.occurrence());

            if (a != null && !a.isDeleted()) {

                answer(questioned, a, 1f / size(), null, n);
            }
        }


        return questioned;
    }


    @Nullable
    private Task insert(@NotNull Task t, @NotNull List<Task> displaced) {


        float tp = t.pri();


        final Task[] result = new Task[1];

        list.withWriteLockAndDelegate(l -> {


            int sizeStart = l.size();
            if (sizeStart > 0) {

                list.sortThis(this);

                if (sizeStart >= capacity()) {
                    if (list.get(sizeStart - 1).qua() > tp) {
                        t.log("Insufficient Priority");
                        result[0] = null;
                        return;
                    } else {
                        // FIFO, remove oldest question (last)
                        float removedPri = remove(list, sizeStart - 1, "Table Pop", displaced);
                        if (removedPri == removedPri) //not deleted
                            t.budget().setPriority(Math.max(t.pri(), removedPri)); //utilize at least its priority since theyre sorted by other factor
                    }
                }
            }

            //insert in sorted order by qua
            int i = 0;
            int sizeInsert = list.size();
            for (; i < sizeInsert - 1; i++) {
                if (list.get(i).qua() < tp)
                    break;
            }
            list.add(i, t);

            result[0] = t;
        });


        return t;
    }

    private float remove(Task q, Object reason, @NotNull List<Task> displaced) {

        if (list.remove(q)) {
            if (Param.DEBUG)
                q.log(reason);
            displaced.add(q);
        }
        return q.pri();


    }

    private float remove(List<Task> l, int n, Object reason, @NotNull List<Task> displaced) {

        Task removed = l.remove(n);
        if (Param.DEBUG)
            removed.log(reason);
        displaced.add(removed);
        return removed.pri();

    }

//    /** returns the original, "revised", or null question to input
//     * @param t incoming question which may be unique, overlapping, or equivalent to an existing question in the table
//     * */
//    private Task tryMerge(@NotNull Task t, @NotNull Memory m) {
//
//
////        List<Task> list1 = this.list;
////        long[] tEvidence = t.evidence();
////        long occ = t.occurrence();
////        for (int i = 0, list1Size = list1.size(); i < list1Size; i++) {
////            Task e = list1.get(i);
////
////            //TODO merge occurrence time if within memory's duration period
////
////            if (occ != e.occurrence())
////                continue;
////
////            long[] eEvidence = e.evidence();
////
////            long[] zipped = Stamp.zip(tEvidence, eEvidence);
////
////            //construct before removing so the budgets arent deleted
////            MutableTask te = new MutableTask(
////                    t, e,
////                    m.time(), occ,
////                    zipped, BudgetMerge.max);
////
////            replace(i, te);
////
////            t.delete("Merged");
////
////
////        /*if (Global.DEBUG_DERIVATION_STACKTRACES && Global.DEBUG_TASK_LOG)
////            task.log(Premise.getStack());*/
////
////            //eventTaskRemoved.emit(task);
////
////        /* else: a more destructive cleanup of the discarded task? */
////
////            return null;
////
////        }
//        return t;
//    }


//    private void replace(int index, @NotNull Task newTask) {
//        list.set(index, newTask);
//    }

    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return list.iterator();
    }

    @Override
    public final void forEach(@NotNull Consumer<? super Task> action) {
        for (int i = 0, listSize = list.size(); i < listSize; i++) {
            Task t = list.get(i);
            if (t != null && !t.isDeleted())
                action.accept(t);
        }
    }

    @Override
    public int compare(@NotNull Task o1, @NotNull Task o2) {
        return Float.compare(o2.qua(), o1.qua());
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

}

