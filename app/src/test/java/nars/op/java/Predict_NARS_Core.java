package nars.op.java;

import com.gs.collections.impl.set.mutable.primitive.IntHashSet;
import nars.Global;
import nars.NAR;
import nars.Op;
import nars.nar.Default;
import nars.op.in.ChangedTextInput;
import nars.task.Task;
import nars.util.time.IntervalTree;

import java.util.List;

import static java.lang.System.out;

/**
 * https://github.com/encog/encog-java-examples/blob/master/src/main/java/org/encog/examples/neural/predict/sunspot/PredictSunspotElman.java
 * https://github.com/encog/encog-java-examples/blob/master/src/main/java/org/encog/examples/neural/recurrent/elman/ElmanXOR.java
 *
 * @author me
 */
public class Predict_NARS_Core {

    static float signal = 0;

    static IntervalTree<Long, Task> predictions = new IntervalTree<>();
    static double maxval = 0;
    private static int last = -1;

    public static void main(String[] args) {

        Global.DEBUG = false;
        int duration = 8;
        float freq = 1.0f / duration * 0.1f;
        int thinkInterval = 24;
        float discretization = 7f;

        NAR n = new Default(1000, 2, 4, 4);
        n.cyclesPerFrame.set(4);
        n.shortTermMemoryHistory.set(3);
        //n.param.duration.set(duration);
        //n.param.noiseLevel.set(0);
        //n.param.conceptForgetDurations.set(16);

        n.eventTaskProcess.on(t -> {
            if (!t.isDeleted() && t.isBelief() && t.op()== Op.PRODUCT && t.term().volume()==2 &&  !t.isEternal() && t.occurrence() > n.time() && t.expectation()>0.5) {

                long time = (int) t.occurrence();
                String ts = t.term().toString();
                if (ts.startsWith("(y")) {
                    predict(time, t);
                }
            }

        });


//        IntervalTree<Long,Float> observed = new //new TreeMLData("value", Color.WHITE).setRange(0, 1f);
//        predictions = new TreeMLData[(int)discretization];
//        TreeMLData[] reflections = new TreeMLData[(int)discretization];
//
//        for (int i = 0; i < predictions.length; i++) {
//            predictions[i] = new TreeMLData("Pred" + i,
//                    Color.getHSBColor(0.25f + i / 4f, 0.85f, 0.85f));
//
//            reflections[i] = new TreeMLData("Refl" + i,
//                    Color.getHSBColor(0.25f + i / 4f, 0.85f, 0.85f));
//            reflections[i].setDefaultValue(0.0);
//        }
//        TimelineVis tc = new TimelineVis(
//                new LineChart(0,1,observed).thickness(16f).height(128),
//                new LineChart(predictions[0]).thickness(16f).height(128)
//                //new BarChart(reflections).thickness(16f).height(128)
//                /*new LineChart(predictions[1]).thickness(16f).height(128),
//                new LineChart(predictions[2]).thickness(16f).height(128),*/
//        );

        //new NWindow("_", new PCanvas(tc)).show(800, 800, true);

        n.log();
        n.run((int) discretization * 4);


        ChangedTextInput chg = new ChangedTextInput(n);
        double lastsignal = 0;
        double lasttime = 0;

        while (true) {

            try {
                n.run(thinkInterval);
            }
            catch (Exception e) {
                System.err.println(e);
                n.stop();
                //e.printStackTrace();
            }
            //Util.pause(30);

            //signal  = (float)Math.max(0, Math.min(1.0, Math.tan(freq * n.time()) * 0.5f + 0.5f));
            signal = (float) Math.sin(freq * n.time()) * 0.5f + 0.5f;
            //signal = ((float) Math.sin(freq * n.time()) > 0 ? 1f : -1f) * 0.5f + 0.5f;
            //signal *= 1.0 + (Math.random()-0.5f)* 2f * noiseRate;

            int cols = 40;
            int colActual = (int) Math.round(signal * cols);
            int val = (int) (((int) ((signal * discretization)) * (10.0 / discretization)));

            long windowStart = n.time();
            long windowEnd = n.time() + 2;

            predictions.removeContainedBy(0L, windowStart);

            List<Task> pp = predictions.searchContainedBy(windowStart, windowEnd);
            IntHashSet pi = new IntHashSet();
            for (Task tf : pp) {
                if (tf.isDeleted())
                    continue;

                char cc = tf.term().toString().charAt("(y".length());
                int f = cc - '0';
                //if(time>=curmax) {
                //  curmax=time;
                //}
                //maxval=Math.max(maxval, (value)/10.0f);

                pi.add(Math.round((f / discretization) * cols));
                if (Math.abs(f-val) > 1) {
                    System.err.println(f + " vs actual " + val );
                    System.err.println(tf.explanation());
                } else {
                    System.out.println("OK: " + tf);
                    System.out.println(tf.explanation());
                }
            }

            for (int i = 0; i <= cols; i++) {


                char c;

                if (pi.contains(i))
                    c = 'X';
                else if (i == colActual)
                    c = '#';
                else
                    c = '.';

                out.print(c);

            }

            out.println();

//            observed.removeData((int) (lasttime+1));  //this
//            observed.removeData((int) (lasttime+2));  //is not good practice
//            observed.add((int) n.time(), signal);
//            observed.add((int) n.time()+1, -1); //but is fine
//            observed.add((int) n.time()+2, 1); //for now (just wanted a line at the end)

            lastsignal = signal;
            lasttime = n.time();
//            predictions[0].setData(0, maxval);
            //if(cnt<1000) { //switch to see what NARS does when observations end :)
            int dt = 1;
            /*if (last!=val && chg.set("((y" + val + ") &&-" + dt + " (--,(y" + last + "))). :|:")) {
                last = val;
            }*/
//            if (last!=val && chg.set("((y" + val + ") &&-" + dt + " (--,(y" + last + "))). :|:")) {
//                last = val;
//            }
            if (chg.set("(y" + val + "). :|:")) {
                if (last != -1) {
                    n.input("(y" + last + "). :|: %0.00;0.90%");
                }
                last = val;
            }

            //System.out.println(val);
            /*} else if (cnt==1000){
                System.out.println("observation phase end, residual predictions follow");
            }*/

            //n.ask(n.term("(?X)"), '?', n.time() + thinkInterval);
            n.ask(n.term("(?X)"), '?', n.time() + thinkInterval / 2);
            //n.ask(n.term("(?X)"), '?', n.time() + thinkInterval / 4);

        }

    }

    private static void predict(long time, Task v) {
        //System.out.println("predict; " + time + " " + v);
        predictions.put(time, v);
    }
}