package nars.web;

import com.google.common.collect.Lists;
import jcog.event.On;
import jcog.io.Twokenize;
import nars.*;
import nars.nlp.Twenglish;
import nars.term.Term;
import nars.time.Tense;
import nars.util.Loop;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class Hear extends Loop {
    private final NAR nar;
    private final Term[] context;
    private final List<Term> tokens;
    public final On onReset;
    int token = 0;

    /** set wordDelayMS to -1 to disable twenglish function */
    public static Loop hear(NAR nar, String msg, String src, int wordDelayMS) {
        @NotNull List<Task> parsed = $.newArrayList();
        @NotNull List<Narsese.NarseseException> errors = $.newArrayList();
        Narsese.the().tasks(msg.replace("http://", ""), parsed, errors, nar);

        if (!parsed.isEmpty() && errors.isEmpty()) {
            parsed.forEach(nar::input);
        } else {
            if (wordDelayMS > 0)
                return new Hear(nar, msg, src, wordDelayMS);
        }
        return null;
    }

    public Hear(NAR nar, @NotNull String msg, @NotNull String who, int wordDelayMS) {
        super(msg, wordDelayMS);
        this.nar = nar;

        onReset = nar.eventReset.on(this::onReset);
        tokens = tokenize(msg);
        context = new Term[]{$.the("hear"), $.quote(who), Op.Imdex};
    }

    protected void onReset(NAR n) {
        stop();
        onReset.off();
    }


    public static List<Term> tokenize(String msg) {
        return Lists.transform(Twokenize.tokenize(msg), Twenglish::spanToTerm);
    }

    @Override
    public void next() {
        if (token >= tokens.size()) {
            stop();
            return;
        }

        Term next = tokens.get(token++);
        nar.believe(0.25f,
                //$.func("hear", chan_nick, tokens.get(token++))
                $.inh(next, $.imge(context)),
                Tense.Present, 1f, 0.9f);
    }
}
