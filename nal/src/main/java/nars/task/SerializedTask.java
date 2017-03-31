package nars.task;

import jcog.data.byt.ByteSeq;
import nars.NAR;
import nars.Task;
import nars.budget.Budget;
import nars.budget.Budgeted;
import nars.term.Compound;
import nars.truth.Truth;
import nars.truth.TruthDelta;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * TODO
 */
public class SerializedTask extends ByteSeq.WindowByteSeq implements Task {

    public SerializedTask(byte[] b) {
        super(b, 0, b.length);
    }

    @Override
    public float pri() {
        return 0;
    }

    @Override
    public boolean delete() {
        return false;
    }


    @Override
    public @Nullable Truth truth() {
        return null;
    }

    @Override
    public @NotNull Budget budget() {
        return null;
    }

    @Override
    public float qua() {
        return 0;
    }

    @Override
    public Budget setBudget(@Nullable Budgeted srcCopy) {
        return null;
    }

    @Override
    public byte punc() {
        return 0;
    }

    @Override
    public long creation() {
        return 0;
    }

    @Override
    public @NotNull Compound term() {
        return null;
    }

    @Override
    public long start() {
        return 0;
    }

    @Override
    public long end() {
        return 0;
    }

    @Override
    public @NotNull long[] stamp() {
        return new long[0];
    }

    @Override
    public void feedback(TruthDelta delta, float deltaConfidence, float deltaSatisfaction, NAR nar) {

    }

    @Override
    public Map meta() {
        return null;
    }

    @Override
    public <X> X meta(Object key) {
        return null;
    }

    @Override
    public void meta(Object key, Object value) {

    }
}
