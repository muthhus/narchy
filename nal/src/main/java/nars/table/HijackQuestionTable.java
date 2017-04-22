package nars.table;

import nars.NAR;
import nars.bag.TaskHijackBag;

/**
 * Created by me on 2/16/17.
 */
public class HijackQuestionTable extends TaskHijackBag implements QuestionTable {


    public HijackQuestionTable(int cap, int reprobes) {
        super(reprobes);

        capacity(cap);
    }


//    @Override
//    public float pri(@NotNull Task key) {
//        return (1f + key.priSafe(0)) * (1f * key.qua());
//    }



    @Override
    public void capacity(int newCapacity, NAR nar) {
        setCapacity(newCapacity); //hijackbag
        capacity(newCapacity); //question table
    }


}
