package nars.concept.util;

import nars.Global;
import nars.Memory;
import nars.budget.BudgetMerge;
import nars.task.Task;

import java.util.Iterator;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;

/**
 * implements a Task table suitable for Questions and Quests using an ArrayList.
 * uses a List and assumes that it is an ArrayList that can be
 * accessed by index.
 *
 */
public class ArrayListTaskTable implements QuestionTaskTable {

    protected int capacity = 0;

    private final List<Task> list;

    public ArrayListTaskTable(int capacity) {
        super();
        this.list = Global.newArrayList(capacity);
        setCapacity(capacity);
    }

    @Override
    public int getCapacity() {
        return capacity;
    }

    //TODO not tested yet
    @Override public void setCapacity(int newCapacity) {
        if (this.capacity==newCapacity) return;

        capacity = newCapacity;

        int s = list.size();

        int toRemove = s - capacity;
        while (toRemove-- > 0)
            list.remove( --s ); //last element

    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }


    /**
     * iterator-less implementation
     */
    @Override
    public Task getFirstEquivalent(Task t, BiPredicate<Task,Task> e) {
        if (isEmpty()) return null;

        List<Task> ll = this.list;
        for (int i = 0; i < ll.size(); i++) {
            Task a = ll.get(i);
            if (e.test(a, t))
                return a;
        }
        return null;
    }


    @Override
    public Task add(Task t, BiPredicate<Task,Task> equality, BudgetMerge duplicateMerge, Memory m) {

        Task existing = getFirstEquivalent(t, equality);
        if (existing != null) {

            if (existing != t) {
                duplicateMerge.merge(existing.getBudget(), t.getBudget(), 1f);
                m.remove(t, "PreExisting TaskTable Duplicate");
            }

            return existing;
        }

        //Memory m = c.getMemory();
        int siz = size();
        if (siz + 1 > capacity) {
            // FIFO, remove oldest question (last)
            /*Task removed = */list.remove(siz - 1);

            //m.remove(removed, "TaskTable FIFO Out");

            //m.emit(Events.ConceptQuestionRemove.class, c, removed /*, t*/);
        }

        list.add(0, t);

        //m.emit(Events.ConceptQuestionAdd.class, c, t);

        return t;
    }

    @Override
    public Iterator<Task> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(Consumer<? super Task> action) {
        list.forEach(action);
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

