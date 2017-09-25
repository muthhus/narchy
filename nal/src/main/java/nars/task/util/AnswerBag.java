package nars.task.util;

import jcog.bag.impl.PLinkArrayBag;
import jcog.map.SynchronizedHashMap;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.op.Operator;
import org.jetbrains.annotations.NotNull;

public final class AnswerBag extends PLinkArrayBag<Task> {

    private final NAR nar;
    private final Task question;

    public AnswerBag(@NotNull NAR nar, Task question, int capacity) {
        super(capacity, PriMerge.max, new SynchronizedHashMap<>(capacity));

        this.nar = nar;
        this.question = question;

    }

    @Override
    public void onAdd(@NotNull PriReference<Task> x) {
        if (Param.ANSWER_REPORTING)
            nar.input(Operator.log(nar.time(), question + "  " + x.get()));
    }
}
