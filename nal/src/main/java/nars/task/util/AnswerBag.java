package nars.task.util;

import jcog.bag.impl.ArrayBag;
import jcog.map.SynchronizedHashMap;
import jcog.pri.PLink;
import jcog.pri.PriMerge;
import nars.NAR;
import nars.Param;
import nars.Task;
import nars.op.Command;
import org.jetbrains.annotations.NotNull;

public final class AnswerBag extends ArrayBag<Task> {

    private final NAR nar;

    public AnswerBag(@NotNull NAR nar) {
        super(PriMerge.max, new SynchronizedHashMap<>(1));
        this.nar = nar;
    }

    @Override
    public void onAdded(@NotNull PLink<Task> x) {
        if (Param.ANSWER_REPORTING)
            Command.log(nar, this.toString() + "  " + x.get().toString());
    }
}
