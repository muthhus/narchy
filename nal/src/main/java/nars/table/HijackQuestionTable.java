package nars.table;

import jcog.pri.Prioritized;
import nars.NAR;
import nars.Task;
import nars.bag.TaskHijackBag;
import nars.concept.BaseConcept;
import org.jetbrains.annotations.NotNull;

/**
 * Created by me on 2/16/17.
 */
public class HijackQuestionTable extends TaskHijackBag implements QuestionTable {


    public HijackQuestionTable(int cap, int reprobes) {
        super(cap, reprobes);
    }


//    @Override
//    public float pri(@NotNull Task key) {
//        return (1f + key.priElseZero()) * (1f * key.qua());
//    }


    @Override
    public void add(@NotNull Task x, BaseConcept c, NAR n) {
        super.add(x, c, n);

        if (pressure.floatValue() >= Prioritized.EPSILON)
            commit(); //apply forgetting
    }

    @Override
    public void capacity(int newCapacity) {
        setCapacity(newCapacity); //hijackbag
    }


}
