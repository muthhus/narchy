package nars.link;

import org.jetbrains.annotations.Nullable;


public class StrongBLink<X> extends DefaultBLink<X> {

    ///** the referred item */
    public X id;

    public StrongBLink(X id, float p, float d, float q) {
        super(id, p, d, q);
        this.id = id;
    }

    @Override
    public boolean delete() {
        id = null;
        return super.delete();
    }

    @Nullable
    @Override
    public final X get() {
        return id;
    }

}
