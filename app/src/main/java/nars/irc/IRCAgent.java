package nars.irc;

import com.github.fge.grappa.exceptions.GrappaException;
import nars.*;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.gui.Vis;
import nars.index.term.TermIndex;
import nars.index.term.tree.TreeTermIndex;
import nars.nal.nal8.operator.TermFunction;
import nars.nar.Default;
import nars.nar.exe.Executioner;
import nars.nar.exe.MultiThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.time.MySTMClustered;
import nars.rdfowl.NQuadsRDF;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.time.RealtimeDSClock;
import nars.time.RealtimeMSClock;
import nars.util.Texts;
import nars.util.Wiki;
import nars.util.data.random.XorShift128PlusRandom;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static nars.nlp.Twenglish.tokenize;

/**
 * Created by me on 7/10/16.
 */
public class IRCAgent extends IRC {
    private static final Logger logger = LoggerFactory.getLogger(IRCAgent.class);

    //private final Talk talk;
    private final NAR nar;
    //private float ircMessagePri = 0.9f;

    final int wordDelay = 60; //for serializing tokens to events: the time in millisecond between each perceived (subvocalized) word, when the input is received simultaneously

    public IRCAgent(NAR nar, String nick, String server, String... channels) throws Exception {
        super(nick, server, channels);

        this.nar = nar;

        //talk = new Talk(nar);

        //nar.log();


        //SPEAK
        nar.onTask(t -> {
            if (t.pri() >= 0.7f) {
                send(channels, t.toString());
            }
        });

        nar.onExec(new IRCBotOperator("readWiki") {


            @Override
            protected Object function(Compound arguments) {

                String base = "simple.wikipedia.org";
                //"en.wikipedia.org";
                Wiki enWiki = new Wiki(base);

                String lookup = arguments.term(0).toString();
                try {
                    //remove quotes
                    String page = enWiki.normalize(lookup.replace("\"", ""));
                    //System.out.println(page);

                    enWiki.setMaxLag(-1);

                    String html = enWiki.getRenderedText(page);
                    html = StringEscapeUtils.unescapeHtml4(html);
                    String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
                    //System.out.println(strippedText);

                    hear(strippedText, "wikipedia", page);

                    return "Reading " + base + ":" + page + ": " + strippedText.length() + " characters";

                } catch (IOException e) {
                    e.printStackTrace();
                    return e.toString();
                }
            }
        });

//        nar.onExec(new TermProcedure("say") {
//            @Override public @Nullable Object function(Compound arguments, TermIndex i) {
//                Term content = arguments.term(0);
//                String context = arguments.size() > 1 ? arguments.term(1).toString() : "";
//                if (context.equals("I"))
//                    send(channel, content.toString());
//
//                return null;
//            }
//        });

//        nar.onExec(new IRCBotOperator("memstat") {
//            @Nullable @Override public Object function(Compound arguments) {
//
//                @NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;
//                return nar.concepts.summary();
//                        //" | core pri: " + cbag.priMin() + "<" + Texts.n4(cbag.priHistogram(5)) + ">" + cbag.priMax();
//
//            }
//        });
//        nar.onExec(new IRCBotOperator("top") {
//
//            @Override
//            protected Object function(Compound arguments) {
//                return null;
//            }
//        });

        //inter = new InterNAR(nar, (short)udpPort );
        //inter.broadcastPriorityThreshold = 0.25f; //lower threshold

        //nar.believe("connect(\"" + server + "\").", Tense.Present, 1f, 0.9f);
    }

    //    abstract class IRCBotOperator extends TermProcedure {
//
//
//        public IRCBotOperator(String id) {
//            super(id);
//        }
//
//
//        @Nullable
//        @Override
//        public final Object function(Compound arguments, TermIndex i) {
//            Object y = function(arguments);
//
//            if (y!=null)
//                send( y.toString().replace("\n"," ") );
//
//            //loop back as hearing
//            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));
//
//            return y;
//        }
//
//        protected abstract Object function(Compound arguments);
//
//
//    }

    abstract class IRCBotOperator extends TermFunction {


        public IRCBotOperator(String id) {
            super(id);
        }


        @Nullable
        @Override
        public final synchronized Object function(Compound arguments, TermIndex i) {

            Object y = function(arguments);

            if (y != null)
                broadcast(y.toString().replace("\n", " "));

            //loop back as hearing
            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));

