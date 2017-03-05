package nars.op.mental;

import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import jcog.bag.RawPLink;
import jcog.bag.impl.HijackBag;
import nars.*;
import nars.bag.impl.PLinkHijackBag;
import nars.budget.BLink;
import nars.budget.Budget;
import nars.concept.PermanentConcept;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import net.byteseek.automata.factory.MutableStateFactory;
import net.byteseek.automata.trie.TrieFactory;
import net.byteseek.io.reader.ByteArrayReader;
import net.byteseek.matcher.automata.ByteMatcherTransitionFactory;
import net.byteseek.matcher.automata.SequenceMatcherTrie;
import net.byteseek.matcher.multisequence.ListMultiSequenceMatcher;
import net.byteseek.matcher.multisequence.MultiSequenceMatcher;
import net.byteseek.matcher.multisequence.TrieMultiSequenceMatcher;
import net.byteseek.matcher.sequence.ByteSequenceMatcher;
import net.byteseek.matcher.sequence.SequenceMatcher;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import static java.util.stream.Collectors.toList;
import static nars.Op.BELIEF;
import static nars.term.Terms.compoundOr;
import static nars.term.Terms.compoundOrNull;
import static nars.time.Tense.ETERNAL;

/**
 * Created by me on 2/11/17.
 */
public class Compressor extends Abbreviation implements RemovalListener<Compound, Compressor.Abbr> {

    static final Logger logger = LoggerFactory.getLogger(Compressor.class);

    final MutableStateFactory<SequenceMatcher> stateFactory = new MutableStateFactory<>();
    final ByteMatcherTransitionFactory<SequenceMatcher> transitionFactory = new ByteMatcherTransitionFactory<>();
    final TrieFactory<SequenceMatcher> smFactory = sequences -> new SequenceMatcherTrie(sequences, stateFactory, transitionFactory);

    private MultiSequenceMatcher encoder, decoder;

    //final Cache<Compound, Abbr> code;
    final HijackBag<Compound,Abbr> code;

    final Map<SequenceMatcher, Abbr> dc = new ConcurrentHashMap(); //HACK
    final Map<SequenceMatcher, Abbr> ec = new ConcurrentHashMap(); //HACK
    private float boostBig = 0.1f;
    private float boostSmall = 0.05f;


    /* static */ class Abbr extends RawPLink<Compound> {

        @NotNull
        public final AliasConcept compressed;
        final SequenceMatcher encode;
        final SequenceMatcher decode;
        private final byte[] encoded;
        private final byte[] decoded;

        Abbr(Compound decompressed, @NotNull AliasConcept compressed, NAR nar) {
            super(decompressed, 1f);

            this.compressed = compressed;

            this.encoded = IO.asBytes(this.compressed);
            this.decoded = IO.asBytes(decompressed);
            this.decode = new ByteSequenceMatcher(encoded);
            this.encode = new ByteSequenceMatcher(decoded);

            //HACK
            ec.put(encode, this);
            dc.put(decode, this);

            nar.on(this.compressed);
        }
    }


    public Compressor(@NotNull NAR n, String termPrefix, int volMin, int volMax, float selectionRate, int pendingCapacity, int maxCodes) {
        super(n, termPrefix, volMin, volMax, selectionRate, pendingCapacity);

        code =
                //Caffeine.newBuilder().maximumSize(maxCodes).removalListener(this).executor(n.exe).build();
                new PLinkHijackBag(maxCodes, 4, nar.random);
    }


    @Override
    protected float onOut(BLink<Compound> b) {
        Compound x = b.get();
//        if (code.getIfPresent(x)!=null)
//            return 0; //already in

//        Compound y = decode(x);
//        int xxl = y.volume();
//        if (xxl >= volume.lo() && xxl <= volume.hi()) {
        //return super.onOut(b);
        x = compoundOrNull((nar.concepts.productNormalize(x)).unneg());
        if (x != null) {
            abbreviate(x, b);
            return 1f;
        } else
            return 0; //rejected
    }

    @Override
    public void onRemoval(Compound key, Abbr value, RemovalCause cause) {
        ec.remove(value.encode);
        dc.remove(value.decode);
    }


    @Override
    protected void abbreviate(@NotNull Compound abbreviated, @NotNull Budget b) {
        final boolean[] changed = {false};

        Op ao = abbreviated.op();
        if (ao == Op.EQUI || ao == Op.IMPL) //HACK for equivalence relation
            return;

        abbreviated = decode(abbreviated);
        if (abbreviated.volume() > volume.hi())
            return; //expanded too much

        /** dont abbreviate PermanentConcept's themselves */
        if (nar.concept(abbreviated) instanceof PermanentConcept)
            return;

        Abbr abb = (Abbr) code.get(abbreviated);
        if (abb != null) {
            //boost it
            abb.priAdd(b.pri());
            return;
        }


        //System.out.println("compress CODE: " + a + " to " + compr);

        changed[0] = true;
        AliasConcept aa = AliasConcept.get(newSerialTerm(), abbreviated, nar);
        if (aa == null)
            return;

        abb = new Abbr(abbreviated  /** store fully decompress */, aa, nar);

        synchronized (code) {
            if (code.put(abb) == null) {
                return; //failed insert
            }
        }

        code.commit();

        Compound s = compoundOrNull(
                //$.sim
                $.equi
                        (abb.compressed, abb.get())
        );
        if (s == null) {
            logger.error("unrelateable: {}", abb);
        } else {


            Task abbreviationTask = new AbbreviationTask(
                    s, BELIEF, $.t(1f, abbreviationConfidence.floatValue()),
                    nar.time(), ETERNAL, ETERNAL,
                    new long[]{nar.time.nextStamp()}, abb.get(), abb.compressed
            );
            abbreviationTask.log("Abbreviate");
            abbreviationTask.setBudget(b);
            nar.input(abbreviationTask);

            recompile();
        }

    }



