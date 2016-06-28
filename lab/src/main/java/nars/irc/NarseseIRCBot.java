package nars.irc;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.concept.OperationConcept;
import nars.experiment.Talk;
import nars.index.TermIndex;
import nars.nal.nal8.Execution;
import nars.nal.nal8.operator.TermFunction;
import nars.task.MutableTask;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by me on 6/20/15.
 */
public class NarseseIRCBot extends Talk {

    private static final Logger logger = LoggerFactory.getLogger(NarseseIRCBot.class);


    public String toString(Object t) {
        if (t instanceof Task) {
            Task tt = (Task)t;

            String ss = ((Task)t).toStringWithoutBudget(nar);

            if (tt.log()!=null && tt.lastLogged().toString().startsWith("Answer"))
                ss += " " + tt.lastLogged();

            return ss;
        }
        return t + " " + t.getClass();
    }

    final NAR nar;
    final IRCBot irc;

    public NarseseIRCBot(NAR nar) throws Exception {
        super(nar);
        irc = new IRCBot(
                "irc.freenode.net",
                //"localhost",
                "NARchy", "#netention") {
            @Override
            protected void onMessage(String channel, String nick, String msg) {
                NarseseIRCBot.this.onMessage(channel, nick, msg);
            }
        };

        this.nar = nar;

        addOperators();
    }

    private void addOperators() {
        nar.onExec(new IRCBotOperator("help") {

            @Nullable @Override public Object function(Compound arguments, TermIndex i) {
                return "hahha you need to RTFM";
            }
        });
        nar.onExec(new IRCBotOperator("memstat") {
            @Nullable @Override public Object function(Compound arguments, TermIndex i) {
                return nar.index.summary();
            }
        });
    }

    abstract class IRCBotOperator extends TermFunction {


        public IRCBotOperator(String id) {
            super(id);
        }

        @Override
        public boolean autoReturnVariable() {
            return true;
        }

        @Override
        protected @Nullable MutableTask result(@NotNull OperationConcept goal, Term y) {

            send("//" + Execution.resultTerm(goal,y).toString());

            //loop back as hearing
            //say($.quote(y.toString()), $.p(nar.self, $.the("controller")));

            return super.result(goal, y);
        }
    }

//    public synchronized void flush() {
//        if (output != null) {
//            SortedIndex<BLink<Task>> l = output.buffer.list;
//
//            int n = Math.min(paragraphSize, l.size());
//
//            Iterable<String> ii = Iterables.transform(
//                    l.list().subList(0, n), (x) -> toString(x.get()) );
//
//            String s = Joiner.on("  ").join(ii);
//            if (!s.isEmpty()) {
//                send(s);
//            }
//            l.clear();
//        }
//    }

    public static void main(String[] args) throws Exception {
        Global.DEBUG = false;



        NAR nar = Talk.nar();

        new NarseseIRCBot(nar);

        nar.loop(15f);

    }

//    public void loop(File corpus, int lineDelay) {
//        new Thread(() -> {
//            try {
//                nar.step();
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//           /* List<String> lines = null;
//            try {
//                lines = Files.readAllLines(Paths.get(corpus.toURI()));
//            } catch (IOException e) {
//                e.printStackTrace();
//                return;
//            }
//
//            while (true)  {
//
//                for (String s : lines) {
//                    s = s.trim();
//                    if (s.isEmpty())continue;
//
//                    nar.input(s);
//
//                    try {
//                        Thread.sleep(lineDelay);
//                    } catch (InterruptedException e) {
//                    }
//                }
//
//            }*/
//        }).start();
//
//    }
//


    String buffer = "";
    int outputBufferLength = 64;

    @Override
    public synchronized void say(Term content, Compound context) {
        super.say(content, context);

        content.recurseTerms(v -> {
            if (v instanceof Atom) {
                Atom a = (Atom)v;
                buffer += ' ' + a.toStringUnquoted();
            }
        });

        if (buffer.length() > outputBufferLength) {

            buffer = buffer.trim().replace(" .", ". ").replace(" !", "! ").replace(" ?", "? ");

            send(buffer);

            buffer = "";
        }

    }

    protected void send(String buffer) {
        if ((irc.writer!=null) && (irc.outputting)) {
            irc.send(irc.channel, buffer);
        } else {
            System.out.println("(not connected)\t" + buffer);
        }
    }


//    public void restart() {
//        if(running !=null) {
//            try {
//                running.stop();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }
//
//        nar = narBuilder.get();
//
//        send("Ready: " + nar.toString());
//
//
////        nar.eventTaskProcess.on(c -> {
////            if (!c.isInput())
////                //output.buffer.accept(c);
////                say(c.toString());
////        });
////
////        nar.memory.eventAnswer.on(c -> {
////            if (c.getOne().isInput())
////                send(c.toString());
////        });
//
//        running = nar.loop(100f);
//    }