            return null;
        }

        protected abstract Object function(Compound arguments);

    }

    @Override
    public void onGenericMessage(GenericMessageEvent event) throws Exception {

        //}

        //@Override
        //protected void onMessage(String channel, String nick, String msg) {

//        nar.goal(
//                "say(\"" + msg + "\",(\"" + channel+ "\",\"" + nick + "\"))",
//                Tense.Present, 1f, 0.9f );
//
//    }
//
//    protected void onMessage(String channel, String nick, String msg) {

        if (event instanceof MessageEvent) {
            MessageEvent pevent = (MessageEvent) event;

            if (pevent.getUser().equals(irc.getUserBot())) {
                return; //ignore own messages (echo)
            }

            String msg = pevent.getMessage().trim();

            //        if (channel.equals("unknown")) return;
            if (msg.startsWith("//"))
                return; //comment or previous output

            switch (msg) {
                case "memstat":
                    pevent.respondWith(nar.concepts.summary());
                    return;
                //@NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;
                //" | core pri: " + cbag.priMin() + "<" + Texts.n4(cbag.priHistogram(5)) + ">" + cbag.priMax();
                case "top":
                    pevent.respondWith(top(Terms.ZeroProduct));
                    return;
                case "clear":
                    ((Default) nar).core.concepts.clear();
                    pevent.respondWith("Ready.");
                    return;
                case "save":
                    File tmp = Files.createTempFile("nar_save_", ".nal").toFile();
                    PrintStream ps = new PrintStream(new FileOutputStream(tmp));
                    nar.outputTasks((x) -> true, ps);
                    pevent.respondWith("Memory saved to: " + tmp.getAbsolutePath());
                    return;
            }

            String nick = pevent.getUser().getNick(); //TODO use hostmask ?
            String channel = pevent.getChannel().getName();

            //        if (msg.equals("RESET")) {
            //            restart();
            //        }
            //        else if (msg.equals("WTF")) {
            //            flush();
            //        }
            //        else {

            try {
                try {
                    @NotNull List<Task> parsed = nar.tasks(msg.replace("http://", "") /* HACK */, o -> {
                        //logger.error("unparsed narsese {} in: {}", o, msg);
                    });


                    int narsese = parsed.size();
                    if (narsese > 0) {
                        for (Task t : parsed) {
                            logger.info("narsese({},{}): {}", channel, nick, t);
                        }
                        parsed.forEach(nar::input);
                        return;
                    }
                } catch (GrappaException | Narsese.NarseseException f) {
                    hear(msg, nick, channel);
                }
            } catch (Exception e) {
                pevent.respond(e.toString());
            }

            //logger.info("hear({},{}): {}", channel, nick, msg);
            //talk.hear(msg, context(channel, nick), ircMessagePri);
        }


    }


    void hear(String msg, String who, String channel) {
//        nar.believe(
//                $.inst($.p(tokenize(msg)), $.p($.quote(channel), $.quote(nick))),
//                Tense.Present
//        );

        final long[] time = {nar.time()};

        Atom chan_nick = $.quote(channel + "/" + who);

        nar.runLater(() -> {

            for (Term token : tokenize(msg)) {
                Term pr = $.exec("hear", chan_nick, token );
                nar.believe(pr, time[0], 1f);
                time[0] += wordDelay;
            }

        });

    }

    @NotNull
    public static Default newRealtimeNAR(int activeConcepts, int framesPerSecond, int conceptsPerFrame) throws FileNotFoundException {

        Random random = new XorShift128PlusRandom(System.currentTimeMillis());

        Executioner exe =
                //new SingleThreadExecutioner();
                new MultiThreadExecutioner(3, 1024*8);

        Default nar = new Default(activeConcepts, conceptsPerFrame, 2, 2, random,

                //new CaffeineIndex(new DefaultConceptBuilder(random), 10000000, false, exe),
                new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(new XorShift128PlusRandom(3)), 400000, 64 * 1024, 3),


                new RealtimeDSClock(),
                exe
        );


        int volMax = 25;

//        //Multi nar = new Multi(3,512,
//        Default nar = new Default(2048,
//                conceptsPerCycle, 2, 2, rng,
//                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*1024, volMax/2, false, exe)
//                new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 400000, 64*1024, 3)
//
//                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.8f);

        float p = 0.01f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.75f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 0.25f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;

        nar.confMin.setValue(0.05f);
        nar.compoundVolumeMax.setValue(volMax);


        nar.inputLater(
                NQuadsRDF.stream(nar, new File(
                        "/home/me/Downloads/nquad"
                )).
                        peek(t -> {
                            t.setBudget(0.01f, 0.25f, 0.75f); //low priority
                        }).
                        collect(Collectors.toList())
                , 4f
        );
        nar.run(1);

        MySTMClustered stm = new MySTMClustered(nar, 64, '.', 3, true);

        nar.loop(framesPerSecond);

        return nar;
    }

    public static void main(String[] args) throws Exception {

        @NotNull Default n = newRealtimeNAR(1024, 10, 32);


        IRC bot = new IRCAgent(n,
                "experiment1", "irc.freenode.net",
                "#123xyz"
                //"#netention"
                //"#nars"
        );

        Vis.show(n, 256);

        bot.start();


    }
//    public static void main(String[] args) throws Exception {
//        //while (running) {
//            try {
//                new IRCAgent(
//                        "irc.freenode.net",
//                        //"localhost",
//                        "NARchy", "#netention");
//            } catch (Exception e) {
//                e.printStackTrace();
//
//            }
//        //}
//    }


    public int MAX_RESULT_LENGTH = 800;

    @Nullable
    public String top(Compound arguments) {

        StringBuilder b = new StringBuilder();
        @NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;

        String query;
        if (arguments.size() > 0 && arguments.term(0) instanceof Atom) {
            query = arguments.term(0).toString().toLowerCase();
        } else {
            query = null;
        }

        cbag.topWhile(c -> {
            String bs = c.get().toString();
            if (query == null || bs.toLowerCase().contains(query)) {
                b.append(c.get()).append('=').append(Texts.n2(c.pri())).append("  ");
            }
            return b.length() <= MAX_RESULT_LENGTH;
        });

        return b.toString();
    }

}
