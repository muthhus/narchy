package nars.task.util;

import jcog.bag.impl.PLinkArrayBag;
import jcog.map.SynchronizedHashMap;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.op.Operation;
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
    public void onAdded(@NotNull PriReference<Task> x) {
        if (Param.ANSWER_REPORTING)
            Operation.log(nar, question + "  " + x.get());
    }
}