    protected void onMessage(String channel, String nick, String msg) {
        if (channel.equals("unknown")) return;
        if (msg.startsWith("//")) return; //comment or previous output

//        if (msg.equals("RESET")) {
//            restart();
//        }
//        else if (msg.equals("WTF")) {
//            flush();
//        }
//        else {

        @NotNull List<Task> parsed = nar.tasks(msg, o -> {
            //logger.error("unparsed narsese {} in: {}", o, msg);
        });

        int narsese = parsed.size();
        if (narsese > 0) {
            for (Task t : parsed) {
                logger.info("narsese({},{}): {}", channel, nick, t);
            }
            parsed.forEach(nar::input);
        } else {
            logger.info("hear({},{}): {}", channel, nick, msg);
            hear(msg, context(channel, nick));
        }

    }

    private Term context(String channel, String nick) {
        return $.quote(nick); //ignore channel for now
    }

//    protected void hear(String msg, String context) {
//        try {
//            new Twenglish().parse(nick, nar, msg).forEach(t -> {
//                //t.setPriority(INPUT_SENTENCE_PRIORITY);
//                if (t!=null)
//                    nar.input(t);
//            });
//        } catch (Exception f) {
//            System.err.println(msg + ' ' + f);
//            //send(e.toString());
//        }
//    }

}


   /* public void read(String[] sentences, int delayMS, float priority) {
        new Thread(new Runnable() {

            @Override
            public void run() {
                for (String s : sentences) {

                    s = s.trim();
                    if (s.length() < 2) continue;

                    if (!s.endsWith(".")  && !s.endsWith("?") && !s.endsWith("!")) s=s+'.';
                    if (hear("book", s, priority, delayMS) == 0) continue;

                }
            }
        }).start();
    }*/


   /* public int hear(String channel, String m, float priority, long wordDelay) {
        final int delay = 25, endDelay = 1000, tokenMax = 16, tokenMin = 1;
        List<Twokenize.Span> tokens = Twokenize.twokenize(m);
        int nonPunc = Iterables.size(Iterables.filter(tokens, new Predicate<Twokenize.Span>() {

            @Override
            public boolean apply(Twokenize.Span input) {
                return !input.pattern.equals("punct");
            }
        }));

        if (nonPunc > tokenMax) return 0;
        if (nonPunc < tokenMin) return 0;



        String i = "<language --> hear>. :|: \n " + delay + "\n";

        Iterable<String> s = Iterables.transform(tokens, new Function<Twokenize.Span, String>() {

            @Override
            public String apply(Twokenize.Span input) {
                String a = "";
                String pattern = "";
                Term wordTerm;
                if (input.pattern.equals("word")) {
                    a = input.content.toLowerCase().toString();
                    wordTerm = Atom.the(a);
                    pattern = "word";
                }
                TODO apostrophe words
                else if (input.pattern.equals("punct")) {
                    String b = input.content;
                    wordTerm = Atom.quote(b);

                    a = input.content;
                    pattern = "word";
                }
                else {
                    return "";
                }
                else
                a = "\"" + input.content.toLowerCase() + "\"";
                String r = "<" + a + " --> " + pattern + ">. :|:\n";

                Term tt = Inheritance.make(wordTerm, Term.get(pattern));
                char punc = '.';

                Term tt = Operation.make(nar.memory.operate("^say"), new Term[] {wordTerm});
                char punc = '!';

                nar.input(new Sentence(tt, punc, new TruthValue(1.0f, 0.9f), new Stamp(nar.memory, Tense.Present)));
                nar.think(delay);
                r += "say(" + a + ")!\n";
                r += delay + "\n";
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if (a.isEmpty()) return "";
                return "<{\"" + a + "\"}-->WORD>.";
                return "(say, \"" + a + "\", " + channel + "). :|:";
            }
        });
        String xs = "say()!\n" + delay + "\n"; clear the buffer before
        for (String w : s) {
            String xs = "$" + Texts.n2(priority) + "$ " + w + "\n";

            System.err.println(xs);
            nar.input(xs);

            try {
                Thread.sleep(wordDelay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }

        System.out.println(nar.time() + " HEAR: " + tokens);
        System.out.println("HEAR: " + i);

        String i = "<(*," + c + ") --> PHRASE>.";
        nar.input(i);
        String j = "<(&/," + c + ") --> PHRASE>. :|:";
        nar.input(j);

        try {
            Thread.sleep(endDelay);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return tokens.size();
    }*/
