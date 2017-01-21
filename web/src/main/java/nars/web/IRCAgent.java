package nars.web;

import jcog.Texts;
import jcog.data.random.XorShift128PlusRandom;
import jcog.event.On;
import nars.*;
import nars.bag.ArrayBag;
import nars.budget.BudgetMerge;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ConceptBagControl;
import nars.index.term.map.CaffeineIndex;
import nars.link.BLink;
import nars.link.DefaultBLink;
import nars.nar.Default;
import nars.op.Command;
import nars.op.Leak;
import nars.op.mental.Inperience;
import nars.term.Term;
import nars.term.Terms;
import nars.term.atom.Atom;
import nars.time.RealTime;
import nars.time.Tense;
import nars.util.Loop;
import nars.util.exe.MultiThreadExecutioner;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jetbrains.annotations.NotNull;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.net.IRC;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import static nars.nlp.Twenglish.tokenize;

/**
 * $0.9;0.9;0.99$
 * <p>
 * $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 * $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 * $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 * $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 * $0.9;0.9;0.99$ hear(I, #something)!
 * hear(I,?x)?
 * <p>
 * $0.9$ (($x,"the") <-> ($x,"a")).
 */
public class IRCAgent extends IRC {
    private static final Logger logger = LoggerFactory.getLogger(IRCAgent.class);

    //private final Talk talk;
    private final NAR nar;
    //private float ircMessagePri = 0.9f;

    final int wordDelayMS = 25; //for serializing tokens to events: the time in millisecond between each perceived (subvocalized) word, when the input is received simultaneously
    private final Leak<Task> out;
    private boolean hearTwenglish = true;
    private List<Loop> hearing = new CopyOnWriteArrayList<>();

