package nars.op.mental;

import nars.IO;
import nars.NAR;
import nars.budget.Budget;
import nars.concept.CompoundConcept;
import nars.nar.Default;
import nars.term.Term;
import net.byteseek.io.reader.ByteArrayReader;
import net.byteseek.matcher.multisequence.ListMultiSequenceMatcher;
import net.byteseek.matcher.multisequence.MultiSequenceMatcher;
import net.byteseek.matcher.multisequence.TrieMultiSequenceMatcher;
import net.byteseek.matcher.sequence.ByteSequenceMatcher;
import net.byteseek.matcher.sequence.SequenceMatcher;
import org.jetbrains.annotations.NotNull;
import org.mockito.internal.util.concurrent.WeakConcurrentMap;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by me on 2/11/17.
 */
public class Compressor extends Abbreviation {

    private MultiSequenceMatcher encoder;
    private MultiSequenceMatcher decoder;

    final Map<CompoundConcept, Abbr> code = new ConcurrentHashMap<>();
    final WeakConcurrentMap<SequenceMatcher, Abbr> codeRev = new WeakConcurrentMap(true); //HACK

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
            codeRev.put(encode, this);
            codeRev.put(decode, this);
        }
    }


    public Compressor(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int capacity) {
        super(n, termPrefix, volMin, volMax, selectionRate, capacity);
    }

    protected void abbreviate(@NotNull CompoundConcept abbreviated, @NotNull Budget b) {
        final boolean[] changed = {false};
        synchronized (code) { //TODO not synch
            code.computeIfAbsent(abbreviated, (a) -> {

                String compr = newSerialTerm();

                System.out.println("compressing: " + a + " to " + compr);

                changed[0] = true;
                return new Abbr(a.term(), new AliasConcept(compr, a, nar));
            });
            if (changed[0]) {
                recompile();
            }
        }
    }

    private void recompile() {

        decoder = multi(x -> x.decode);
        encoder = multi(x -> x.encode);

    }

    private MultiSequenceMatcher multi(Function<Abbr, SequenceMatcher> theDecode) {
        //SequenceMatcherTrieFactory factory = new SequenceMatcherTrieFactory();

        List<SequenceMatcher> mm = code.values().stream().map(theDecode).collect(Collectors.toList());
        //decoder = //new SetHorspoolSearcher(
        switch (mm.size()) {
            case 0:
                return null;
            case 1:
            default:
                return new ListMultiSequenceMatcher(mm);

            //default:
               // return new TrieMultiSequenceMatcher.( mm );

            //default:
                //throw new UnsupportedOperationException();
        }
    }


    public byte[] encode(byte[] b) {

        if (encoder == null)
            return b;

        byte[] start = b;

        int i = 0;
        int l = b.length;

        //System.out.println(encoder + ": " + new String(b));

        ByteArrayReader wr = null;

        do {

            if (wr == null) {
                 wr = new ByteArrayReader(b);
            }

            try {
                SequenceMatcher m = encoder.firstMatch(wr, i);
                //m = encoder.allMatches(b, i);
                if (m == null) {
                    i++;
                    continue;
                }

                Abbr c = abbr(m);
                System.out.println("match: " + c + " @ " + i);

                int newLength = b.length + c.encoded.length - c.decoded.length;
                byte[] prev = b;
                b = new byte[newLength];
                System.arraycopy(prev, 0, b, 0, i);
                System.arraycopy(c.encoded, 0, b, i, c.encoded.length);
                if (i < l)
                    System.arraycopy(prev, i + c.decoded.length, b, i + c.encoded.length, l - (i + c.decoded.length));
                wr = null;

                //dont advance if changed

            } catch (IOException e) {
                e.printStackTrace();
            }

        } while (i < l);

        if (b!=start) {
            System.out.println("\t\t" + new String(b));
            System.out.println("\t\t\t" + IO.termFromBytes(b, nar.concepts));
        }

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

    private Abbr abbr(SequenceMatcher m) {
        return codeRev.get(m);
    }

    public byte[] decode(byte[] b) {
        return b;
    }

    public static void main(String[] args) {
        Default n = new Default();
        Compressor c = new Compressor(n, "_", 2, 8, 0.5f, 8);

        n.onTask(x -> {
            byte[] tb = IO.asBytes(x.term());
            c.encode(tb);
        });
        n.log();
        n.input("a:b. b:(a,c, 1, 1, 2). c:(d,a). d:e.");
        n.run(200);


    }
}
