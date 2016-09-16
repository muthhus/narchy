package nars.util.version;

import nars.util.data.list.FasterList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** versioning context that holds versioned instances */
public class Versioning extends FasterList<Versioned> {

    private final int capacity;

    public Versioning(int capacity) {
        super(0, new Versioned[capacity]);
        this.capacity = capacity;
    }


    private int now;

    /** serial id's assigned to each Versioned */
    private int nextID = 1;


    @NotNull
    @Override
    public String toString() {
        return now + ":" + super.toString();
    }

    public final int now() {
        return now;
    }


    /** increment the version count, returns the new current version
     *  @return -1 if capacity exceeded
     * */
    private final int next() {
        int c = ++now;
        if (c == capacity) {
            now--; //return to original value
            return -1;
        } else {
            return c;
        }
    }

    /** start a new version with a commit, returns true if add was successful or false if unsuccessful (capacity exceeded)
     *  @return null if capacity exceeded
     * */
    public final boolean nextChange(@Nullable Versioned v, @Nullable Object x) {
        int c = next();
        if (c!=-1) {
            if (v!=null)
                v.add(x);
            add(v);
            return true;
        }
        return false;
    }

//    /** track change on current commit, returns current version */
//    public final int continueChange(Versioned v) {
//        if (!addIfCapacity(v))
//            throw new RuntimeException();
//        return now;
//    }


    /** reverts to previous state */
    public final void revert() {
        revert(now -1);
    }

    /** reverts/undo to previous state */
    public final void revert(int when) {
        for (int i = 0; i < (now-when); i++) {
            Versioned versioned = removeLast();
            if (versioned!=null)
                versioned.removeLast();
        }
        now = when;
    }


//    private void doRevertPop(int when, final int start) {
//        int s = start;
//
//        Versioned[] ii = this.items;
//        while (ii[s].revertNext(1)) {
//            if (--s < 0)
//                break;
//        }
//
//        if (start!=s) {
//            doRevertClean(s+1, ii, start - s);
//        }
//    }

//    private void doRevertClean(int s, Versioned[] ii, int popped) {
//
//        if (popped > 1) {
//            Arrays.fill(ii, s, s + popped, null);
//        } else { //if (popped == 1) {
//            ii[s] = null;
//        }
//
//        popTo(s-1);
//    }

//    /** assigns a new serial ID to a versioned item for its use as a hashcode */
//    public final int track() {
//        return nextID++;
//    }


    @Override
    public void clear() {
        revert(0);
    }


    public final boolean isFull() {
        return now+1 >= capacity;
    }
}
