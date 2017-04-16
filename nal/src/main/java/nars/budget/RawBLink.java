package nars.budget;

import jcog.bag.PLink;
import jcog.bag.Prioritized;
import org.jetbrains.annotations.NotNull;

/**
 * Buffered/Budgeted Link (an entry in a bag)
 * equalsTo/hashCode proxies to the wrapped element, X id
 *
 * Acts as a "budget vector" containing an accumulating delta
 * that can be commit()'d on the next udpate
 */
public class RawBLink<X> extends RawBudget implements BLink<X> {


    ///** the referred item */
    @NotNull protected final X id;

    public RawBLink(@NotNull X id) {
        this.id = id;
    }

    public RawBLink(@NotNull X id, float p) {
        this.id = id;
        this.priority = p;
    }

    public RawBLink(@NotNull X id, @NotNull Prioritized b) {
        this(id, b.priSafe(0));
    }

    public RawBLink(@NotNull X id, @NotNull Prioritized b, float scale) {
        this(id, b.pri() * scale);
    }



    @Override
    public final boolean equals(@NotNull Object that) {
        if (this==that)
            return true;

        Object b = ((PLink) that).get();
        return this.id.equals(b);
    }

    @Override
    public final int hashCode() {
        return id.hashCode();
    }

    @Override
    public @NotNull String toString() {
        return id + "=" + super.toString();
    }

    @Override
    public boolean delete() {
        float p = this.priority;
        if (p==p) {
            //not already deleted
            this.priority = (Float.NaN);
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public final X get() {
        return id;
    }

}
