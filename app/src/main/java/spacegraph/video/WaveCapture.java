package spacegraph.video;

import com.jogamp.opengl.GL2;
import nars.util.event.DefaultTopic;
import nars.util.event.Topic;
import nars.util.signal.OneDHaar;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.Cuboid;
import spacegraph.obj.layout.Grid;
import spacegraph.obj.widget.MatrixView;
import spacegraph.obj.widget.Plot2D;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static spacegraph.obj.layout.Grid.VERTICAL;

/**
 * Created by me on 10/28/15.
 */
public class WaveCapture implements Runnable {


    private final Plot2D.Series rawWave, wavelet1d;

    private int bufferSamples;

    ScheduledThreadPoolExecutor exec;
    private float[] samples;


    private final int freqSamplesPerFrame = 32;
    private final int freqOffset = 16;

    private final int historyFrames = 32;
    private float[] history;

    private WaveSource source;

    /**
     * called when next sample buffer is ready
     */
    final Topic<WaveCapture> nextReady = new DefaultTopic();
    private final boolean normalizeDisplayedWave = false;

    synchronized void start(float FRAME_RATE) {

        if (exec != null) {
            exec.shutdownNow();
            exec = null;
        }

        history = new float[historyFrames * freqSamplesPerFrame];

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


        MatrixView freqHistory = new MatrixView(freqSamplesPerFrame, historyFrames, (x, y, g) -> {
            if (history == null)
                return 0;
            float v = history[y * freqSamplesPerFrame + x];
            g.glColor3f(v, v, v);
            return 0;
        });


        Grid v = new Grid(VERTICAL,
                audioPlot,
                audioPlot2, freqHistory) {
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

                if (normalizeDisplayedWave) {
                    autorange();
                } else {
                    minValue = -1;
                    maxValue = +1;
                }

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
                sampleFrequency(samples);
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

                if (normalizeDisplayedWave) {
                    minValue = Float.POSITIVE_INFINITY;
                    maxValue = Float.NEGATIVE_INFINITY;

                    history.forEach(v -> {
                        //if (Float.isFinite(v)) {
                        if (v < minValue) minValue = v;
                        if (v > maxValue) maxValue = v;
                        //}
                        //mean += v;
                    });
                } else {
                    minValue = -1f;
                    maxValue = 1f;
                }

                //System.out.println(maxHistory + " " + start + " " + end + ": " + minValue + " " + maxValue);

            }

            private void sampleFrequency(float[] freqSamples) {
                int lastFrameIdx = history.length - freqSamplesPerFrame;
                System.arraycopy(history, freqSamplesPerFrame, history, 0, lastFrameIdx);

                int f = freqOffset;
                int freqSkip = 1;
                int n = lastFrameIdx;
                for (int i = 0; i < freqSamplesPerFrame; i++) {
                    history[n++] = freqSamples[f];
                    f+=freqSkip*2;
                }
                System.arraycopy(freqSamples, freqOffset, history, lastFrameIdx, freqSamplesPerFrame);
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
        AudioSource audio = new AudioSource(7, 50);
        WaveCapture au = new WaveCapture(
                audio,
                //new SineSource(128),
                50);
        audio.gain = 1000f;

        new SpaceGraph(new Cuboid(au.newMonitorPane(), 16,8)).show(1200,1200);

//            b.setScene(new Scene(au.newMonitorPane(), 500, 400));
//            b.show();
//        });
    }

}
