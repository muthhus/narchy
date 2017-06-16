package nars.op.stm;

import jcog.Util;
import jcog.data.MutableInteger;
import jcog.list.ArrayIterator;
import jcog.list.FasterList;
import jcog.pri.mix.PSink;
import nars.$;
import nars.NAR;
import nars.Task;
import nars.budget.BudgetFunctions;
import nars.index.term.TermIndex;
import nars.task.GeneratedTask;
import nars.task.ITask;
import nars.term.Compound;
import nars.term.Term;
import nars.truth.Stamp;
import nars.truth.TruthFunctions;
import org.eclipse.collections.api.tuple.primitive.ObjectBooleanPair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static nars.Op.CONJ;
import static nars.term.Terms.normalizedOrNull;

/**
 * Task Dimension Mapping:
 *  0: Start time
 *  1: End time
 *  2: Freq
 *  3: Conf (grouping by confidence preserves the maximum collective confidence of any group, which is multiplied in conjunction truth)
 */
public class MySTMClustered extends STMClustered {

    private static final Logger logger = LoggerFactory.getLogger(MySTMClustered.class);

    private final int maxGroupSize;
    private final int minGroupSize;
    private final int inputsPerDur;
    private final PSink<Object, ITask> in;

    float freqCoherenceThresh = 0.9f;
    float confCoherenceThresh = 0.5f;

    float confMin;

    long lastIteration;
    private int dur;

    public MySTMClustered(@NotNull NAR nar, int size, byte punc, int maxGroupSize, boolean allowNonInput, float drainRatePerDuration) {
        this(nar, size, punc, maxGroupSize, allowNonInput, (int) Math.ceil((float)size/maxGroupSize * drainRatePerDuration));
    }

    public MySTMClustered(@NotNull NAR nar, int size, byte punc, int maxGroupSize, boolean allowNonInput, int inputsPerDuration) {
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

        this.in = nar.in.stream(this);
        net.setAlpha(0.05f);
        //net.setBeta(0.05f);
        net.setWinnerUpdateRate(0.03f, 0.01f);
    }

    @Override
    @NotNull
    public double[] getCoord(@NotNull Task t) {
        double[] c = new double[dims];
        c[0] = t.start();
        c[1] = t.end();
        c[2] = t.freq(); //0..+1
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
        TasksNode t = new TasksNode(id);
        t.randomizeUniform(0, -dur*2, +dur*2);
        t.randomizeUniform(1, -dur*2, +dur*2);
        t.randomizeUniform(2, 0f, 1f);
        t.randomizeUniform(3, 0f, 1f);
        return t;
    }

    @Override
    protected boolean iterate() {

        if (super.iterate()) {

            confMin = nar.confMin.floatValue();
            dur = nar.dur();

            //LongObjectHashMap<ObjectFloatPair<TasksNode>> selected = new LongObjectHashMap<>();

            //clusters where all terms occurr simultaneously at precisely the same time
            //cluster(maxConjunctionSize, 1.0f, freqCoherenceThresh);

            long now = nar.time();
            float deltaT = now - lastIteration;
            lastIteration = now;

            int inputs = Math.round(inputsPerDur * deltaT / dur);
            if (inputs > 0) {
                cluster(inputs, minGroupSize, maxGroupSize);
            }

            //clusters where dt is allowed, but these must be of length 2. process any of these pairs which remain
            //if (maxGroupSize != 2)
            //cluster(2);

            return true;
        }

        return false;
    }

