package nars.irc;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import nars.Global;
import nars.Memory;
import nars.NARLoop;
import nars.bag.BLink;
import nars.bag.impl.DigestBag;
import nars.nar.Default;
import nars.op.in.TextInput;
import nars.op.in.Twenglish;
import nars.task.Task;
import nars.term.TermIndex;
import nars.term.index.MapIndex2;
import nars.time.RealtimeMSClock;
import nars.util.data.Util;
import nars.util.data.sorted.SortedIndex;

import java.io.File;

/**
 * Created by me on 6/20/15.
 */
public class NarseseIRCBot extends IRCBot {


    private final long outputIntervalMS = 2*60000;
    int paragraphSize = 3;

    private Default nar;
    private DigestBag.OutputBuffer output;

    public String toString(Object t) {
        if (t instanceof Task) {
            Task tt = (Task)t;

            String ss = ((Task)t).toStringWithoutBudget(nar.memory);

            if (tt.log()!=null && tt.getLogLast().toString().startsWith("Answer"))
                ss += " " + tt.getLogLast();

            return ss;
        }
        return t + " " + t.getClass();
    }

    public NarseseIRCBot() throws Exception {
        super("irc.freenode.net", "NARchy", "#nars");

        new Thread(()-> {

            while (true) {
                try {
                    flush();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                Util.pause(outputIntervalMS);
            }
        }).start();
    }

    public synchronized void flush() {
        if (output != null) {
            SortedIndex<BLink<Task>> l = output.buffer.list;

            int n = Math.min(paragraphSize, l.size());

            Iterable<String> ii = Iterables.transform(
                    l.list().subList(0, n), (x) -> toString(x.get()) );

            String s = Joiner.on("  ").join(ii);
            if (!s.isEmpty()) {
                send(s);
            }
            l.clear();
        }
    }

    public static void main(String[] args) throws Exception {
        Global.DEBUG = false;

        /*DNARide.show(n.loop(), (i) -> {
        });*/

        new NarseseIRCBot().restart();
    }

    public void loop(File corpus, int lineDelay) {
        new Thread(() -> {
            try {
                nar.step();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
           /* List<String> lines = null;
            try {
                lines = Files.readAllLines(Paths.get(corpus.toURI()));
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }

            while (true)  {

                for (String s : lines) {
                    s = s.trim();
                    if (s.isEmpty())continue;

                    nar.input(s);

                    try {
                        Thread.sleep(lineDelay);
                    } catch (InterruptedException e) {
                    }
                }

            }*/
        }).start();

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


    String buffer = "";
    int outputBufferLength = 100;

    public synchronized void say(String s) {

        System.out.println("say: " + s);
        buffer += ' ' + s;

        if (buffer.length() > outputBufferLength) {


            buffer = buffer.trim();
            buffer = buffer.replace(" .", ". ");

            System.out.println("SAY: " + buffer);
            if ((writer!=null) && (outputting)) {
                send(channel, buffer);
            }
            buffer = "";
        }

    }

    public void restart() {
        if(oldnar!=null) {
            oldnar.stop();
        }

        nar = new Default(new Memory(new RealtimeMSClock(),
                //TermIndex.memoryWeak(numConcepts * 2)
                new MapIndex2(null, null //TODO
                         )), 1024, 1, 4, 3);

        output = new DigestBag.OutputBuffer(nar, 128);

        nar.memory.duration.set(2000);
        nar.core.conceptsFiredPerCycle.set(256);

        //nar.log();

        send("Ready: " + nar.toString());


//        nar.memory.eventTaskProcess.on(c -> {
//            if (!c.isInput() && c.getPriority() > 0.25f)
//                send(c.toString());
//        });
//
//        nar.memory.eventAnswer.on(c -> {
//            if (c.getOne().isInput())
//                send(c.toString());
//        });

        oldnar = nar.loop(0.1f);
    }


    static NARLoop oldnar = null;
    @Override
    protected void onMessage(IRCBot bot, String channel, String nick, String msg) {
        if (channel.equals("unknown")) return;

        if (msg.equals("RESET")) {
            restart();
        }
        else if (msg.equals("WTF")) {
            flush();
        }
        else {

            TextInput ii;
            try {
                ii = nar.input(msg);
            } catch (Exception e) {
                ii = null;
            }

            if (ii == null || ii.size() == 0) {
                try {
                    new Twenglish().parse(nick, nar, msg).forEach(t -> {
                        //t.getBudget().setPriority((float) sentenceBudget);
                        nar.input(t);
                    });
                } catch (Exception f) {
                    System.err.println(msg + ' ' + f);
                    //send(e.toString());
                }
            }
        }

    }
}