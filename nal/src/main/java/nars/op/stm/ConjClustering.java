package nars.op.stm;

import jcog.Util;
import jcog.list.ArrayIterator;
import jcog.list.FasterList;
import jcog.pri.VLink;
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
import org.eclipse.collections.impl.tuple.primitive.PrimitiveTuples;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConjClustering extends Causable {
    private final CauseChannel<ITask> in;


    final static BagClustering.Dimensionalize<Task> ConjClusterModel = new BagClustering.Dimensionalize<Task>(3) {

        @Override
        public void coord(Task t, double[] c) {
            c[0] = t.start();
            c[1] = t.truth().isNegative() ? (1f - t.freq()) : t.freq(); //0..+1 //if negative, will be negated in subterms
            c[2] = t.conf(); //0..+1
        }

        @Override
        public double distanceSq(double[] a, double[] b) {
            return Util.sqr(
                    Math.abs(a[0] - b[0])
            ) +    //time

                    (Math.abs(a[1] - b[1]))  //freq
                    + (Math.abs(a[2] - b[2])  //conf

            );

        }
    };
    private final BagClustering<Task> bag;
    private final byte punc;
    private final int maxConjSize;
    private long now;
    private float truthRes, confMin;
    private int volMax;

    public ConjClustering(NAR nar, int maxConjSize, byte punc, int centroids, int capacity) {
        super(nar);

        this.punc = punc;
        this.in = nar.newCauseChannel(this);
        this.bag = new BagClustering<>(ConjClusterModel, centroids, capacity);
        this.maxConjSize = maxConjSize;

        nar.onTask(t -> {
            if (!t.isEternal() && t.punc() == punc) {
                bag.put(t, t.priElseZero());
            }
        });
    }


    @Override
    protected int next(NAR nar, int iterations) {
        int created = 0;

        now = nar.time();
        truthRes = nar.truthResolution.floatValue();
        confMin = nar.confMin.floatValue();
        this.volMax = nar.termVolumeMax.intValue();

        bag.commit(1, (sorted) -> {
            List<Task> batch = $.newArrayList();
            int current = -1;
            int n = sorted.size();
            int bs = -1;
            for (int i = 0; i < n; i++) {
                VLink<Task> x = sorted.get(i);
                if (current != x.centroid) {
                    current = x.centroid;
                    if (bs != -1) {
                        if (i - bs > 1)
                            conjoin(nar, sorted, bs, i);
                    }
                    bs = i;
                }
            }
        });
        return created;
    }

    /**
     * produces a parallel conjunction term consisting of all the task's terms
     */
    public Stream<List<Task>> chunk(Stream<Task> input, int maxComponentsPerTerm, int maxVolume) {
        final int[] group = {0};
        final int[] subterms = {0};
        final int[] currentVolume = {0};
        return input.
                filter(x -> !x.isDeleted())
                .collect(Collectors.groupingBy(x -> {

                    int v = x.volume();

                    if ((subterms[0] >= maxComponentsPerTerm) || (currentVolume[0] + v >= maxVolume)) {
                        //next group
                        group[0]++;
                        subterms[0] = 1;
                        currentVolume[0] = v;
                    } else {

                        subterms[0]++;
                        currentVolume[0] += v;
                    }

                    return group[0];
                }))
                .entrySet().stream()
                .filter(c -> c.getKey() >= 0 && c.getValue().size() > 1) //only batches of >1
                .map(Map.Entry::getValue); //ignore the -1 discard group

    }

    private void conjoin(NAR nar, List<VLink<Task>> sorted, int is, int ie) {

        //get only the maximum confidence task for each term
        Map<Term, Task> vv = new HashMap();

        final long[] end = {Long.MIN_VALUE};
        final long[] start = {Long.MAX_VALUE};


        chunk(sorted.subList(is, ie).stream().map(x -> x.id), maxConjSize, volMax).forEach(subs -> {
            //
//                    //if temporal clustering is close enough, allow up to maxGroupSize in &&, otherwise lmiit to 2
//
//


            subs.forEach(z -> {


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
                return;

            Task[] uu = vv.values().toArray(new Task[vs]);

            @Nullable Term conj = conj(uu);
            if (conj == null)
                return;

            @Nullable ObjectBooleanPair<Term> cp = Task.tryContent(conj, punc, true);
            if (cp != null) {


                float conf = TruthFunctions.confAnd(uu); //used for emulation of 'intersection' truth function
                PreciseTruth t = $.t(1, conf).negIf(cp.getTwo()).ditherFreqConf(truthRes, confMin, 1f);
                if (t != null) {

                    int uuLen = uu.length;
                    long[] evidence = Stamp.zip(() -> new ArrayIterator<>(uu), uuLen); //HACK
                    NALTask m = new STMClusterTask(cp, t, start, end, evidence, punc, now); //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined

                    for (Task u : uu)
                        m.causeMerge(u); //cause merge

                    float maxPri = new FasterList<>(uuLen, uu)
                            .maxValue(Task::priElseZero) / uuLen; //HACK todo dont use List

                    m.setPri(BudgetFunctions.fund(maxPri, false, uu));
                    in.input(m);
                }

            }

        });

    }

    @Override
    public float value() {
        return in.amp();
    }


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

//
//                    //if temporal clustering is close enough, allow up to maxGroupSize in &&, otherwise lmiit to 2
//                    int gSize = ((n.range(0) <= dur && n.range(1) <= dur)) ? maxConjSize : 2;
//
//                    return n.chunk(gSize, maxVol).

    //
    @Nullable
    private static Term conj(Task[] uu) {

        return
                Op.conj(
                        new FasterList<ObjectLongPair<Term>>(
                                Util.map(t -> PrimitiveTuples.pair(
                                        t.term().negIf(t.truth().isNegative()),
                                        t.start()), ObjectLongPair[]::new,
                                        uu))
                );


    }

    public static class STMClusterTask extends NALTask {
        public STMClusterTask(@Nullable ObjectBooleanPair<Term> cp, PreciseTruth t, long[] start, long[] end, long[] evidence, byte punc, long now) throws InvalidTaskException {
            super(cp.getOne(), punc, t, now, start[0], end[0], evidence);
        }

        @Override
        public boolean isInput() {
            return true;
        }
    }


}