    public IRCAgent(NAR nar, String nick, String server, String... channels) throws Exception {
        super(nick, server, channels);

        this.nar = nar;

        //talk = new Talk(nar);

        //nar.log();

        out = new Leak<Task>(new ArrayBag<Task>(16, BudgetMerge.plusBlend, new ConcurrentHashMap<>()), 0.01f, nar) {

            //boolean echoCommandInput = false;

            @Override
            protected float onOut(@NotNull BLink<Task> t) {
                return send(t.get());
            }

            private float send(Task task) {
                boolean cmd = task.isCommand();
                if (cmd || !task.isDeleted()) {
                    String s = (!cmd) ? task.toString() : task.term().toString();
                    IRCAgent.this.send(channels, s);
                    return cmd ? 0 : 1; //no cost for command outputs
                }
                return 0;
            }

            @Override
            protected void in(@NotNull Task t, Consumer<BLink<Task>> each) {
                if (t.isCommand()) {
                    send(t); //immediate
                } else {
                    float p = t.pri();
                    if (p == p) { // || (t.term().containsTermRecursively(nar.self()) && (p > 0.5f))) {
                        each.accept(new DefaultBLink<>(t, t));
                    }
                }
            }
        };

//        //SPEAK
//        nar.onTask(t -> {
//
//        });

        nar.on("readWiki", (Command) (op, args, n) -> {

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

            hear(strippedText, page);

            Command.log(n, "Reading " + base + ":" + page + ": " + strippedText.length() + " characters");

        });

        nar.on("top", (Command) (op, args, n) -> {
            ConceptBagControl cbag = ((Default) n).core;

            int MAX_RESULT_LENGTH = 200;
            StringBuilder b = new StringBuilder();

            if (args.length > 0 && args[0] instanceof Atom) {
                String query = args[0].toString().toLowerCase();
                cbag.active.forEachWhile(c -> {
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
                cbag.active.forEachWhile(c -> {
                    b.append(c.get()).append('=').append(Texts.n2(c.pri())).append("  ");
                    if (b.length() > MAX_RESULT_LENGTH)
                        return false;

                    return true;
                });
            }

            Command.log(n, b.toString());
                    //"core pri: " + cbag.active.priMin() + "<" + Texts.n4(cbag.active.priHistogram(new double[5])) + ">" + cbag.active.priMax());

        });


        nar.on("clear", (Command) (op, args, n) -> {
            n.clear();
            n.runLater(()->{
                Command.log(n, "Ready. (" + n.concepts.size() + " subconcepts)");
            });
        });

        //                case "save":
//                    File tmp = Files.createTempFile("nar_save_", ".nal").toFile();
//                    PrintStream ps = new PrintStream(new FileOutputStream(tmp));
//                    nar.outputTasks((x) -> true, ps);
//                    pevent.respondWith("Memory saved to: " + tmp.getAbsolutePath());
//                    return;

//        nar.onExec(new IRCBotOperator("top") {
//
//            @Override
//            protected Object function(Compound arguments) {
//                return null;
//            }
//        });

        /*
        $0.9;0.9;0.99$ (hear(?someone, $something) ==>+1 hear(I,$something)).
 $0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) && hear(I, #someThing)) ==>+1 hear(I, $nextThing)).
 $0.9;0.9;0.99$ (((hear($someone,$someThing) &&+1 hear($someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).
 $0.9;0.9;0.99$ (((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).
 $0.9;0.9;0.99$ hear(I, #something)!
 hear(I,?x)?

 $0.9$ (($x,"the") <-> ($x,"a")).
         */
//        nar.input(
//                "$0.9;0.9;0.99$ (hear(?someone, $something) ==>+0 hear(I,$something)).",
//                "$0.9;0.9;0.99$ (((hear(#someone,#someThing) &&+1 hear(#someone,$nextThing)) &&+0 hear(I, #someThing)) ==>+1 hear(I, $nextThing)).\n",
//                //"$0.9;0.9;0.99$ (((hear(#someone,$someThing) &&+1 hear(#someone,$nextThing)) <=> hear($someone, ($someThing,$nextThing)))).",
//                "$0.9;0.9;0.99$ hear(I, #something)!",
//                //"(((I<->#someone) && hear(#someone, $something)) ==>+1 hear(I, $something)).",
//                "$0.9;0.9;0.99$ hear(I,?x)?"
//        );
//        final Atomic HEAR = $.the("hear");
//        final Atomic I = $.the("I");
//        nar.onTask(tt->{
//            //HACK
//            if (tt.isBelief()) {
//                if (Math.abs(tt.occurrence()-nar.time()) < 100) {
//                    if (tt.op() == INH && tt.term(0).op() == PROD && tt.term(1).equals(HEAR)) {
//                        Compound arg = (Compound) tt.term(0);
//                        if (arg.term(0).equals(I)) {
//                            Term w = arg.term(1);
//                            if (!(w instanceof Variable)) {
//                                say(channels, w);
//                            }
//                        }
//                    }
//                }
//            }
//        });

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

//    abstract class IRCBotOperator implements Operator.CommandOperator {
//
//
//        @Nullable
//        @Override
//        public final synchronized void run(Atom op, Term[] args, NAR nar) {
//
//            Object y = function(args);
//
//            if (y != null)
//                broadcast(y.toString().replace("\n", " "));
//
//            //loop back as hearing
//            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));
//
//            return null;
//        }
//
//        protected abstract Object function(Term[] arguments);
//
//    }

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

//            switch (msg) {
//                case "memstat":
//                    pevent.respondWith(nar.concepts.summary());
//                    return;
//                //@NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;
//                //" | core pri: " + cbag.priMin() + "<" + Texts.n4(cbag.priHistogram(5)) + ">" + cbag.priMax();
////                case "top":
////                    pevent.respondWith(top(Terms.ZeroProduct));
////                    return;
//                case "clear":
//                    ((Default) nar).core.active.clear();
//                    pevent.respondWith("Ready.");
//                    return;
//                case "save":
//                    File tmp = Files.createTempFile("nar_save_", ".nal").toFile();
//                    PrintStream ps = new PrintStream(new FileOutputStream(tmp));
//                    nar.outputTasks((x) -> true, ps);
//                    pevent.respondWith("Memory saved to: " + tmp.getAbsolutePath());
//                    return;
//            }

            String nick = pevent.getUser().getNick(); //TODO use hostmask ?
            String channel = pevent.getChannel().getName();

            try {

                @NotNull List<Task> parsed = $.newArrayList();
                @NotNull List<Narsese.NarseseException> errors = $.newArrayList();
                Narsese.the().tasks(msg.replace("http://", ""), parsed, errors, nar);

                if (!parsed.isEmpty() && errors.isEmpty()) {
                    parsed.forEach(nar::input);
                } else {
                    if (hearTwenglish)
                        hear(msg, nick);
                }

            } catch (Exception e) {
                pevent.respond(e.toString());
            }


            //logger.info("hear({},{}): {}", channel, nick, msg);
            //talk.hear(msg, context(channel, nick), ircMessagePri);
        }


    }


    void hear(@NotNull String msg, @NotNull String who) {
        hearing.add( hear(nar, msg, who, wordDelayMS) );
    }

    public Loop hear(NAR nar, @NotNull String msg, @NotNull String src, int wordDelayMS) {
//        nar.believe(
//                $.inst($.p(tokenize(msg)), $.p($.quote(channel), $.quote(nick))),
//                Tense.Present
//        );

        final List<Term> tokens = tokenize(msg);
        if (!tokens.isEmpty()) {

            Atom chan_nick = $.quote(src);

            Term[] aa = new Term[] { $.the("hear"), chan_nick, Op.Imdex };

            return new Loop(msg, wordDelayMS) {

                public On onReset;
                int token = 0;

                @Override
                public synchronized void stop() {
                    onReset.off();
                    onReset = null;
                    super.stop();
                    hearing.remove(this);
                }

                @Override
                public void next() {
                    if (token >= tokens.size()) {
                        stop();
                        return;
                    }

                    onReset = nar.eventReset.on((n) -> {
                        if (onReset != null) {
                            stop();
                        }
                    });

                    Term next = tokens.get(token++);
                    nar.believe(0.25f,
                            //$.func("hear", chan_nick, tokens.get(token++))
                            $.inh(next, $.imge( aa)),
                            Tense.Present, 1f, 0.9f);
                }
            };
//            nar.runLater(() -> {
//
//                for (Term token :) {
//                    nar.believe(pr, time[0], 1f);
//                    time[0] += wordDelayMS;
//                }
//
//            });
        }

        return null;
    }

    @NotNull
    public static Default newRealtimeNAR(int activeConcepts, int framesPerSecond, int conceptsPerFrame) {

        Random random = new XorShift128PlusRandom(System.currentTimeMillis());

        MultiThreadExecutioner exe = new MultiThreadExecutioner(2, 1024 * 4);
        exe.sync(true);

        Default nar = new Default(activeConcepts, conceptsPerFrame, 1, 3, random,

                new CaffeineIndex(new DefaultConceptBuilder(), 128 * 1024, false, exe),
                //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 400000, 64 * 1024, 3),

                new RealTime.CS(true),
                exe
        );


        int volMax = 48;

//        //Multi nar = new Multi(3,512,
//        Default nar = new Default(2048,
//                conceptsPerCycle, 2, 2, rng,
//                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*1024, volMax/2, false, exe)
//                new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 400000, 64*1024, 3)
//
//                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);

        float p = 0.25f;
        nar.DEFAULT_BELIEF_PRIORITY = 1f * p;
        nar.DEFAULT_GOAL_PRIORITY = 1f * p;
        nar.DEFAULT_QUESTION_PRIORITY = 1f * p;
        nar.DEFAULT_QUEST_PRIORITY = 1f * p;

        nar.confMin.setValue(0.01f);
        nar.termVolumeMax.setValue(volMax);


//        nar.inputLater(
//                NQuadsRDF.stream(nar, new File(
//                        "/home/me/Downloads/nquad"
//                )).
//                        peek(t -> {
//                            t.setBudget(0.01f, 0.5f, 0.9f);
//                        }).
//                        collect(Collectors.toList())
//                , 32
//        );
//        nar.run(1);

        //MySTMClustered stm = new MySTMClustered(nar, 64, '.', 2, true, 1);

        new Inperience(nar, 0.005f, 8);

        nar.loop(framesPerSecond);

        return nar;
    }

    public static void main(String[] args) throws Exception {

        @NotNull Default n = newRealtimeNAR(1024, 50, 200);


        IRCAgent bot = new IRCAgent(n,
                "experiment1", "irc.freenode.net",
                "#123xyz"
                //"#netention"
                //"#nars"
        );


        bot.start();


    }

    final StringBuilder b = new StringBuilder();

    public void say(String[] channels, Term w) {
        logger.info("say {}", w);
        b.append(w.toString()).append(' ');
        if (b.length() > 40) {
            send(channels, b.toString());
            b.setLength(0);
        }

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


}
//package nars.irc;
//
//import com.google.common.base.Function;
//import com.google.common.base.Predicate;
//import com.google.common.collect.Iterables;
//import nars.NAR;
//import spacegraph.net.IRC;
//
//import java.io.File;
//import java.io.IOException;
//import java.nio.file.Files;
//import java.nio.file.Paths;
//import java.util.List;
//
///**
// * Created by me on 6/20/15.
// */
//public class NLPIRCBot extends IRC {
//
//
//    private final NAR nar;
//
//    public NLPIRCBot(NAR n) throws Exception {
//        super("irc.freenode.net", "NARchy", "#nars");
//
//        this.nar = n;
//
//        new NARReaction(nar, say.class) {
//
//            public String last = "";
//
//
//            int minSpokenWords = 2;
//
//            @Override
//            public void event(Class event, Object[] args) {
//                if (event == say.class) {
//                    //Operator.ExecutionResult er = (Operator.ExecutionResult)args[0];
//
//                    Term a = (Term)args[0]; //er.getOperation().getArguments().term;
//
//                    String s = a.toString();
//                    s = s.replace("{\"", "");
//                    s = s.replace("\"}", "");
//                    s = s.trim();
//                    if (s.length() == 1) {
//                        if (s.equals("¸")) s = "."; //hotfix for the period
//                        if (s.equals(last))
//                            return; //dont repeat punctuation
//                    }
//                    else {
//
//                    }
//
//                    if (!s.isEmpty()) {
//                        say(s);
//                        last = s;
//                    }
//
////                    if (a.length >= minSpokenWords)  {
////                        String m = "";
////                        int n = 0;
////                        for (int i = 0; i < a.length; i++) {
////                            Term x = a[i];
////                            if (x.equals(nar.memory.getSelf()))
////                                continue;
////                            m += x.toString().replace("\"", "").trim() + " ";
////                        }
////                        m = m.trim();
////
////                        if (!m.equals(lastMessage))
////                            say(m);
////
////                        lastMessage = m;
////                    }
////                    else {
////                        //System.out.println("not SAY: " + Arrays.toString(a));
////
////                    }
//                }
//            }
//        };
//
//    }
//
//    public static void main(String[] args) throws Exception {
//        Global.DEBUG = false;
//
//
//        Default d = new Default();
//        //Default d = new Solid(4, 64, 0,5, 0,3);
//        d.setActiveConcepts(768);
//
//        d.executionThreshold.set(0.5);
//
//        d.temporalRelationsMax.set(4);
//
//        d.shortTermMemoryHistory.set(4);
//
//        d.conceptTaskTermProcessPerCycle.set(4);
//
//        d.conceptsFiredPerCycle.set(64);
//
//        d.duration.set(100 /* ms */);
//        d.setClock(new RealtimeMSClock(false));
//
//
//        //d.temporalPlanner(16f,8,8,2);
//
//        NAR n = new NAR( d );
//
//        TextOutput.out(n);
//
//        File corpus = new File("/tmp/h.nal");
//        n.input(corpus);
//
//        System.out.print("initializing...");
//        for (int i = 0; i < 10; i++) {
//            System.out.print(i + " ");
//            n.frame(10);
//        }
//        System.out.println("ok");
//
//
//
//
//        Video.themeInvert();
//        new NARSwing(n).setSpeed(0.04f);
//
//
//        NLPIRCBot i = new NLPIRCBot(n);
//
//        i.loop(corpus, 200);
//
//        /*String[] book = String.join(" ", Files.readAllLines(Paths.get("/home/me/battle.txt"))).split("\\. ");
//        i.read(book, 1200, 0.5f);*/
//        String[] book2 = String.join(" ", Files.readAllLines(Paths.get("/home/me/meta.txt"))).split("\\. ");
//        i.read(book2, 1300, 0.25f);
//
//    }
//
//    public void loop(File corpus, int lineDelay) {
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                List<String> lines = null;
//                try {
//                    lines = Files.readAllLines(Paths.get(corpus.toURI()));
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return;
//                }
//
//                while (true)  {
//
//                    for (String s : lines) {
//                        s = s.trim();
//                        if (s.isEmpty())continue;
//
//
//                        nar.input(s);
//
//                        try {
//                            Thread.sleep(lineDelay);
//                        } catch (InterruptedException e) {
//                        }
//                    }
//
//                }
//            }
//        }).start();
//
//    }
//
//    public void read(String[] sentences, int delayMS, float priority) {
//        new Thread(new Runnable() {
//
//            @Override
//            public void run() {
//                for (String s : sentences) {
//
//                    s = s.trim();
//                    if (s.length() < 2) continue;
//
//                    if (!s.endsWith(".")  && !s.endsWith("?") && !s.endsWith("!")) s=s+'.';
//                    if (hear("book", s, priority, delayMS) == 0) continue;
//
//                }
//            }
//        }).start();
//    }
//
//
//    public int hear(String channel, String m, float priority, long wordDelay) {
//        final int delay = 25 /*cycles */, endDelay = 1000, tokenMax = 16, tokenMin = 1;
//        List<Twokenize.Span> tokens = Twokenize.twokenize(m);
//        int nonPunc = Iterables.size(Iterables.filter(tokens, new Predicate<Twokenize.Span>() {
//
//            @Override
//            public boolean apply(Twokenize.Span input) {
//                return !input.pattern.equals("punct");
//            }
//        }));
//
//        if (nonPunc > tokenMax) return 0;
//        if (nonPunc < tokenMin) return 0;
//
//
//
//        //String i = "<language --> hear>. :|: \n " + delay + "\n";
//
//        Iterable<String> s = Iterables.transform(tokens, new Function<Twokenize.Span, String>() {
//
//            @Override
//            public String apply(Twokenize.Span input) {
//                String a = "";
//                String pattern = "";
//                Term wordTerm;
//                if (input.pattern.equals("word")) {
//                    a = input.content.toLowerCase().toString();
//                    wordTerm = Atom.the(a);
//                    pattern = "word";
//                }
//                //TODO apostrophe words
//                else if (input.pattern.equals("punct")) {
//                    String b = input.content;
//                    wordTerm = Atom.quote(b);
//
//                    a = input.content;
//                    pattern = "word";
//                }
//                else {
//                    return "";
//                }
//                //else
//                //  a = "\"" + input.content.toLowerCase() + "\"";
//                //String r = "<" + a + " --> " + pattern + ">. :|:\n";
//
//                //Term tt = Inheritance.make(wordTerm, Term.get(pattern));
//                //char punc = '.';
//
//                //Term tt = Operation.make(nar.memory.operate("^say"), new Term[] {wordTerm});
//                //char punc = '!';
//
//                //nar.input(new Sentence(tt, punc, new TruthValue(1.0f, 0.9f), new Stamp(nar.memory, Tense.Present)));
//                //nar.think(delay);
//                //r += "say(" + a + ")!\n";
//                //r += delay + "\n";
////                try {
////                    Thread.sleep(delay);
////                } catch (InterruptedException e) {
////                    e.printStackTrace();
////                }
//                if (a.isEmpty()) return "";
//                //return "<{\"" + a + "\"}-->WORD>.";
//                return "(say, \"" + a + "\", " + channel + "). :|:";
//            }
//        });
//        //String xs = "say()!\n" + delay + "\n"; //clear the buffer before
//        for (String w : s) {
//            String xs = "$" + Texts.n2(priority) + "$ " + w + "\n";
//
//            System.err.println(xs);
//            nar.input(xs);
//
//            try {
//                Thread.sleep(wordDelay);
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//
//        }
////
////        System.out.println(nar.time() + " HEAR: " + tokens);
////        //System.out.println("HEAR: " + i);
////
////        String i = "<(*," + c + ") --> PHRASE>.";
////        nar.input(i);
////        String j = "<(&/," + c + ") --> PHRASE>. :|:";
////        nar.input(j);
////
////        try {
////            Thread.sleep(endDelay);
////        } catch (InterruptedException e) {
////            e.printStackTrace();
////        }
//
//        return tokens.size();
//    }
//
//
//    String buffer = "";
//    int outputBufferLength = 100;
//
//    public synchronized void say(String s) {
//
//        System.out.println("say: " + s);
//        buffer += " " + s;
//
//        if (buffer.length() > outputBufferLength) {
//
//
//            buffer = buffer.trim();
//            buffer = buffer.replace(" .", ". ");
//
//            System.out.println("SAY: " + buffer);
//            if ((writer!=null) && (outputting)) {
//                send(channel, buffer);
//            }
//            buffer = "";
//        }
//
//    }
//
//    @Override
//    protected void onMessage(IRCBot bot, String channel, String nick, String msg) {
//        new Thread( () -> { hear(channel, msg, 0.7f, 100); } ).start();
//    }
// }
