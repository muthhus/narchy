package nars.op.stm;

import jcog.Util;
import jcog.learn.gng.impl.Centroid;
import jcog.list.ArrayIterator;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.Task;
import nars.bag.BagClustering;
import nars.control.Causable;
import nars.control.CauseChannel;
import nars.task.ITask;
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
public class ClusterJunction extends Causable {

    //private static final Logger logger = LoggerFactory.getLogger(MySTMClustered.class);
    final static int NEW_CENTROID_DUR_SCAN_RADIUS = 1;

    public final BagClustering<Task> bag;

    private final CauseChannel<ITask> in;
    private final boolean allowNonInput;
    private final byte punc;

    float confMin;

    long lastIteration;
    private int dur;
    private float truthRes;

    final BagClustering.Dimensionalize<Task> STMClusterModel = new BagClustering.Dimensionalize<Task>(4) {

        @Override
        public void coord(Task t, double[] c) {
            c[0] = t.start();
            c[1] = t.end();
            c[2] = t.truth().isNegative() ? (1f - t.freq()) : t.freq(); //0..+1 //if negative, will be negated in subterms
            c[3] = t.conf(); //0..+1
        }

    };

    @Override
    public boolean singleton() {
        return true;
    }

    private int minConjSize, maxConjSize;

    public ClusterJunction(@NotNull NAR nar, boolean allowNonInput, byte punc, int minConjSize, int maxConjSize, int centroids, int capacity) {
        super(nar);

        this.minConjSize = minConjSize;
        this.maxConjSize = maxConjSize;

        bag = new BagClustering<>(STMClusterModel, centroids, capacity);

        this.punc = punc;
        this.allowNonInput = allowNonInput;

        this.lastIteration = nar.time();

        this.in = nar.newCauseChannel(this);

        nar.onTask((t) -> accept(nar, t));
    }


    public void accept(NAR nar, @NotNull Task t) {
        if (STMLinkage.stmLinkable(t, allowNonInput) && (t.punc() == punc && !t.isEternal())) {
            bag.put(t,
                t.priElseZero() + t.conf()
                //t.priElseZero()
                //t.conf()
                //t.conf() * t.priElseZero()
            );
        }
    }


    @Override
    protected int next(NAR nar, int work) {

        confMin = nar.confMin.floatValue();
        truthRes = nar.truthResolution.floatValue();
        dur = nar.dur();

        //LongObjectHashMap<ObjectFloatPair<TasksNode>> selected = new LongObjectHashMap<>();

        //clusters where all terms occurr simultaneously at precisely the same time
        //cluster(maxConjunctionSize, 1.0f, freqCoherenceThresh);

        long now = nar.time();
        float deltaT = now - lastIteration;
        lastIteration = now;


        //int maxVol = nar.termVolumeMax.intValue() - 2;

        bag.commit(1);

        return work;

//        Map<Term, Task> vv = new HashMap();
//        net.nodeStream()
//                .filter(x -> x.size() >= minConjSize)
//                .sorted(Comparator.comparingDouble(x -> x.localError() / (x.size())))
//                .filter(n -> {
//                    //TODO wrap all the coherence tests in one function call which the node can handle in a synchronized way because the results could change in between each of the sub-tests:
//
//
//                    double[] fc = n.coherence(2);
//                    if (fc != null && fc[1] >= freqCoherenceThresh) {
//                        double[] cc = n.coherence(3);
//                        if (cc != null && cc[1] >= confCoherenceThresh) {
//                            return true;
//                        }
//                        //return true;
//                    }
//
//                    return false;
//                })
//                .flatMap(n -> {
//
//                    @Nullable double[] freqDim = n.coherence(2);
//                    if (freqDim == null)
//                        return null;
//
//
//                    float freq = (float) freqDim[0];
//
////                    boolean negated = false;
////                    if (freq < 0.5f) {
////                        freq = 1f - freq;
////                        negated = true;
////                    } else {
////                        negated = false;
////                    }
//
//                    float finalFreq = freq;
//
//                    //if temporal clustering is close enough, allow up to maxGroupSize in &&, otherwise lmiit to 2
//                    int gSize = ((n.range(0) <= dur && n.range(1) <= dur)) ? maxConjSize : 2;
//
//                    return n.chunk(gSize, maxVol).map(tt -> {
//
//                        //Task[] uu = Stream.of(tt).filter(t -> t!=null).toArray(Task[]::new);
//
//                        //get only the maximum confidence task for each term
//
//                        vv.clear();
//                        final long[] end = {Long.MIN_VALUE};
//                        final long[] start = {Long.MAX_VALUE};
//                        tt.forEach(_z -> {
//                            Task z = _z.get();
//
//                            long zs = z.start();
//                            long ze = z.end();
//                            if (start[0] > zs) start[0] = zs;
//                            if (end[0] < ze) end[0] = ze;
//                            if (end[0] < start[0])
//                                throw new RuntimeException("wtf");
//
//                            vv.merge(z.term(), z, (prevZ, newZ) -> {
//                                if (prevZ == null || newZ.conf() > prevZ.conf())
//                                    return newZ;
//                                else
//                                    return prevZ;
//                            });
//                        });
//
//                        int vs = vv.size();
//                        if (vs < 2)
//                            return null;
//
//                        Task[] uu = vv.values().toArray(new Task[vs]);
//
//                        @Nullable Term conj = conj(uu);
//                        if (conj == null)
//                            return null;
//
//                        @Nullable ObjectBooleanPair<Term> cp = Task.tryContent(conj, punc, true);
//                        if (cp != null) {
//
//
//                            float conf = TruthFunctions.confAnd(uu); //used for emulation of 'intersection' truth function
//                            PreciseTruth t = $.t(finalFreq, conf).negIf(cp.getTwo()).ditherFreqConf(truthRes, confMin, 1f);
//                            if (t != null) {
//
//                                int uuLen = uu.length;
//                                long[] evidence = Stamp.zip(() -> new ArrayIterator<>(uu), uuLen); //HACK
//                                NALTask m = new STMClusterTask(cp, t, start, end, evidence, punc, now); //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined
//
//                                for (Task u : uu)
//                                    m.causeMerge(u); //cause merge
//
//                                float maxPri = new FasterList<>(uuLen, uu)
//                                        .maxValue(Task::priElseZero) / uuLen; //HACK todo dont use List
//
//                                m.setPri(BudgetFunctions.fund(maxPri, false, uu));
//                                return m;
//                            }
//
//                        }
//
//                        return null;
//
//
//                    }).filter(Objects::nonNull);
//
//                }).limit(work).forEach(in::input);


    }

    @Override
    public float value() {
        return in.amp();
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
