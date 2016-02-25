package nars.term;

import com.googlecode.concurrenttrees.common.*;
import com.googlecode.concurrenttrees.radix.MyConcurrentRadixTree;
import com.googlecode.concurrenttrees.radix.node.concrete.SmartArrayBasedNodeFactory;
import com.googlecode.concurrenttrees.radix.node.util.PrettyPrintable;
import nars.Op;
import nars.term.atom.Atomic;
import org.jetbrains.annotations.Nullable;

/**
 * String interner that maps strings to integers and resolves them
 * bidirectionally with a globally shared Atomic concept
 */
public class Atoms extends MyConcurrentRadixTree<Integer> {

    private static volatile int serial = 0;

    public Atoms() {
        super(new SmartArrayBasedNodeFactory());
    }

    public final int resolve(CharSequence id) {
        Integer i = getValueForExactKey(id);
        return i == null ? -1 : i;
    }
    public final int resolveOrAdd(CharSequence id) {
        int exists = resolve(id);
        if (exists == -1) {
            int newSerial = ++serial;
            putIfAbsent(id, newSerial);
            return newSerial;
        }
        return exists;
    }

    public void print() {
        System.out.println("Tree structure:");
        // PrettyPrintable is a non-public API for testing, prints semi-graphical representations of trees...
        PrettyPrinter.prettyPrint((PrettyPrintable) tree, System.out);
    }

    public final class InternedAtom extends Atomic /* implements Concept */ {

        public final int id;

        public InternedAtom(int id) {
            this.id = id;
        }

        @Override
        public
        @Nullable
        String toString() {
            return null;
        }

        @Override
        public @Nullable
        Op op() {
            return null;
        }

        @Override
        public int complexity() {
            return 0;
        }

        @Override
        public int varIndep() {
            return 0;
        }

        @Override
        public int varDep() {
            return 0;
        }

        @Override
        public int varQuery() {
            return 0;
        }

        @Override
        public int varPattern() {
            return 0;
        }

        @Override
        public int vars() {
            return 0;
        }

        @Override
        public int compareTo(Object o) {
            return 0;
        }
    }


}
