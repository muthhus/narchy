package spacegraph.audio;

import com.jogamp.opengl.GL2;
import jcog.Util;
import jcog.event.ListTopic;
import jcog.event.Topic;
import jcog.math.OneDHaar;
import org.eclipse.collections.impl.list.mutable.primitive.FloatArrayList;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.layout.Grid;
import spacegraph.widget.meter.MatrixView;
import spacegraph.widget.meter.Plot2D;
import spacegraph.widget.slider.FloatSlider;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static spacegraph.layout.Grid.VERTICAL;
import static spacegraph.layout.Grid.row;

/**
 * Created by me on 10/28/15.
 */
public class WaveCapture implements Runnable {


    private final Plot2D.Series rawWave, wavelet1d;

    private int bufferSamples;

    ScheduledThreadPoolExecutor exec;
    private float[] samples;


    public final int freqSamplesPerFrame = 16;

    private final int historyFrames = 16;
    public float[] data;

    private WaveSource source;

    /**
     * called when next sample buffer is ready
     */
    final Topic<WaveCapture> nextReady = new ListTopic();

    /** holds the normalized value of the latest data */
    public float[] dataNorm = new float[freqSamplesPerFrame];

    //private final boolean normalizeDisplayedWave = false;

    synchronized void start(float FRAME_RATE) {

        if (exec != null) {
            exec.shutdownNow();
            exec = null;
        }

        data = new float[historyFrames * freqSamplesPerFrame];

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
            if (data == null)
                return 0; //HACK
            float kw = (data[y * freqSamplesPerFrame + x]);
            //int kw = (int)(v*255);
            g.glColor3f(kw >= 0 ? kw : 0, kw < 0 ? -kw : 0, 0);
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

    interface Envelope {
        float apply(int band, int frequency);
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


                int bufferSamples = Math.min(WaveCapture.this.bufferSamples, samples.length);
                for (int i = 0; i < bufferSamples; i++)
                    add(samples[i]);

                //minValue = -0.5f;
                //maxValue = 0.5f;

//                if (normalizeDisplayedWave) {
//                    autorange();
//                } else {
//                    minValue = -1;
//                    maxValue = +1;
//                }

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

            final float[] transformedSamples = new float[Util.largestPowerOf2NoGreaterThan(bufferSamples)];
            final AtomicBoolean busy = new AtomicBoolean();

            @Override
            public void update() {

                if (!busy.compareAndSet(false, true))
                    return;



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


                final int bufferSamples = Math.min(samples.length, WaveCapture.this.bufferSamples);

                float[] ss = transformedSamples;
                //1d haar wavelet transform
                //OneDHaar.displayOrderedFreqsFromInPlaceHaar(x);
                System.arraycopy(samples, 0, ss, 0, bufferSamples); //the remainder will be zero
                OneDHaar.inPlaceFastHaarWaveletTransform(ss);
                sampleFrequency(ss);
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
                for (int i = 0; i < bufferSamples; i++)
                    history.addAll(ss[i]);

//                minValue = Short.MIN_VALUE;
//                maxValue = Short.MAX_VALUE;

//                if (normalizeDisplayedWave) {
//                    minValue = Float.POSITIVE_INFINITY;
//                    maxValue = Float.NEGATIVE_INFINITY;
//
//                    history.forEach(v -> {
//                        //if (Float.isFinite(v)) {
//                        if (v < minValue) minValue = v;
//                        if (v > maxValue) maxValue = v;
//                        //}
//                        //mean += v;
//                    });
//                } else {
//                    minValue = -1f;
//                    maxValue = 1f;
//                }

                //System.out.println(maxHistory + " " + start + " " + end + ": " + minValue + " " + maxValue);

                busy.set(false);
            }

            private void sampleFrequency(float[] freqSamples) {
                int lastFrameIdx = data.length - freqSamplesPerFrame;

                int samples = freqSamples.length;

                float bandWidth = ((float)samples) / freqSamplesPerFrame;
                float sensitivity = 1f;

                final Envelope uniform = (i,k)->{
                    float centerFreq = (0.5f + i) * bandWidth;
                    return 1f/(1f + Math.abs(k - centerFreq)/(bandWidth/sensitivity));
                };

                System.arraycopy(data, 0, data, freqSamplesPerFrame, lastFrameIdx);

                float[] h = WaveCapture.this.data;

//                int f = freqOffset;
//                int freqSkip = 1;
//                for (int i = 0; i < freqSamplesPerFrame; i++) {
//                    h[n++] = freqSamples[f];
//                    f+=freqSkip*2;
//                }
                float max = Float.NEGATIVE_INFINITY, min = Float.POSITIVE_INFINITY;
                for (int i = 0; i < freqSamplesPerFrame; i++) {

                    float s = 0;
                    for (int k = 0; k < samples; k++) {
                        float fk = freqSamples[k];
                        s += uniform.apply(i,k) * fk;
                    }
                    if (s > max)
                        max = s;
                    if (s < min)
                        min = s;

                    h[i] = s;
                }

                if (max!=min) { //TODO epsilon check
                    float range = max-min;
                    for (int i = 0; i < freqSamplesPerFrame; i++)
                        dataNorm[i] = (data[i]-min) / range;
                }

                //System.arraycopy(freqSamples, 0, history, 0, freqSamplesPerFrame);
            }

        };

        rawWave.range(-1,+1);
        wavelet1d.range(-1,+1);

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
                samples = new float[Util.largestPowerOf2NoGreaterThan(audioBufferSize)];
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
        AudioSource audio = new AudioSource( 7, 20);
        WaveCapture au = new WaveCapture(
                audio,
                //new SineSource(128),
                20);

        SpaceGraph.window(row(
            au.newMonitorPane(),
            new FloatSlider(audio.gain)
        ),1200,1200);

//            b.setScene(new Scene(au.newMonitorPane(), 500, 400));
//            b.show();
//        });
    }

}
