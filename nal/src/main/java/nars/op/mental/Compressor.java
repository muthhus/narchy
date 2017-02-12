package nars.op.mental;

import nars.$;
import nars.IO;
import nars.NAR;
import nars.Task;
import nars.budget.Budget;
import nars.concept.CompoundConcept;
import nars.nar.Default;
import nars.task.MutableTask;
import nars.term.Compound;
import nars.term.Term;
import net.byteseek.automata.factory.MutableStateFactory;
import net.byteseek.automata.trie.Trie;
import net.byteseek.automata.trie.TrieFactory;
import net.byteseek.io.reader.ByteArrayReader;
import net.byteseek.matcher.automata.ByteMatcherTransitionFactory;
import net.byteseek.matcher.automata.SequenceMatcherTrie;
import net.byteseek.matcher.multisequence.ListMultiSequenceMatcher;
import net.byteseek.matcher.multisequence.MultiSequenceMatcher;
import net.byteseek.matcher.multisequence.TrieMultiSequenceMatcher;
import net.byteseek.matcher.sequence.ByteSequenceMatcher;
import net.byteseek.matcher.sequence.SequenceMatcher;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import static nars.term.Terms.compoundOrNull;

/**
 * Created by me on 2/11/17.
 */
public class Compressor extends Abbreviation {

    final MutableStateFactory<SequenceMatcher> stateFactory = new MutableStateFactory<>();
    final ByteMatcherTransitionFactory<SequenceMatcher> transitionFactory = new ByteMatcherTransitionFactory<>();

    private MultiSequenceMatcher encoder;
    private MultiSequenceMatcher decoder;

    final Map<Compound, Abbr> code = new ConcurrentHashMap<>();
    final Map<SequenceMatcher, Abbr> dc = new ConcurrentHashMap(); //HACK
    final Map<SequenceMatcher, Abbr> ec= new ConcurrentHashMap(); //HACK

    /* static */ class Abbr {
        public final Term decompressed;
        public final AliasConcept compressed;
        final SequenceMatcher encode;
        final SequenceMatcher decode;
        private final byte[] encoded;
        private final byte[] decoded;

        Abbr(Term decompressed, AliasConcept compressed) {
            this.decompressed = decompressed;
            this.compressed = compressed;
            this.encoded = IO.asBytes(compressed.term());
            this.decode = new ByteSequenceMatcher(encoded);
            this.decoded = IO.asBytes(decompressed);
            this.encode = new ByteSequenceMatcher(decoded);

            //HACK
            ec.put(encode, this);
            dc.put(decode, this);
        }
    }


    public Compressor(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
        super(n, termPrefix, volMin, volMax, selectionRate, capacity);
    }

    @Override
    protected void abbreviate(@NotNull Compound abbreviated, @NotNull Budget b) {
        final boolean[] changed = {false};
        synchronized (code) { //TODO not synch
            code.computeIfAbsent(abbreviated, (a) -> {

                String compr = newSerialTerm();

                System.out.println("compress CODE: " + a + " to " + compr);

                changed[0] = true;
                AliasConcept ac = AliasConcept.get(compr, a, nar);
                nar.on(ac);
                return new Abbr(a.term(), ac);
            });
            if (changed[0]) {
                recompile();
            }
        }
    }

    private void recompile() {

        decoder = matcher(x -> x.decode);
        encoder = matcher(x -> x.encode);

    }

    public final TrieFactory<SequenceMatcher> smFactory = new TrieFactory<SequenceMatcher>() {

        @Override
        public Trie create(Collection<? extends SequenceMatcher> sequences) {
            return new SequenceMatcherTrie(sequences, stateFactory, transitionFactory);
        }
    };

    private MultiSequenceMatcher matcher(Function<Abbr, SequenceMatcher> theDecode) {
        //SequenceMatcherTrieFactory factory = new SequenceMatcherTrieFactory();

        List<SequenceMatcher> mm = code.values().stream().map(theDecode).collect(Collectors.toList());
        switch (mm.size()) {
            case 0:
                return null;

            case 1:
                return new ListMultiSequenceMatcher(mm);

            default:
                return new TrieMultiSequenceMatcher(smFactory, mm);

        }
    }

    @NotNull public Task encode(Task tt) {
        return transcode(tt, true);
    }
    @NotNull public Task decode(Task tt) {
        return transcode(tt, false);
    }

    public Task transcode(Task tt, boolean en) {

        Term i = tt.term();
        Term o = transcode(i, en);
        if (o!=i) {

            try {
                //HACK wrap in product if atomic
//                if (!(o instanceof Compound))
//                    o = $.p(o);

                //System.out.println("  compress: " + i + " to " + o);

                if (o instanceof Compound)
                    return MutableTask.clone(tt, (Compound) o);
            } catch (Throwable t) {
                //HACK ignore
            }
        }

        return tt;

    }

    @NotNull public Term transcode(Term t, boolean en) {
        if (!(t instanceof Compound))
            return t;
        else {
            byte[] ii = IO.asBytes(t);
            byte[] oo = transcode(ii, en);
            return ii != oo ? IO.termFromBytes(oo, nar.concepts) : t;
        }
    }

    @NotNull public Term encode(Term t) {
        return transcode(t, true);
    }

    @NotNull public Term decode(Term t) {
        return transcode(t, false);
    }

    public byte[] transcode(byte[] b, boolean en) {

        MultiSequenceMatcher coder = en ? this.encoder : this.decoder;
        if (coder == null)
            return b;

        //byte[] bOriginal = b;

        int i = 0;


        ByteArrayReader wr = null;

        do {

            if (wr == null) {
                 wr = new ByteArrayReader(b);
            }

            try {
                SequenceMatcher m = coder.firstMatch(wr, i);
                //m = encoder.allMatches(b, i);
                if (m == null) {
                    i++;
                    continue;
                }

                Abbr c = abbr(m, en);

                final byte[] to = en ? c.encoded : c.decoded;
                final byte[] from = en ? c.decoded : c.encoded;

                final byte[] prev = b;
                int change = to.length - from.length;
                final byte[] next = new byte[b.length + change];

                System.arraycopy(prev, 0, next, 0, i);
                System.arraycopy(to, 0, next, i, to.length);
                int l = b.length;
                if (i < l)
                    System.arraycopy(prev, i + from.length, next, i + to.length, l - (i + from.length));

                wr = null;
                b = next;

                //dont advance if changed

            } catch (IOException e) {
                e.printStackTrace();
            }

        } while (i < b.length);
//
//        if (b!=bOriginal) {
//            System.out.println("\t\t" + new String(b));
//            System.out.println("\t\t\t" + IO.termFromBytes(b, nar.concepts));
//        }

        return b;


//        List<SearchResult<SequenceMatcher>> matches = encoder.searchForwards(b);
//
//        System.out.println(b.toString());
//        for (SearchResult<SequenceMatcher> s : matches) {
//            System.out.println("\t" + s.getMatchingObject().getClass() + ": " + s.getMatchingObject() + " @"  + s.getMatchPosition());
//        }
//
//        return b;
    }

    private Abbr abbr(SequenceMatcher m, boolean en) {
        return (en ? this.ec : this.dc).get(m);
    }



    public static void main(String[] args) {
        Default n = new Default();
        Compressor c = new Compressor(n, "_", 2, 8, 0.5f, 8);

        n.onTask(x -> {
            Task y = c.encode(x);
            if (!y.equals(x))
                System.out.println(y);
        });
        n.log();
        n.input("a:b. b:(a,c, 1, 1, 2). c:(d,a). d:e.");
        n.run(800);


    }
}
