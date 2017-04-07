package nars.audio;

import nars.NAR;
import nars.Narsese;
import nars.concept.Concept;
import nars.nar.Default;
import nars.term.Term;
import nars.truth.Truth;
import spacegraph.audio.Audio;
import spacegraph.audio.granular.Granulize;
import spacegraph.audio.sample.SampleLoader;
import spacegraph.audio.sample.SonarSample;

import javax.sound.sampled.LineUnavailableException;
import java.io.File;
import java.io.FileNotFoundException;
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
        this(n, new Audio(64));
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
        if (l!=null && !l.isEmpty())
            return l.get(Math.abs(nar.random.nextInt(l.size())));
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

            Granulize g = new Granulize(sampleRandom(), 0.25f, 1.5f);
            audio.play(g, 0.1f, 1f, (float) (Math.random() - 0.5f));
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

            Truth b = c.belief(nar.time(), nar.dur());
            if (b!=null)
                v.setStretchFactor((b.expectation() - 0.5f) * 2f);
            
            //v.setStretchFactor();
            v.pitchFactor.setValue(1f + (c.volume()));
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
        n.deriver.conceptsFiredPerCycle.set(2);
        //n.log();
        n.input("a:b. :|: (--,b:c). c:d. d:e. (--,e:f). f:g. b:f. a:g?");
        n.loop(64);
        SoNAR s = new SoNAR(n);
        s.samples("/home/me/wav");
        s.listen($("a"));
        s.listen($("b"));
        s.listen($("c"));
        s.listen($("d"));
        s.listen($("e"));
        s.listen($("f"));
        s.listen($("g"));
        s.listen($("a:b"));
        s.listen($("b:c"));
        s.listen($("c:d"));
        s.listen($("d:e"));
        s.listen($("e:f"));
        s.listen($("f:g"));
        s.listen($("a:g"));
        try {
            s.audio.record("/tmp/test.raw");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        s.join();
    }
}
