package nars.audio;

import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import spacegraph.audio.Audio;
import spacegraph.audio.SoundListener;
import spacegraph.audio.granular.Granulize;
import spacegraph.audio.sample.SampleLoader;
import spacegraph.audio.sample.SonarSample;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static nars.$.$;

/**
 * NAR sonification
 */
public class SoNAR {

    private final NAR nar;
    private final Audio audio;

    final Map<String, SonarSample> samples = new ConcurrentHashMap<>();

    public SoNAR(NAR n) throws LineUnavailableException {
        this(n, new Audio(16));
    }

    public SonarSample sample(String file) {
        return samples.computeIfAbsent(file, SampleLoader::load);
    }
    public void samples(String dirPath) {
        for (File f : new File(dirPath).listFiles()) {
            String path = f.getAbsolutePath();
            samples.computeIfAbsent(path, SampleLoader::load);
        }
    }

    /** gets a random sample from what is loaded */
    public SonarSample sampleRandom() {
        List<SonarSample> l = samples.values().stream().collect(Collectors.toList());         //HACK
        if (l!=null)
            return l.get(nar.random.nextInt(l.size()));
        else
            return null;
    }

    public SoNAR(NAR n, Audio audio) {
        this.nar = n;
        this.audio = audio;

//        Granulize ts =
//                new Granulize(sample("/tmp/awake.wav"), 0.25f, 0.9f)
//                        .setStretchFactor(0.25f);

        //audio.play(ts, SoundListener.zero, 1, 1);

        //audio.play(new SamplePlayer(smp, 0.5f), SoundListener.zero, 1, 1);

        n.onCycle(this::update);

    }

    /** updated each cycle */
    final Map<Term, Granulize> termListeners = new ConcurrentHashMap();

    public void listen(Term k) {
        termListeners.computeIfAbsent(k, kk->{

            Granulize g = new Granulize(sampleRandom(), 1f, 1f);
            audio.play(g, 0.25f, 1f);
            return g;
        });

    }

    protected void update() {
        termListeners.forEach(this::update);
    }

    private void update(Term k, Granulize v) {
        Concept c = nar.concept(k);
        if (c!=null) {
            float p = nar.pri(k);
            v.setAmplitude(p);
            //v.setStretchFactor(1f + p);
            //g.setStretchFactor(1f/(1f+kk.volume()/4f));

        } else {
            v.stop();
        }
    }

    public void join() throws InterruptedException {
        audio.thread.join();
    }

    public static void main(String[] args) throws LineUnavailableException, InterruptedException, Narsese.NarseseException {
        Default n = new Default();
        n.core.conceptsFiredPerCycle.set(5);
        n.log();
        n.input("a:b. b:c. c:d. d:e. e:f. f:g. a:g?");
        n.loop(5);
        SoNAR s = new SoNAR(n);
        s.samples("/tmp/wav");
        s.listen($("a"));
        s.listen($("b"));
        s.listen($("c"));
        s.listen($("d"));
        s.listen($("e"));
//        s.listen($("a:b"));
//        s.listen($("b:c"));
        s.join();
    }
}
