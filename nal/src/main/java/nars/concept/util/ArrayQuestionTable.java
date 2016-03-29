package nars.concept.util;

import nars.Global;
import nars.Memory;
import nars.budget.BudgetMerge;
import nars.task.MutableTask;
import nars.task.Task;
import nars.truth.Stamp;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;

/**
 * implements a Task table suitable for Questions and Quests using an ArrayList.
 * uses a List and assumes that it is an ArrayList that can be
 * accessed by index.
 *
 * TODO use a ring-buffer deque slightly faster than basic ArrayList modification
 */
public class ArrayQuestionTable implements QuestionTable {

    protected int capacity;

    @NotNull
    private final List<Task> list;

    public ArrayQuestionTable(int capacity) {
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
    @Nullable
    @Override
    public Task contains(Task t) {
        if (isEmpty()) return null;

        List<Task> ll = this.list;
        int s = ll.size();
        for (int i = 0; i < s; i++) {
            Task a = ll.get(i);
            if (a.equals(t))
                return a;
        }
        return null;
    }


    @Nullable
    @Override
    public Task add(@NotNull Task t, @NotNull Memory m) {


        t = tryMerge(t, m);
        if (t == null)
            return null;

        int siz = size();
        if (siz + 1 > capacity) {
            // FIFO, remove oldest question (last)
            remove(siz-1, m, "FIFO Forgot");
        }

        insert(t);

        return t;
    }

    public void insert(@NotNull Task t) {
        list.add(0, t);
    }

    public void remove(int n, @NotNull Memory m, String reason) {
        Task removed = list.remove(n);
        m.remove(removed, reason);
    }

    /** returns the original, "revised", or null question to input
     * @param t incoming question which may be unique, overlapping, or equivalent to an existing question in the table
     * */
    private Task tryMerge(@NotNull Task t, @NotNull Memory m) {
        if (isEmpty())
            return t;

        Task existing = contains(t);
        if (existing != null) {

            if (existing != t) {
                BudgetMerge.plusDQBlend.merge(existing.budget(), t, 1f);
                m.remove(t, "Duplicate Question");
            }

            return null;
        }

        List<Task> list1 = this.list;
        long[] tEvidence = t.evidence();
        long occ = t.occurrence();
        for (int i = 0, list1Size = list1.size(); i < list1Size; i++) {
            Task e = list1.get(i);

            //TODO merge occurrence time if within memory's duration period

            if (occ != e.occurrence())
                continue;

            long[] eEvidence = e.evidence();

            long[] zipped = Stamp.zip(tEvidence, eEvidence);

            //construct before removing so the budgets arent deleted
            MutableTask te = new MutableTask(
                    t, e,
                    m.time(), occ,
                    zipped, BudgetMerge.plusDQBlend);

            remove(i, m, "Merged");
            m.remove(t, "Merged");

            //insert quietly, pretending as if already existed
            insert(te);
            return null;

        }
        return t;
    }


    @NotNull
    @Override
    public Iterator<Task> iterator() {
        return list.iterator();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Task> action) {
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

