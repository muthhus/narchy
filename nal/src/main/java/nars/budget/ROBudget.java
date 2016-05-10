package nars.budget;

import org.jetbrains.annotations.NotNull;

/**
 * reverse osmosis read-only budget
 */
public final class ROBudget extends Budget {

    private final float pri, dur, qua;

    public ROBudget(float pri, float dur, float qua) {
        this.pri = pri;
        this.dur = dur;
        this.qua = qua;
    }

    @Override
    public final boolean isDeleted() {
        return false;
    }

    @Override
    public void delete() {
        throw new UnsupportedOperationException();
    }

    @Override
    protected void _setPriority(float p) {
        throw new UnsupportedOperationException();
    }

    @Override
    public float setLastForgetTime(float currentTime) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void _setDurability(float d) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void _setQuality(float q) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    @Override
    public Budget clone() {
        return new UnitBudget(this);
    }

    @Override
    public float pri() {
        return pri;
    }

    @Override
    public float qua() {
        return qua;
    }

    @Override
    public float dur() {
        return dur;
    }

    @Override
    public float getLastForgetTime() {
        return Float.NaN;
    }
}
