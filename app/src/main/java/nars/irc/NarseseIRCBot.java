//package nars.irc;
//
//
//import nars.$;
//import nars.NAR;
//import nars.Op;
//import nars.Task;
//import nars.bag.Bag;
//import nars.concept.Concept;
//import nars.concept.OperationConcept;
//import nars.experiment.misc.Talk;
//import nars.index.TermIndex;
//import nars.nar.Default;
//import nars.term.Compound;
//import nars.term.Term;
//import nars.term.atom.Atom;
//import nars.term.atom.Atomic;
//import nars.term.atom.Operator;
//import nars.time.Tense;
//import nars.util.Texts;
//import nars.util.Util;
//import nars.util.Wiki;
//import nars.util.signal.markov.MarkovChain;
//import org.apache.commons.lang3.StringEscapeUtils;
//import org.jetbrains.annotations.NotNull;
//import org.jetbrains.annotations.Nullable;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Map;
//import java.util.concurrent.ConcurrentHashMap;
//
///**
// * Created by me on 6/20/15.
// */
//public class NarseseIRCBot extends Talk {
//
//    public static final Atom inner = $.the("inner");
//    private static final Logger logger = LoggerFactory.getLogger(NarseseIRCBot.class);
//    private final IRCBot irc;
//
//    class MarkovContext implements Runnable {
//        final MarkovChain<Term> markov = new MarkovChain<Term>(2);
//        final List<Term> buffer = new ArrayList();
//        public final Term context;
//        final int chunkSize = 4;
//        boolean running = true;
//        int updatePeriodMS = 500;
//
//        public MarkovContext(Term context) {
//            this.context = context;
//
//            new Thread(this).start();
//        }
//
//
//        public synchronized void push(Term next) {
//            buffer.add(next);
//            if (buffer.size() > chunkSize) {
//                markov.learn(buffer);
//                buffer.clear();
//            }
//        }
//
//        @Override
//        public void run() {
//            Term x = null;
//            while (running) {
//
//                x = markov.next(true, 0);
//
//                if (x != null) {
//                    hear(x, $.p(inner, context), nar.time(), innerPri);
//
//                }
//
//                Util.pause(updatePeriodMS);
//
//            }
//        }
//    }
//
//    final Map<Term,MarkovContext> markovs = new ConcurrentHashMap();
//
//
//    float innerPri = 0.03f;
//    float wikiPri = 0.05f;
//
//    public NarseseIRCBot(NAR nar, String server, String nick, String channel) throws Exception {
//        super(nar);
//
//
////        inter = new InterNAR(nar);
////        inter.connect("localhost", 11001);
////        inter.broadcastPriorityThreshold = 0.45f;
//
//        irc = new IRCBot(server, nick, channel) {
//            @Override
//            protected void onMessage(String channel, String nick, String msg) {
//                System.err.println("received: " + msg);
//                //NarseseIRCBot.this.onMessage(channel, nick, msg);
//            }
//        };
//
//
//
//
//        this.nar = nar;
//
//        final int maxNodes = 128;
//        final int maxEdges = 8;
//
////        new SpaceGraph<Termed>(
////                new ConceptBagInput(nar, maxNodes, maxEdges)
////        ).withTransform(
////                //new Spiral()
////                new FastOrganicLayout()
////        ).show(900, 900);
//        //BagChart.show((Default) nar);
//
//        addOperators();
//
//        nar.input("say(\"" + this.toString() + " ready\", I)! :|:");
//
//
//
//        nar.onTask(x -> {
//           Term t = x.term();
//            if (x.isInput()) {
//                Atomic operator = Operator.operator(t);
//                if (operator!=null && operator.equals(Talk.hear)) {
//                    Compound args = Operator.opArgs(t);
//                    if (args.size() >= 2) {
//                        Term word = args.term(0);
//                        Term context = args.term(1);
//                        if (!context.toString().equals("I") &&
//                                context.op()!= Op.PROD //avoid (inner, ...)
//                                ) {
//                            markovs.computeIfAbsent(context, MarkovContext::new).push(word);
//                        }
//                    }
//                }
//            }
//        });
//
//
//
//    }
//
//
////    public static void main(String[] args) throws Exception {
////        Param.DEBUG = false;
////
////        Random rng = new XorShift128PlusRandom(1);
////        Default nar = new Default(
////                1024, 4, 2, 2, rng,
////                new CaffeineIndex(new DefaultConceptBuilder(rng), 2000000)
////                //new InfinispanIndex(Terms.terms, new DefaultConceptBuilder(rng))
////                //new Indexes.WeakTermIndex(256 * 1024, rng)
////                //new Indexes.SoftTermIndex(128 * 1024, rng)
////                //new Indexes.DefaultTermIndex(128 *1024, rng)
////                //,new FrameClock()
////                ,new RealtimeMSClock()
////        );
////
////        nar.DEFAULT_BELIEF_PRIORITY = 0.25f;
////        nar.DEFAULT_GOAL_PRIORITY = 0.9f;
////
////        nar.DEFAULT_QUEST_PRIORITY = 0.5f;
////        nar.DEFAULT_QUESTION_PRIORITY = 0.5f;
////
////
////        //nar.inputActivation.setValue(0.1f);
////        nar.cyclesPerFrame.set(16);
////
////        nar.logSummaryGT(System.out, 0.75f);
////
////        new MySTMClustered(nar, 32, '.', 2);
////
////        NarseseIRCBot bot = new NarseseIRCBot(nar);
////
////
////
////        //nar.inputNarsese(new File("/home/me/quietwars.nal"));
////
////
////        List<Task> goals = new ArrayList();
////            goals.add(nar.task("(hear(#x,#c) &&+100 think(#x,I))! %1.0;0.70%"));
////            goals.add(nar.task("((hear(#x,#c) &&+100 hear(#y,#c)) ==>+100 think((#x,#y),I)). %1.0;0.9%"));
////            //goals.add(nar.task("(hear(#x,#c) ==>+100 say(#x,#c)). %1.0;1.0%"));
////
//////            nar.task("((#x --> (/,hear,#c,_)) &&+0 say(#x,#c))! %1.0;1.0%");
//////            nar.task("((#x --> (/,hear,#c,_)) ==>+0 say(#x,#c)). %1.0;1.0%");
//////            nar.task("(#something-->(/,hear,I,_))! %1.0;1.0%");
////
////
////        for (Task t : goals) {
////            nar.input(t);
////        }
//////
//////        new Thread(()->{
//////            while (true) {
//////                for (Task t : goals) {
//////                    nar.activate(t, 1f);
//////                }
//////                Util.pause(10000);
//////            }
//////        }).start();
////
////
////        nar.loop(20f);
////
////        //addInitialCorpus(bot);
////
////    }
//
//    public String toString(Object t) {
//        if (t instanceof Task) {
//            Task tt = (Task)t;
//
//            String ss = ((Task)t).toStringWithoutBudget(nar);
//
//            if (tt.log()!=null && tt.lastLogged().toString().startsWith("Answer"))
//                ss += " " + tt.lastLogged();
//
//            return ss;
//        }
//        return t + " " + t.getClass();
//    }
//
//    final NAR nar;
//    //final InterNAR inter;
//
//
//
//    private void addOperators() {
//
////        nar.onExec(new AbstractOperator("hear") {
////            @Override
////            public void execute(OperationConcept x) {
////                //@Nullable Operator say = operator();
////                Term[] args = x.parameters().terms();
////                if (args.length == 2) {
////                    Term content = args[0];
////                    Term context = args[1];
////                    if (content instanceof Variable) {
////                        return; //maybe randomly select a word
////                    }
////
////                    //hear(content, context, nar.time(), 0.5f);
////                    say(content);
////                }
////
////            }
////        });
//        nar.onExec(new IRCBotOperator("help") {
//
//            @Nullable @Override public Object function(Compound arguments) {
//                return "hahha you need to RTFM";
//            }
//        });
//        nar.onExec(new IRCBotOperator("clear") {
//            @Nullable @Override public Object function(Compound arguments) {
//                @NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;
//                cbag.clear();
//                return "Conscious cleared";
//            }
//        });
////        nar.onExec(new IRCBotOperator("say") {
////
////            @Override protected Object function(Compound arguments) {
////                return arguments.toString();
////                //cancel feedbacks
////                //return null;
////            }
////        });
//
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
//            public int MAX_RESULT_LENGTH = 200;
//
//            @Nullable @Override public Object function(Compound arguments) {
//
//                StringBuilder b = new StringBuilder();
//                @NotNull Bag<Concept> cbag = ((Default) nar).core.concepts;
//
//                if (arguments.term(0) instanceof Atom) {
//                    String query = arguments.term(0).toString().toLowerCase();
//                    cbag.topWhile(c -> {
//                        String bs = c.toString();
//                        String cs = bs.toLowerCase();
//                        if (cs.contains(query)) {
//                            b.append(bs).append("  ");
//                            if (b.length() > MAX_RESULT_LENGTH)
//                                return false;
//                        }
//                        return true;
//                    });
//                } else {
//
//                    cbag.topWhile(c -> {
//                        b.append(c.get()).append('=').append(Texts.n2(c.pri())).append("  ");
//                        if (b.length() > MAX_RESULT_LENGTH)
//                            return false;
//
//                        return true;
//                    });
//                }
//
//                return b.toString();
//            }
//        });
////        nar.onExec(new IRCBotOperator("hear") {
////            @Override
////            protected Object function(Compound arguments) {
////                try {
////                    hear(((Atom) (arguments.term(1))).toStringUnquoted(), arguments.term(0), 0.9f);
////                } catch (Exception e) { }
////                return null;
////            }
////        });
//        nar.onExec(new IRCBotOperator("readWiki") {
//
//
//
//            @Override
//            protected Object function(Compound arguments) {
//
//                String base = "simple.wikipedia.org";
//                                //"en.wikipedia.org";
//                Wiki enWiki = new Wiki(base);
//
//                String lookup = arguments.term(0).toString();
//                try {
//                    //remove quotes
//                    String page = enWiki.normalize(lookup.replace("\"",""));
//                    //System.out.println(page);
//
//                    enWiki.setMaxLag(-1);
//
//                    String html = enWiki.getRenderedText(page);
//                    html = StringEscapeUtils.unescapeHtml4(html);
//                    String strippedText = html.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
//                    //System.out.println(strippedText);
//
//                    List<Term> tokens = hear(strippedText, $.the("wiki_" + page), wikiPri);
//
//                    return "Reading " + base + ":" + page + ": " + strippedText.length() + " characters, " + tokens.size() + " tokens";
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                    return e.toString();
//                }
//            }
//        });
//    }
//
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
//
////    public synchronized void flush() {
////        if (output != null) {
////            SortedIndex<BLink<Task>> l = output.buffer.list;
////
////            int n = Math.min(paragraphSize, l.size());
////
////            Iterable<String> ii = Iterables.transform(
////                    l.list().subList(0, n), (x) -> toString(x.get()) );
////
////            String s = Joiner.on("  ").join(ii);
////            if (!s.isEmpty()) {
////                send(s);
////            }
////            l.clear();
////        }
////    }
//
//
//
//
////    public void goals() {
////
////        //ECHO
////        //nar.goal("(hear(#x,#c) &&+0 hear(#x,union(#c,[I])))", Tense.Present, 1f, 0.9f);
////        //nar.goal("(hear(#x,(#c,I)) &&+1 hear(#x,(I,#c)))", Tense.Present, 1f, 0.5f);
////        //nar.believe("((hear(#x,(#c,I)) &&+1 hear(#y,(#c,I))) ==>+0 (hear(#x,(I,#d)) &&+1 hear(#y,(I,#d))))", Tense.Present, 1f, 0.75f);
////        //nar.goal("(hear(#x,(#c,I)) &&+1 hear(#x,(I,#c)))", Tense.Present, 1f, 0.85f);
////
////        nar.goal("((#x --> (/,hear,#c,_)) &&+0 say(#x,#c))", Tense.Eternal, 1f, 1f);
////        //nar.goal("((#x --> (/,hear,#c,_)) &&+0 say(#x,#c))", Tense.Present, 1f, 0.95f);
////        nar.believe("((#x --> (/,hear,#c,_)) ==>+0 say(#x,#c))", Tense.Eternal, 1f, 1f);
////
////        nar.goal("(#something-->(/,hear,I,_))", Tense.Eternal, 1f, 0.95f); //hear self say something
////
////        //nar.ask($("(hear(?x,?c) ==> hear(?y, ?c))"), '?', nar.time());
////        //nar.ask($("hear(#y, (I,#c))"), '@', nar.time()+wordDelay*10); //what would i like to hear myself saying to someone
////
////
////        //WORD ANALYSIS
////        //nar.goal($("(hear(#x,#c1) &&+1 wordInfo(#x,#z))"), Tense.Eternal, 1f, 0.75f);
////        //nar.believe($("(hear(#x,#c1) &&+1 wordCompare({#x,#y},#z)))"));
////
////        //nar.goal("((hear(#x,?c1) ==> hear(#y,?c2)) &&+0 wordCompare({#x,#y},#z))", Tense.Present, 1f, 0.9f);
////        //nar.ask($("(&&, hear(#x,#c1), hear(#y,#c2), wordCompare({#x,#y},#z))"), '?', nar.time());
////        //nar.believe($("((hear(#x,#c1) &&+0 hear(#y,#c1)) ==>+0 wordCompare({#x,#y},#z)))"));
////
////    }
//
//    public static void addInitialCorpus(NarseseIRCBot bot) {
//        logger.info("Reading corpus..");
//
//        Term sehToMe = $.the("seh");
//
//        final String[] corpus = new String[] {
//                "these words are false.",
//                "here is a word and the next word.",
//                "i hear words.",
//
//                "are these words true?",
//
//                "true is not false.",
//
//                "i say words.",
//                "hear my words!",
//                "say my words!",
//
//                "if i hear it maybe i say it.",
//                "a solution exists for each problem.", //https://simple.wikipedia.org/wiki/Problem
//                "talk in a way that helps and feels good!",
//                "language rules word combining to form statements and questions.",
//                "i learn meaning.",
//                "symbols represent ideas, objects, or quantities.",
//                "communication transcends literal meaning.", //https://simple.wikipedia.org/wiki/Pragmatics
//                "feelings, beliefs, desires, and emotions seem to originate spontaneously.",
//                "what is the origin of mental experience?",
//                "i am not you.",
//                "you are not me.",
//
//                "who am i?",
//                "who are you?",
//                "i am me.",
//                "you are you.",
//                "we are we.",
//                "they are they.",
//
//                "where is it?",
//                "it is here.",
//                "it is there.",
//
//                "why is it?",
//                "it is.",
//                "it is not.",
//                "it is because that.",
//
//                "when is it?",
//                "it is now.",
//                "it is then.",
//
//                "dunno."
//        };
//
//
//        bot.hear(corpus, sehToMe, 0.9f);
//    }
//
////    public void loop(File corpus, int lineDelay) {
////        new Thread(() -> {
////            try {
////                nar.step();
////            }
////            catch (Exception e) {
////                e.printStackTrace();
////            }
////           /* List<String> lines = null;
////            try {
////                lines = Files.readAllLines(Paths.get(corpus.toURI()));
////            } catch (IOException e) {
////                e.printStackTrace();
////                return;
////            }
////
////            while (true)  {
////
////                for (String s : lines) {
////                    s = s.trim();
////                    if (s.isEmpty())continue;
////
////                    nar.input(s);
////
////                    try {
////                        Thread.sleep(lineDelay);
////                    } catch (InterruptedException e) {
////                    }
////                }
////
////            }*/
////        }).start();
////
////    }
////
//
//
//    final StringBuilder buffer = new StringBuilder();
//    int outputBufferLength = 64;
//
//    @Override
//    public void think(OperationConcept o, Term content, Term context) {
//        super.think(o, content, context);
//
////        content.recurseTerms(v -> {
////            String s = null;
////            if (v instanceof Atom) {
////                Atom a = (Atom) v;
////                s = a.toStringUnquoted();
////            } else {
////                s = v.toString();
////            }
////            if (s!=null) {
////                synchronized(buffer) {
////                    buffer.append(' ').append(s);
////                }
////            }
////        });
//
//        if (context.toString().equals("I"))
//            say(content);
//
//    }
//
//    private Term last = $.the("null");
//
//    public void say(Term content) {
//
//        synchronized(buffer) {
//
//            if (content.equals(last)) //filter repeats, although repetition could indicate a degree of emphasis
//                return;
//
//            last = content;
//
//            String x = content.toString().replace("\""," "); //HACK unquote everything
//            String toSend = null;
//
//            buffer.append(x);
//
//            if (buffer.length() > outputBufferLength) {
//                toSend = buffer.toString().trim().replace(" .", ". ").replace(" !", "! ").replace(" ?", "? ");
//                buffer.setLength(0);
//            }
//
//            if (toSend!=null)
//                send(toSend);
//        }
//
//
//    }
//
//    protected void send(String buffer) {
//        //if ((irc.writer!=null) && (irc.outputting)) {
//        nar.goal(0.85f, $.exec($.oper("say"), $.quote( buffer ), $.the("I")), Tense.Present, 1f, 0.9f);
//
//
//            //irc.send(irc.channel, buffer);
////        } else {
////            System.out.println("(not connected)\t" + buffer);
////        }
//    }
//
//
////    public void restart() {
////        if(running !=null) {
////            try {
////                running.stop();
////            } catch (InterruptedException e) {
////                e.printStackTrace();
////            }
////        }
////
////        nar = narBuilder.get();
////
////        send("Ready: " + nar.toString());
////
////
//////        nar.eventTaskProcess.on(c -> {
//////            if (!c.isInput())
//////                //output.buffer.accept(c);
//////                say(c.toString());
//////        });
//////
//////        nar.memory.eventAnswer.on(c -> {
//////            if (c.getOne().isInput())
//////                send(c.toString());
//////        });
////
////        running = nar.loop(100f);
////    }
//
//
//
//
//
//
////    protected void hear(String msg, String context) {
////        try {
////            new Twenglish().parse(nick, nar, msg).forEach(t -> {
////                //t.setPriority(INPUT_SENTENCE_PRIORITY);
////                if (t!=null)
////                    nar.input(t);
////            });
////        } catch (Exception f) {
////            System.err.println(msg + ' ' + f);
////            //send(e.toString());
////        }
////    }
//
//}
//
//
//   /* public void read(String[] sentences, int delayMS, float priority) {
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
//    }*/
//
//
//   /* public int hear(String channel, String m, float priority, long wordDelay) {
//        final int delay = 25, endDelay = 1000, tokenMax = 16, tokenMin = 1;
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
//        String i = "<language --> hear>. :|: \n " + delay + "\n";
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
//                TODO apostrophe words
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
//                else
//                a = "\"" + input.content.toLowerCase() + "\"";
//                String r = "<" + a + " --> " + pattern + ">. :|:\n";
//
//                Term tt = Inheritance.make(wordTerm, Term.get(pattern));
//                char punc = '.';
//
//                Term tt = Operation.make(nar.memory.operate("^say"), new Term[] {wordTerm});
//                char punc = '!';
//
//                nar.input(new Sentence(tt, punc, new TruthValue(1.0f, 0.9f), new Stamp(nar.memory, Tense.Present)));
//                nar.think(delay);
//                r += "say(" + a + ")!\n";
//                r += delay + "\n";
//                try {
//                    Thread.sleep(delay);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//                if (a.isEmpty()) return "";
//                return "<{\"" + a + "\"}-->WORD>.";
//                return "(say, \"" + a + "\", " + channel + "). :|:";
//            }
//        });
//        String xs = "say()!\n" + delay + "\n"; clear the buffer before
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
//
//        System.out.println(nar.time() + " HEAR: " + tokens);
//        System.out.println("HEAR: " + i);
//
//        String i = "<(*," + c + ") --> PHRASE>.";
//        nar.input(i);
//        String j = "<(&/," + c + ") --> PHRASE>. :|:";
//        nar.input(j);
//
//        try {
//            Thread.sleep(endDelay);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        return tokens.size();
//    }*/