    private void cluster(int limit, int minGroupSize, int maxGroupSize) {

        Map<Term, Task> vv = new HashMap();

        net.nodeStream()
                .filter(x -> x.size() >= minGroupSize)
                .sorted(Comparator.comparingDouble(x -> x.localError() / (x.size() )))
                .filter(n -> {

                    //System.out.println(n.localError() + " " + n.size() + " " + n.toString());

                    if (n == null || n.size() < minGroupSize)
                        return false;

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

                    boolean negated;
                    if (freq < 0.5f) {
                        freq = 1f - freq;
                        negated = true;
                    } else {
                        negated = false;
                    }

                    float finalFreq = freq;
                    int maxVol = nar.termVolumeMax.intValue();

                    //if temporal clustering is close enough, allow up to maxGroupSize in &&, otherwise lmiit to 2
                    int gSize = ((n.range(0) <= dur && n.range(1) <= dur)) ? maxGroupSize : 2;

                    return n.chunk(gSize, maxVol - 1).map(tt -> {

                        //Task[] uu = Stream.of(tt).filter(t -> t!=null).toArray(Task[]::new);

                        //get only the maximum confidence task for each term
                        final long[] start = {Long.MAX_VALUE};
                        final long[] end = {Long.MIN_VALUE};

                        vv.clear();
                        tt.forEach(_z -> {
                            Task z = _z.get();
                            //if (z != null) {
                            long zs = z.start();
                            long ze = z.end();
                            if (start[0] > zs) start[0] = zs;
                            if (end[0] < ze) end[0] = ze;

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

                        //float confMin = (float) Stream.of(uu).mapToDouble(Task::conf).min().getAsDouble();
                        float conf = TruthFunctions.confAnd(uu); //used for emulation of 'intersection' truth function
                        if (conf < confMin)
                            return null;


                        @Nullable Compound conj = group(negated, uu);
                        if (conj == null)
                            return null;

                        @Nullable ObjectBooleanPair<Compound> cp = Task.tryContent(conj, punc, nar.terms);
                        if (cp!=null) {
                            int uuLen = uu.length;
                            long[] evidence = Stamp.zip(()->new ArrayIterator<Stamp>(uu), uuLen); //HACK

                            Task m = new GeneratedTask(cp.getOne(), punc,
                                    $.t(finalFreq, conf).negIf(cp.getTwo()), now, start[0], end[0], evidence); //TODO use a truth calculated specific to this fixed-size batch, not all the tasks combined

                            float maxPri = new FasterList<Task>(uuLen, uu)
                                                .maxValue(Task::priElseZero) / uuLen; //HACK todo dont use List

                            m.priority().setPri( BudgetFunctions.fund( maxPri, false, uu ) );
                            return m;

                        }

                        return null;

//                        float priTotal = (float)(uu.stream().mapToDouble(x -> x.pri()).sum());
//                        float priAvg = ((float)(priTotal / uu.size()));
//

//        if (srcCopy == null) {
//            delete();
//        } else {
//            float p = srcCopy.priSafe(-1);
//            if (p < 0) {
//                delete();
//            } else {
//                setPriority(p);
//            }
//        }
//
//        return this;
                        //m.log("STMCluster CoOccurr");


                        //logger.debug("{}", m);
                        //generate.emit(m);

                        //System.err.println(m + " " + Arrays.toString(m.evidence()));

                        //node.tasks.removeAll(tt);


                    }).filter(Objects::nonNull);

                }).limit(limit).forEach(in::input);


    }

    @Nullable
    private Compound group(boolean negated, @NotNull Task[] uu) {


        TermIndex index = nar.terms;
        if (uu.length == 2) {
            //find the dt and construct a sequence
            Task early, late;

            Task u0 = uu[0];
            Task u1 = uu[1];
            if (u0.start() <= u1.start()) {
                early = u0;
                late = u1;
            } else {
                early = u1;
                late = u0;
            }
            int dt = (int) (late.start() - early.start());


            return normalizedOrNull(
                    index.the(CONJ, dt, $.negIf(early.term(), negated),
                            $.negIf(late.term(), negated)), index);

        } else {

            Term[] u = Util.map((tx) -> $.negIf(tx.term(), negated), new Term[uu.length], uu);

            //just assume they occurr simultaneously
            return normalizedOrNull( index.the(CONJ, 0, u ), index);
        }
    }
}
