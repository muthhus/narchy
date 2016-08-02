package spacegraph.video;

import com.gs.collections.impl.list.mutable.primitive.FloatArrayList;
import com.jogamp.opengl.GL2;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import nars.util.event.DefaultTopic;
import nars.util.event.On;
import nars.util.event.Topic;
import nars.util.signal.OneDHaar;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.GridSurface;
import spacegraph.obj.Plot2D;
import spacegraph.obj.RectWidget;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static spacegraph.obj.GridSurface.VERTICAL;

/**
 * Created by me on 10/28/15.
 */
public class WaveCapture implements Runnable {


    private final Plot2D.Series rawWave, wavelet1d;

    private int bufferSamples;

    ScheduledThreadPoolExecutor exec;
    private float[] samples;

    private WaveSource source;

    /**
     * called when next sample buffer is ready
     */
    final Topic<WaveCapture> nextReady = new DefaultTopic();

    synchronized void start(float FRAME_RATE) {
        if (exec != null) {
            exec.shutdownNow();
            exec = null;
        }

        if (FRAME_RATE > 0) {
            exec = new ScheduledThreadPoolExecutor(1);
            exec.scheduleAtFixedRate(this, 0, (long) (1000.0f / FRAME_RATE),
                    TimeUnit.MILLISECONDS);

        }
    }

    public void stop() {
        start(0);
    }


    public Surface newMonitorPane() {

        Plot2D audioPlot = new Plot2D(bufferSamples, Plot2D.Line);//, bufferSamples, 450, 60);
        audioPlot.add(rawWave);
        Plot2D audioPlot2 = new Plot2D(bufferSamples, Plot2D.Line);
        audioPlot2.add(wavelet1d);




        GridSurface v = new GridSurface(VERTICAL,
                audioPlot,
                audioPlot2) {
            @Override
            protected void paint(GL2 gl) {
                audioPlot.update();
                audioPlot2.update();
                super.paint(gl);
            }
        };

//
//        //noinspection OverlyComplexAnonymousInnerClass
//        ChangeListener onParentChange = new ChangeListener() {
//
//            public On observe;
//
//            @Override
//            public void changed(ObservableValue observableValue, Object o, Object t1) {
//
//                if (t1 == null) {
//                    if (observe != null) {
//                        //System.out.println("stopping view");
//                        this.observe.off();
//                        this.observe = null;
//                    }
//                } else {
//                    if (observe == null) {
//                        //System.out.println("starting view");
//                        observe = nextReady.on(u);
//                    }
//                }
//            }
//        };

        return v;
    }

    public WaveCapture(WaveSource source, float updateFrameRate) {

        setSource(source);


        //                double nextDouble[] = new double[1];
        //                DoubleSupplier waveSupplier = () -> {
        //                    return nextDouble[0];
        //                };


        rawWave = new Plot2D.Series("Audio", 1) {

            @Override
            public void update() {
                clear();

                float[] samples = WaveCapture.this.samples;
                if (samples == null) return;
                //samples[0] = null;

                addAll(samples);

                //minValue = -0.5f;
                //maxValue = 0.5f;

                autorange();

//                final FloatArrayList history = this.history;
//
//                for (int i = 0; i < nSamplesRead; i++) {
//                    history.add((float) samples[i]);
//                }
//
//                while (history.size() > maxHistory)
//                    history.removeAtIndex(0);

//                                        minValue = Float.POSITIVE_INFINITY;
//                                        maxValue = Float.NEGATIVE_INFINITY;
//
//                                        history.forEach(v -> {
//                                            if (Double.isFinite(v)) {
//                                                if (v < minValue) minValue = v;
//                                                if (v > maxValue) maxValue = v;
//                                            }
//                                            //mean += v;
//                                        });
            }

        };
        wavelet1d = new Plot2D.Series("Wavelet", 1) {

            @Override
            public void update() {
                float[] ss = samples;
                if (ss == null) return;
                //samples[0] = null;

                FloatArrayList history = this;

                //                        for (short s : ss) {
                //                            history.add((float)s);
                //                        }
                //
                //
                //                        while (history.size() > maxHistory)
                //                            history.removeAtIndex(0);
                //
                //                        while (history.size() < maxHistory)
                //                            history.add(0);


                //1d haar wavelet transform
                //OneDHaar.displayOrderedFreqsFromInPlaceHaar(x);
                OneDHaar.inPlaceFastHaarWaveletTransform(samples);
                //OneDHaar.displayOrderedFreqsFromInPlaceHaar(samples, System.out);

//                //apache commons math - discrete cosine transform
//                {
//                    double[] dsamples = new double[samples.length + 1];
//                    for (int i = 0; i < samples.length; i++)
//                        dsamples[i] = samples[i];
//                    dsamples = new FastCosineTransformer(DctNormalization.STANDARD_DCT_I).transform(dsamples, TransformType.FORWARD);
//                    for (int i = 0; i < samples.length; i++)
//                        samples[i] = (float) dsamples[i];
//                }

                history.clear();
                history.addAll(samples);

//                minValue = Short.MIN_VALUE;
//                maxValue = Short.MAX_VALUE;

                minValue = Float.POSITIVE_INFINITY;
                maxValue = Float.NEGATIVE_INFINITY;

                history.forEach(v -> {
                    //if (Float.isFinite(v)) {
                    if (v < minValue) minValue = v;
                    if (v > maxValue) maxValue = v;
                    //}
                    //mean += v;
                });

                //System.out.println(maxHistory + " " + start + " " + end + ": " + minValue + " " + maxValue);

            }

        };

        start(updateFrameRate);

    }

    public final synchronized void setSource(WaveSource source) {
        if (this.source != null) {
            this.source.stop();
            this.source = null;
        }

        this.source = source;

        if (this.source != null) {
            int audioBufferSize = this.source.start();

            bufferSamples = audioBufferSize;

            //System.out.println("bufferSamples=" + bufferSamples + ", sampleRate=" + sampleRate + ", numChannels=" + numChannels);

            if (samples == null || samples.length!=audioBufferSize)
                samples = new float[audioBufferSize];
        }
    }

    @Override
    public void run() {
        try {

            source.next(samples);

            nextReady.emit(this);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        WaveCapture au = new WaveCapture(
                new AudioSource(0, 20),
                //new SineSource(128),
                20);

        new SpaceGraph(new RectWidget(au.newMonitorPane(), 16,8)).show(1200,1200);

//            b.setScene(new Scene(au.newMonitorPane(), 500, 400));
//            b.show();
//        });
    }

}
