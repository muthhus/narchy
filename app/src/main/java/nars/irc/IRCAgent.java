package nars.irc;

import com.github.fge.grappa.exceptions.GrappaException;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.bag.Bag;
import nars.concept.Concept;
import nars.index.CaffeineIndex;
import nars.nar.Default;
import nars.nar.exe.SingleThreadExecutioner;
import nars.nar.util.DefaultConceptBuilder;
import nars.nlp.Twenglish;
import nars.term.Compound;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.time.RealtimeMSClock;
import nars.time.Tense;
import nars.util.Texts;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.util.List;
import java.util.Random;

import static nars.nlp.Twenglish.tokenize;

/**
 * Created by me on 7/10/16.
 */
public class IRCAgent extends IRC {
    private static final Logger logger = LoggerFactory.getLogger(IRCAgent.class);

    //private final Talk talk;
    private final NAR nar;
    //private float ircMessagePri = 0.9f;

    public IRCAgent(NAR nar, String nick, String server, String... channels) throws Exception {
        super(nick, server, channels);

        this.nar = nar;

        //talk = new Talk(nar);

        //nar.log();

        nar.onTask(t -> {
            if (t.pri() >= 0.5f) {
                send(channels, t.toString());
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
//        });

        //inter = new InterNAR(nar, (short)udpPort );
        //inter.broadcastPriorityThreshold = 0.25f; //lower threshold

        nar.believe("connect(\"" + server + "\").", Tense.Present, 1f, 0.9f);
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
                    nar.believe(
                            $.inst($.p(tokenize(msg)), $.p($.quote(channel), $.quote(nick))),
                            Tense.Present
                    );
                }
            } catch (Exception e) {
                pevent.respond(e.toString());
            }

            //logger.info("hear({},{}): {}", channel, nick, msg);
            //talk.hear(msg, context(channel, nick), ircMessagePri);
        }


    }

    @NotNull
    public static Default newRealtimeNAR(int activeConcepts, int framesPerSecond, int conceptsPerFrame) {

        Random random = new XorShift128PlusRandom(System.currentTimeMillis());

        SingleThreadExecutioner exe = new SingleThreadExecutioner();
        Default nar = new Default(activeConcepts, conceptsPerFrame, 2, 2, random,
                new CaffeineIndex(new DefaultConceptBuilder(random), 10000000, false, exe),
                new RealtimeMSClock(),
                exe
        );

        nar.loop(framesPerSecond);

        return nar;
    }

    public static void main(String[] args) throws Exception {

        @NotNull Default n = newRealtimeNAR(512, 4, 128);


        IRC bot = new IRCAgent(n,
                "experiment1", "irc.freenode.net", "#123xyz");

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


    public int MAX_RESULT_LENGTH = 200;

    @Nullable
    public String top(Compound arguments) {

        StringBuilder b = new StringBuilder();
        @NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;

        if (arguments.size() > 0 && arguments.term(0) instanceof Atom) {
            String query = arguments.term(0).toString().toLowerCase();
            cbag.topWhile(c -> {
                String bs = c.toString();
                String cs = bs.toLowerCase();
                if (cs.contains(query)) {
                    b.append(bs).append("  ");
                    if (b.length() > MAX_RESULT_LENGTH)
                        return false;
                }
                return true;
            });
        } else {

            cbag.topWhile(c -> {
                b.append(c.get()).append('=').append(Texts.n2(c.pri())).append("  ");
                if (b.length() > MAX_RESULT_LENGTH)
                    return false;

                return true;
            });
        }

        return b.toString();
    }

}
