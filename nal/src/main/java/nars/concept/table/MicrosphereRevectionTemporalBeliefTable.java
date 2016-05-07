package nars.concept.table;

import nars.Global;
import nars.bag.impl.ListTable;
import nars.bag.impl.SortedArrayTable;
import nars.bag.impl.SortedTable;
import nars.task.Task;
import org.happy.collections.lists.decorators.SortedList_1x4;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/** stores the items unsorted; revection manages their ranking and removal */
public class MicrosphereRevectionTemporalBeliefTable extends ArrayListTable<Task,Task> {

    private final SortedTable<Task, Task> eternal;

    public MicrosphereRevectionTemporalBeliefTable(Map<Task, Task> mp, int cap, SortedTable<Task,Task> eternal) {
        super(mp, Global.newArrayList(cap));
        setCapacity(cap);
        this.eternal = eternal;
    }

    @Override
    public Task key(Task task) {
        return task;
    }

    //    @Override
//    protected Task addItem(Task i) {
//        if (size() >= capacity()) //should ensure space for this task before calling
//            throw new RuntimeException("temporal belief table fault");
//
//        ((SortedList_1x4)list()).list.add(i); //store unsorted directly
//        return null;
//    }

}
