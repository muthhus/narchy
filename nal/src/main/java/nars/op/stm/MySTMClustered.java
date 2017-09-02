package nars.op.stm;

import jcog.Util;
import jcog.data.MutableInteger;
import jcog.list.ArrayIterator;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.control.CauseChannel;
import nars.task.NALTask;
import nars.task.util.InvalidTaskException;
import nars.term.Term;
import nars.truth.PreciseTruth;
import nars.truth.Stamp;
import nars.truth.TruthFunctions;
import nars.util.BudgetFunctions;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.eclipse.collections.api.tuple.primitive.ObjectLongPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples.pair;

/**
 * Task Dimension Mapping:
 * 0: Start time
 * 1: End time
 * 2: Freq
 * 3: Conf (grouping by confidence preserves the maximum collective confidence of any group, which is multiplied in conjunction truth)
 */
public class MySTMClustered extends STMClustered {

    //private static final Logger logger = LoggerFactory.getLogger(MySTMClustered.class);

    private final int maxGroupSize;
    private final int minGroupSize;
    private final int inputsPerDur;
    private final CauseChannel<Task> in;
    private final boolean allowNonInput;

    float freqCoherenceThresh = 0.9f;
    float confCoherenceThresh = 0.5f;

    float confMin;

    long lastIteration;
    private int dur;
    private float truthRes;

    public MySTMClustered(@NotNull NAR nar, int size, byte punc, int maxGroupSize, boolean allowNonInput, float drainRatePerDuration) {
        this(nar, size, punc, maxGroupSize, allowNonInput, (int) Math.ceil((float) size / maxGroupSize * drainRatePerDuration));
    }

    MySTMClustered(@NotNull NAR nar, int size, byte punc, int maxGroupSize, boolean allowNonInput, int inputsPerDuration) {
        this(nar, size, punc, 2, maxGroupSize,
                //Math.round(((float) nar.termVolumeMax.intValue()) / (2)) /* estimate */
                allowNonInput,
                inputsPerDuration);
    }

    MySTMClustered(@NotNull NAR nar, int size, byte punc, int minGroupSize, int maxGroupSize, boolean allowNonInput, int inputsPerDur) {
        super(4, nar, new MutableInteger(size), punc, (int) Math.round(Math.sqrt(size)) /* estimate */);

        this.minGroupSize = minGroupSize;
        this.maxGroupSize = maxGroupSize;

        this.inputsPerDur = inputsPerDur;
        //this.logger = LoggerFactory.getLogger(toString());

        this.allowNonInput = allowNonInput;

        lastIteration = nar.time();

        this.in = nar.newCauseChannel(this);
        net.setAlpha(0.05f);
        //net.setBeta(0.05f);
        net.setWinnerUpdateRate(0.03f, 0.01f);
    }

    @Override
    public void accept(NAR nar, @NotNull Task t) {
        if (STMLinkage.stmLinkable(t, allowNonInput) && (t.punc() == punc && !t.isEternal()))
            super.accept(nar, t);
    }

    @Override
    @NotNull
    public double[] coord(@NotNull Task t) {
        double[] c = new double[dims];
        c[0] = t.start();
        c[1] = t.end();
        c[2] = t.truth().isNegative() ? (1f - t.freq()) : t.freq(); //0..+1 //if negative, will be negated in subterms
        c[3] = t.conf(); //0..+1
        return c;
    }

    @Override
    protected double[] filter(@NotNull double[] coord) {
        double[] d = coord.clone();
        d[0] -= now;
        d[1] -= now;
        return d;
    }

    @Override
    protected TasksNode newCentroid(int id) {
        TasksNode t = new TasksNode(id, capacity.intValue() / clusters);
        t.randomizeUniform(0, dur * -2, dur * +2);
        t.randomizeUniform(1, dur * -2, dur * +2);
        t.randomizeUniform(2, 0f, 1f);
        t.randomizeUniform(3, 0f, 1f);
        return t;
    }

    @Override
    protected boolean iterate(NAR nar) {

        if (super.iterate(nar)) {

            confMin = nar.confMin.floatValue();
            truthRes = nar.truthResolution.floatValue();
            dur = nar.dur();

            //LongObjectHashMap<ObjectFloatPair<TasksNode>> selected = new LongObjectHashMap<>();

            //clusters where all terms occurr simultaneously at precisely the same time
            //cluster(maxConjunctionSize, 1.0f, freqCoherenceThresh);

            long now = nar.time();
            float deltaT = now - lastIteration;
            lastIteration = now;

            int inputs = Math.round(in.gain() * 0.5f /* swing: 0.5..+1 */ * inputsPerDur * deltaT / dur);
            if (inputs > 0) {
                cluster(inputs, minGroupSize, maxGroupSize, nar);
            }

            //clusters where dt is allowed, but these must be of length 2. process any of these pairs which remain
            //if (maxGroupSize != 2)
            //cluster(2);

            return true;
        }

        return false;
    }

