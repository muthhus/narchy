package nars.web;

import com.google.common.collect.Lists;
import jcog.event.On;
import jcog.io.Twokenize;
import nars.*;
import nars.nlp.Twenglish;
import nars.op.Command;
import nars.term.Term;
import nars.time.Tense;
import nars.util.Loop;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
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
            if (wordDelayMS > 0) {
                List<Term> tokens = tokenize(msg);
                if (tokens!=null && !tokens.isEmpty())
                    return new Hear(nar, tokens, src, wordDelayMS);
            }
        }
        return null;
    }

    public Hear(NAR nar, @NotNull List<Term> msg, @NotNull String who, int wordDelayMS) {
        super( );
        this.nar = nar;

        onReset = nar.eventReset.on(this::onReset);
        tokens = msg;
        context = new Term[]{$.the("hear"), $.quote(who), Op.Imdex};
        start(wordDelayMS);
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

    static public void wiki(NAR nar) {
        nar.on("readWiki", (Command) (op, args, n) -> {

            try {
                String base = "simple.wikipedia.org";
                //"en.wikipedia.org";
                Wiki enWiki = new Wiki(base);

                String lookup = args[0].toString();
                //remove quotes
                String page = enWiki.normalize(lookup.replace("\"", ""));
                //System.out.println(page);

                enWiki.setMaxLag(-1);

                String html = enWiki.getRenderedText(page);
                html = StringEscapeUtils.unescapeHtml4(html);
                String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ").toLowerCase();

                //System.out.println(strippedText);

                Hear.hear(nar, strippedText, page, 25);

                Command.log(n, "Reading " + base + ":" + page + ": " + strippedText.length() + " characters");

            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
