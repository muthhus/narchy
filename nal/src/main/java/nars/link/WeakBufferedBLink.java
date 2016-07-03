package nars.link;

import nars.budget.Budgeted;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * Created by me on 5/29/16.
 */
public class WeakBufferedBLink<X> extends DefaultBLink<X> {

    ///** the referred item */
    @NotNull
    public final WeakReference<X> id;

    public WeakBufferedBLink(X id, @NotNull Budgeted b, float scal) {
        super(id, b, scal);
        this.id = new WeakReference<>(id);
    }


    @Override
    public boolean delete() {
        if (super.delete()) {
            id.clear();
            changed = true;
            return true;
        }
        return false;
    }

    @Override
    public boolean isDeleted() {

        if (super.isDeleted()) {
            return true;
        } else {
            //if the weak ref'd item is lost then delete this link
            if (get() == null) {
                delete();
                return true;
            }
            return false;
        }
    }

    @Override
    public boolean commit() {
        //check existence
        X val = id.get();
        return val == null ? delete() : super.commit();
    }

    @Nullable
    @Override
    public final X get() {
        return id.get();
    }


}
