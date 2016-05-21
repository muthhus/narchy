package nars.nal.meta;

import com.google.common.base.Joiner;
import com.gs.collections.impl.map.mutable.primitive.ObjectIntHashMap;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.magnos.trie.Trie;
import org.magnos.trie.TrieNode;
import org.magnos.trie.TrieSequencer;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;



/** indexes sequences of (a perfectly-hashable fixed number
 * of unique) terms in a magnos trie */
abstract public class TermTrie<K extends Term, V> {

    @NotNull
    public final Trie<List<K>, V> trie;

    public void printSummary() {
        printSummary(System.out);
    }

    public void printSummary(@NotNull PrintStream out) {
        printSummary(trie.root, out);
    }


    public TermTrie(@NotNull Iterable<V> R) {
        super();

        ObjectIntHashMap<Term> conds = new ObjectIntHashMap<>();

        trie = new Trie(new TrieSequencer<List<K>>() {

            @Override
            public int matches(@NotNull List<K> sequenceA, int indexA, @NotNull List<K> sequenceB, int indexB, int count) {
                for (int i = 0; i < count; i++) {
                    K a = sequenceA.get(i + indexA);
                    K b = sequenceB.get(i + indexB);
                    if (!a.equals(b))
                        return i;
                }

                return count;
            }

            @Override
            public int lengthOf(@NotNull List<K> sequence) {
                return sequence.size();
            }

            @Override
            public int hashOf(@NotNull List<K> sequence, int index) {
                //return sequence.get(index).hashCode();

                Term pp = sequence.get(index);
                return conds.getIfAbsentPutWithKey(pp, (p) -> 1 + conds.size());
            }
        });

        R.forEach(this::index);
    }

    /** called for each item on insert */
    abstract public void index(V v);

    public static <A, B> void printSummary(@NotNull TrieNode<List<A>,B> node, @NotNull PrintStream out) {

        node.forEach(n -> {
            List<A> seq = n.seq();

            int from = n.start();
            int to = n.end();


            out.print(n.childCount() + "|" + n.getSize() + "  ");

            indent(from * 2);

            out.println(Joiner.on(", ").join( seq.subList(from, to)));

            printSummary(n, out);
        });

    }
    public static void indent(int amount) {
        for (int i = 0; i < amount; i++) {
            System.out.print(' ');
        }
    }

    public String getSummary() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        printSummary(new PrintStream(baos));
        return baos.toString();
    }
}