    private void cluster(int limit, int minGroupSize, int maxGroupSize, NAR nar) {

        Map<Term, Task> vv = new HashMap();

        net.nodeStream()
                .filter(x -> x.size() >= minGroupSize)
                .sorted(Comparator.comparingDouble(x -> x.localError() / (x.size())))
                .filter(n -> {


                    //TODO wrap all the coherence tests in one function call which the node can handle in a synchronized way because the results could change in between each of the sub-tests:


                    double[] fc = n.coherence(2);
                    if (fc != null && fc[1] >= freqCoherenceThresh) {
                        double[] cc = n.coherence(3);
                        if (cc != null && cc[1] >= confCoherenceThresh) {
                            return true;
                        }
                        //return true;
                    }

                    return false;
                })
                .flatMap(n -> {

                    @Nullable double[] freqDim = n.coherence(2);
                    if (freqDim == null)
                        return null;


                    float freq = (float) freqDim[0];

//                    boolean negated = false;
//                    if (freq < 0.5f) {
//                        freq = 1f - freq;
//                        negated = true;
//                    } else {
//                        negated = false;
//                    }

                    float finalFreq = freq;
                    int maxVol = nar.termVolumeMax.intValue();

                    //if temporal clustering is close enough, allow up to maxGroupSize in &&, otherwise lmiit to 2
                    int gSize = ((n.range(0) <= dur && n.range(1) <= dur)) ? maxGroupSize : 2;

                    return n.chunk(gSize, maxVol - 1).map(tt -> {

                        //Task[] uu = Stream.of(tt).filter(t -> t!=null).toArray(Task[]::new);

                        //get only the maximum confidence task for each term

                        vv.clear();
                        final long[] end = {Long.MIN_VALUE};
                        final long[] start = {Long.MAX_VALUE};
                        tt.forEach(_z -> {
                            Task z = _z.get();
                            if (z == null)
                                return;
                            long zs = z.start();
                            long ze = z.end();
                            if (start[0] > zs) start[0] = zs;
                            if (end[0] < ze) end[0] = ze;
                            if (end[0] < start[0])
                                throw new RuntimeException("wtf");

                            vv.merge(z.term(), z, (prevZ, newZ) -> {
                                if (prevZ == null || newZ.conf() > prevZ.conf())
                                    return newZ;
                                else
                                    return prevZ;
                            });
                        });

                        int vs = vv.size();
                        if (vs < 2)
                            return null;

                        Task[] uu = vv.values().toArray(new Task[vs]);

                        @Nullable Term conj = conj(uu);
                        if (conj == null)
                            return null;

                        @Nullable ObjectBooleanPair<Term> cp = Task.tryContent(conj, punc, true);
                        if (cp != null) {



                            float conf = TruthFunctions.confAnd(uu); //used for emulation of 'intersection' truth function
                            PreciseTruth t = $.t(finalFreq, conf).negIf(cp.getTwo()).ditherFreqConf(truthRes, confMin, 1f);
                            if (t!=null) {

                                int uuLen = uu.length;
                                long[] evidence = Stamp.zip(() -> new ArrayIterator<>(uu), uuLen); //HACK
                                NALTask m = new STMClusterTask(cp, t, start, end, evidence, punc, now); //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined

                                for (Task u : uu)
                                    m.causeMerge(u); //cause merge

                                float maxPri = new FasterList<>(uuLen, uu)
                                        .maxValue(Task::priElseZero) / uuLen; //HACK todo dont use List

                                m.setPri(BudgetFunctions.fund(maxPri, false, uu));
                                return m;
                            }

                        }

                        return null;


                    }).filter(Objects::nonNull);

                }).limit(limit).forEach(in::input);


    }

    @Nullable
    private static Term conj(@NotNull Task[] uu) {

        return
                Op.conj(
                        new FasterList<>(Util.map(t -> pair(
                                t.term().negIf(t.truth().isNegative()),
                                t.start()), new ObjectLongPair[uu.length], uu))
                );


    }

    private static class STMClusterTask extends NALTask {
        public STMClusterTask(@Nullable ObjectBooleanPair<Term> cp, PreciseTruth t, long[] start, long[] end, long[] evidence, byte punc, long now) throws InvalidTaskException {
            super(cp.getOne(), punc, t, now, start[0], end[0], evidence);
        }

        @Override
        public boolean isInput() {
            return true;
        }
    }
}
