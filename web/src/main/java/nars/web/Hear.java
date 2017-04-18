package nars.web;

import com.google.common.collect.Lists;
import jcog.event.On;
import jcog.io.Twokenize;
import nars.*;
import nars.concept.Concept;
import nars.nlp.Twenglish;
import nars.op.Command;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.Tense;
import nars.util.Loop;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

import static nars.Op.BELIEF;


public class Hear extends Loop {

    final static Atomic START = Atomic.the("start");

    private final NAR nar;
    private final Term[] context;
    //private final Term[] contextAnonymous;
    private final List<Term> tokens;
    public final On onReset;
    int token;

    float priorityFactor = 1f;
    float confFactor = 1f;
    float offConf = 0.1f; //nar.confidenceDefault(BELIEF) * confFactor/2f;

    /** set wordDelayMS to 0 to disable twenglish function */
    public static Loop hear(NAR nar, String msg, String src, int wordDelayMS) throws Narsese.NarseseException {
        return hear(nar, msg, src, (m) -> {
            if (wordDelayMS > 0) {
                List<Term> tokens = tokenize(m);
                if (!tokens.isEmpty())
                    return new Hear(nar, tokens, src, wordDelayMS);
            }
            return null;
        });
    }

    public static Loop hear(NAR nar, String msg, String src, Function<String,Loop> ifNotNarsese) throws Narsese.NarseseException {
        @NotNull List<Task> parsed = $.newArrayList();
        @NotNull List<Narsese.NarseseException> errors = $.newArrayList();

        Narsese.the().tasks(msg, parsed, nar);

        if (!parsed.isEmpty() && errors.isEmpty()) {
            logger.info("narsese: {}", parsed);
            nar.input(parsed);
            return null;
        } else {
            return ifNotNarsese.apply(msg);
        }
    }

    public Hear(NAR nar, @NotNull List<Term> msg, @NotNull String who, int wordDelayMS) {
        super( );
        this.nar = nar;

        onReset = nar.eventReset.on(this::onReset);
        tokens = msg;
        context = new Term[]{Atomic.the("hear"), $.quote(who), Op.Imdex};
        //contextAnonymous = new Term[]{$.the("hear"), $.varDep(1), Op.Imdex};
        start(wordDelayMS);
    }

    protected void onReset(NAR n) {
        stop();
        onReset.off();
    }


    @NotNull public static List<Term> tokenize(String msg) {
        return Lists.transform(Twokenize.tokenize(msg), Twenglish::spanToTerm);
    }

    @Override
    public void next() {
        if (token >= tokens.size()) {
            stop();
            return;
        }

//        if (token > 0) {
//            hear(tokens.get(token-1), 0.5f); //word OFF
//        }

        hear(token > 0 ? tokens.get(token-1) : START, tokens.get(token++)); //word ON
    }

    private void hear(Term prev, Term next) {
        Compound term = $.inh(next, $.imge(context));
        //Compound termAnonymous = $.inh(next, $.imge(contextAnonymous));
        //$.inh($.p(prev,next), $.imge(context)), //bigram
        //$.prop(next, (context[1])),
        //$.func("hear", chan_nick, tokens.get(token++))

        float onConf = nar.confDefault(BELIEF) * confFactor;

        Concept concept = nar.concept(term);

        //if (((DefaultBeliefTable) concept.beliefs()).eternal.isEmpty()) {
        if (concept == null) {
            //input a constant negative bias - we dont hear the word when it is not spoken
            //only input when first conceptualized
            nar.believe(term, Tense.Eternal, 0f, offConf);
        }

        nar.believe(nar.priorityDefault(BELIEF) * priorityFactor,
                term, //1 word
                Tense.Present, 1f, onConf);


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

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
