package nars;

import com.google.common.base.Joiner;
import nars.op.nlp.Hear;
import spacegraph.audio.MaryTTSpeech;

public class NARchy extends NARS {

    public static NAR all() {
        NAR nar = NARS
                .realtime()
                //.memory("/tmp/nal")
                .then(Hear::wiki)
                .get();


        installSpeech(nar);

        return nar;
    }

    public static void installSpeech(NAR nar) {
        MaryTTSpeech.speak(""); //forces load of TTS so it will be ready ASAP and not load on the first use
        nar.onOpArgs("speak", (args, n) -> {
            if (args.AND(x -> !x.op().var)) {
                String text = Joiner.on(", ").join(args);
                if (text.isEmpty())
                    return;
                if (text.charAt(0)!='"')
                    text = "\"" + text + '"';

                MaryTTSpeech.speak(text);
            }
        });
    }
}
