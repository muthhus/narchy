package nars.task.util;

import jcog.bag.impl.ArrayBag;
import jcog.map.SynchronizedHashMap;
import jcog.pri.PriReference;
import jcog.pri.op.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.op.Command;
import org.jetbrains.annotations.NotNull;

public final class AnswerBag extends ArrayBag<Task> {

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
            Command.log(nar, question + "  " + x.get().toString());
    }
}