    final AtomicBoolean busy = new AtomicBoolean();

    private void recompile() {

        if (busy.compareAndSet(false, true)) {
            nar.runLater(() -> {
                if (busy.compareAndSet(true, false)) {
                    decoder = matcher(x -> x.decode);
                    encoder = matcher(x -> x.encode);
                }
            });
        }

    }


    private MultiSequenceMatcher matcher(Function<Abbr, SequenceMatcher> which) {

        Collection<SequenceMatcher> mm = HijackBag.stream(code.map.get()).filter(Objects::nonNull).map(which).collect(toList());
            //code.asMap().values().stream().map(which).collect(Collectors.toList());

        switch (mm.size()) {
            case 0:
                return null;

            case 1:
                return new ListMultiSequenceMatcher(mm);

            default:
                return new TrieMultiSequenceMatcher(smFactory, mm);

        }
    }

    @NotNull
    public Task encode(Task tt) {
        return transcode(tt, true);
    }

    @NotNull
    public Task decode(Task tt) {
        return transcode(tt, false);
    }

    public Task transcode(Task tt, boolean en) {

        Term i = tt.term();
        Term o = transcode(i, en);
        if (o != i) {

            try {
                //HACK wrap in product if atomic
//                if (!(o instanceof Compound))
//                    o = $.p(o);

                //System.out.println("  compress: " + i + " to " + o);

                if (o instanceof Compound) {
                    @Nullable Task rr = Task.clone(tt, (Compound) o);
                    if (rr != null)
                        return rr;
                }
            } catch (Throwable t) {
                //HACK ignore
            }
        }

        return tt;

    }

    @NotNull
    public Term transcode(@NotNull Term t, boolean en) {
        if (!(t instanceof Compound))
            return t;
        else {
            byte[] ii = IO.asBytes(t);
            byte[] oo = transcode(ii, en);
            return ii != oo ? compoundOr(IO.termFromBytes(oo, nar.concepts), (Compound) t) : t;
        }
    }

    @NotNull
    public byte[] transcodeBytes(Term t, boolean en) {
        byte[] ii = IO.asBytes(t);
        if (!(t instanceof Compound))
            return ii;
        else {
            return transcode(ii, en);
        }
    }

    @NotNull
    public Term encode(Term t) {
        return transcode(t, true);
    }

    @NotNull
    public Term decode(Term t) {
        return transcode(t, false);
    }

    @NotNull
    public Compound decode(Compound t) {
        return (Compound) transcode(t, false);
    }


    public byte[] transcode(byte[] b, boolean en) {

        MultiSequenceMatcher coder = en ? this.encoder : this.decoder;
        if (coder == null)
            return b;

        //byte[] bOriginal = b;


        int ii = 0;


        ByteArrayReader wr = null;
        final IntArrayList termPos = new IntArrayList();

        int numCodes = code.size();

        do {

            if (wr == null) {
                ii = 0;
                wr = new ByteArrayReader(b);
                termPos.clear();
                try {
                    IO.mapSubTerms(b, (o, depth, p) -> {
                        termPos.add(p);
                    });
                } catch (Exception e) {
                    //logger.error("{}", e);
                    return b;
                }
            }

            int i = termPos.get(ii++);

            try {

                /*Array*/
                SequenceMatcher m = coder.firstMatch(wr, i);
                if (m != null) {
                    //if (!mm.isEmpty()) {
                    //int mms = mm.size();

                    //for (int i1 = 0, mmSize = mms; i1 < mmSize; i1++) {
                    //SequenceMatcher m = mm.get(i1);
                    Abbr c = abbr(m, en);
                    if (c != null) {
                        c.priAdd(1f/ numCodes);

                        //substitute
                        final byte[] to = en ? c.encoded : c.decoded;
                        final byte[] from = en ? c.decoded : c.encoded;

                        final byte[] prev = b;
                        int change = to.length - from.length;
                        if (change < -b.length)
                            throw new ArrayIndexOutOfBoundsException();

                        final byte[] next = new byte[b.length + change];

                        System.arraycopy(prev, 0, next, 0, i);
                        System.arraycopy(to, 0, next, i, to.length);
                        int l = b.length;
                        if (i < l)
                            System.arraycopy(prev, i + from.length, next, i + to.length, l - (i + from.length));

                        wr = null;
                        b = next;
                        continue; //restart
                    }

                }

            } catch (IOException e) {
                //logger.error(" {}", e);
                return b;
            }

        } while (ii < termPos.size());


        return b;

    }

    private Abbr abbr(SequenceMatcher m, boolean en) {
        return (en ? this.ec : this.dc).get(m);
    }


    public static void main(String[] args) {
        Default n = new Default();
        Compressor c = new Compressor(n, "_", 4, 8, 0.5f, 8, 32);

        n.onTask(x -> {
            Task y = c.encode(x);
            if (!y.equals(x))
                System.out.println(y);
        });
        n.log();
        n.input("a:b. b(a,c, 1, 1, 2). c:(d,a). d:e.");
        n.run(800);


    }
}
