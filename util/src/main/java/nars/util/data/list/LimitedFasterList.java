package nars.util.data.list;


/**
 * Created by me on 5/26/16.
 */
public final class LimitedFasterList<X> extends FasterList<X> {

    final int max;

    public LimitedFasterList(int max) {
        super(0); //start empty
        this.max = max;
    }

    final void ensureLimit() {
        if (size() + 1 > max) {
            throw new RuntimeException("limit exceeded");
            //+ this + " while trying to add " + x);
        }
    }

    @Override
    public boolean add(X newItem) {
        ensureLimit();
        return super.add(newItem);
    }

    @Override
    public void add(int index, X element) {
        ensureLimit();
        super.add(index, element);
    }
}
