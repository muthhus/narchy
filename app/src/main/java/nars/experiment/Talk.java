package nars.experiment;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
import infinispan.com.google.common.io.NullOutputStream;
import nars.$;
import nars.NAR;
import nars.Op;
import nars.concept.OperationConcept;
import nars.index.CaffeineIndex;
import nars.index.TermIndex;
import nars.nal.Tense;
import nars.nal.nal8.AbstractOperator;
import nars.nal.nal8.operator.TermFunction;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.op.in.Twenglish;
import nars.term.Compound;
import nars.term.Operator;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.variable.Variable;
import nars.time.RealtimeMSClock;
import nars.util.Util;
import nars.util.data.MultiOutputStream;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static nars.$.$;

/**
 * Created by me on 6/27/16.
 */
public class Talk {

    final static Operator hear = $.operator("hear");
    long wordDelay = 50;
    float wordPri = 0.4f;

    public static NAR nar() {
        Random rng = new XorShift128PlusRandom(1);
        Default nar = new Default(
                1024, 4, 2, 3, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng))
                //new InfinispanIndex(Terms.terms, new DefaultConceptBuilder(rng))
                //new Indexes.WeakTermIndex(256 * 1024, rng)
                //new Indexes.SoftTermIndex(128 * 1024, rng)
                //new Indexes.DefaultTermIndex(128 *1024, rng)
                //,new FrameClock()
                ,new RealtimeMSClock()
        );
        nar.DEFAULT_GOAL_PRIORITY = 0.6f;
        nar.DEFAULT_GOAL_DURABILITY = 0.75f;

        nar.conceptActivation.setValue(0.1f);
        nar.cyclesPerFrame.set(64);

        //new MySTMClustered(nar, 32, '.');

        return nar;
    }



    public void goals() {

        //ECHO
        //nar.goal("(hear(#x,#c) &&+0 hear(#x,union(#c,[I])))", Tense.Present, 1f, 0.9f);
        //nar.goal("(hear(#x,(#c,I)) &&+1 hear(#x,(I,#c)))", Tense.Present, 1f, 0.5f);
        nar.believe("(hear(#x,(#c,I)) &&+1 hear(#x,(I,#c)))", Tense.Present, 1f, 0.9f);

        //nar.ask($("(hear(?x,?c) ==> hear(?y, ?c))"), '?', nar.time());
        nar.ask($("hear(#y, (I,#c))"), '@', nar.time()); //what would i like to hear myself saying to someone


        //WORD ANALYSIS
        nar.goal($("(hear(#x,#c1) && wordInfo(#x,#z)))"), Tense.Present, 1f, 0.1f);
        //nar.believe($("(hear(#x,#c1) &&+1 wordCompare({#x,#y},#z)))"));

        //nar.goal("((hear(#x,?c1) ==> hear(#y,?c2)) &&+0 wordCompare({#x,#y},#z))", Tense.Present, 1f, 0.9f);
        //nar.ask($("(&&, hear(#x,#c1), hear(#y,#c2), wordCompare({#x,#y},#z))"), '?', nar.time());
        //nar.believe($("((hear(#x,#c1) &&+0 hear(#y,#c1)) ==>+0 wordCompare({#x,#y},#z)))"));

    }

    private final NAR nar;

    public static class TeeOutputStream extends OutputStream {
        OutputStream ostream1, ostream2;

        /**
         * sole TeeOutputStream constructor
         */
        TeeOutputStream(OutputStream o1, OutputStream o2) throws IOException {
            ostream1 = o1;
            ostream2 = o2;
        }

        public void close() throws IOException {
            ostream1.close();
            ostream2.close();
        }

        public void flush() throws IOException {
            ostream1.flush();
            ostream2.flush();
        }

        public void write(int b) throws IOException {
            byte[] buf = new byte[1];
            buf[0] = (byte) b;
            write(buf, 0, 1);
        }

        public void write(byte[] b, int off, int len) throws IOException {
            ostream1.write(b, off, len);
            ostream2.write(b, off, len);
        }
    }

    boolean loopBack;

    public Talk(NAR n) {
        this.nar = n;


        loopBack = true;
        UnsynchronizedAppenderBase<ILoggingEvent> hearAppender = new UnsynchronizedAppenderBase<>() {
            @Override
            protected void append(ILoggingEvent e) {
                if (loopBack) {
                    String m = e.getFormattedMessage();
                    nar.runLater(() -> {
                        hear(m, nar.self, nar.self);
                    });
                }
            }
        };
        ((ch.qos.logback.classic.Logger)nar.logger).addAppender(hearAppender);
        hearAppender.start();



        nar.onExec(new AbstractOperator("hear") {
            @Override
            public void execute(OperationConcept x) {
                //@Nullable Operator say = operator();
                Term[] args = x.parameters().terms();
                if (args.length == 2) {
                    Term content = args[0];
                    Term context = args[1];
                    if (content instanceof Variable) {
                        return; //maybe randomly select a word
                    }
                    if (context.op() == Op.PROD && context.size() == 2 && ((Compound)context).term(0).equals(nar.self)) {
                        say(content, (Compound) context);
                    }
                }

            }
        });
        nar.onExec(new TermFunction<Term>("wordInfo") {

            @Nullable
            @Override
            public Term function(Compound arguments, TermIndex i) {
                Term the = arguments.term(0);
                if (the instanceof Compound && the.op() == Op.PROD ) //unwrap product
                    the = ((Compound)the).term(0);

                if (the instanceof Atom) {
                    String s = ((Atom)the).toStringUnquoted();
                    int l = s.length();
                    return $.seti( $.the(l + "_chars") );
                }
                return null;
            }

        });

        //nar.logSummaryGT(System.out, 0.25f);

        Term sehToMe = $.p($.the("seh"), nar.self);

        final String[] corpus = new String[] {
                "these words are false.",
                "here is a word and the next word.",
                "i hear words.",

                "are these words true?",

                "true is not false.",

                "i say words.",
                "hear my words!",
                "say my words!",

                "if i hear it maybe i say it.",
                "a solution exists for each problem.", //https://simple.wikipedia.org/wiki/Problem
                "talk in a way that helps and feels good!",
                "language rules word combining to form statements and questions.",
                "i learn meaning.",
                "symbols represent ideas, objects, or quantities.",
                "communication transcends literal meaning.", //https://simple.wikipedia.org/wiki/Pragmatics
                "feelings, beliefs, desires, and emotions seem to originate spontaneously.",
                "what is the origin of mental experience?",
                "i am not you.",
                "you are not me.",

                "who am i?",
                "who are you?",
                "i am me.",
                "you are you.",
                "we are we.",
                "they are they.",

                "where is it?",
                "it is here.",
                "it is there.",

                "why is it?",
                "it is.",
                "it is not.",
                "it is because that.",

                "when is it?",
                "it is now.",
                "it is then.",

                "dunno."
        };

        new Thread(()-> {
            while (true) {
                for (String s : corpus) {
                    //System.out.println("IN: " + s);
                    hear(s, sehToMe);
                    Util.pause(1000);
                }
            }
        }).start();

        new Thread(()->{
            while (true) {
                goals();
                Util.pause(15000);
            }
        }).start();

    }

    public static void inputLoopback(Predicate<String> eachLine) {

        PrintStream sysOut = System.out;

        new Thread(()-> {

            try {

                PipedOutputStream loopOut = new PipedOutputStream();
                System.setOut(new PrintStream(new MultiOutputStream(sysOut, loopOut)));

                PipedInputStream pis = new PipedInputStream();
                loopOut.connect(pis);

                CharStreams.readLines(new InputStreamReader(pis),
                        new LineProcessor<>() {

                            @Override
                            public boolean processLine(String line) throws IOException {
                                return eachLine.test(line);
                            }

                            @Override
                            public Object getResult() {
                                return null;
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    public void hear(String text, Term from, Term to) {
        hear(text, $.p(from, to));
    }

    public void hear(String text, Term context) {
        List<Term> tokens = Twenglish.tokenize(text.toLowerCase());
        if (!tokens.isEmpty())
            hear(context, tokens);
    }

    public void hear(Term context, List<Term> tokens) {
        new Thread(()-> {
            for (Term x : tokens) {
                x = $.p(x);
                hear(context, nar.time(), x);
                Util.pause(wordDelay);
            }
        }).start();
    }

    public synchronized void hear(Term context, float tt, Term x) {
        //System.err.println("HEAR: " + x + " (" + context + ")");
        nar.believe(wordPri, $.exec(hear, new Term[] { x, context } ), Math.round(tt), 1f, 0.1f);
    }

    public void say(Term content, Compound context) {
        //System.err.println("SAY: " + content + " (" + context + ")");
        hear(context, nar.time(), content);
    }


}


//    @NotNull
//    public static String learnSentence(@NotNull NAR nar, int wordDelay, @NotNull String message) {
//        List<Term> t = tokenize(message);
//
//        if (t.isEmpty()) return "silence";
//
//        String sentenceID = Integer.toString(message.hashCode());
//
//        //nar.input("speak:" + sentenceID + ". :|:");
//        //nar.input("say(sentence, " + sentenceID + ")! :|:");
//
//        //float f = 1f;
//        //float df = 0.5f / t.size();
//        for (Term w : t) {
//            //nar.input("sentence(" + sentenceID + "). :|:");// %" + f + ";0.95%");
//            //nar.frame(1);
//
//            //nar.input("(sentence(" + sentenceID + ") ==> say(" + w + ")). :|:");
//
//            nar.input("say(" + w + ")! :|:");
//
//            nar.run(wordDelay/2);
//
//            //nar.input("(--, say(" + w + "))! :|:");
//
//            nar.run(wordDelay/2);
//
//            //nar.input("say(" + w + "). %1|0.9%"); //silence
//            //nar.frame(wordDelay/2);
//
//
//            //f-=df;
//        }
//        //nar.input("say(sentence, " + sentenceID + "). :|: %0%");
//        //nar.input("(--, speak:" + sentenceID + "). :|:");
//
//        //nar.input("(--, sentence(" + sentenceID + ")). :|:");
//
//        //nar.input("(--, sentence(" + sentenceID + ")). :|:");
//
//        return sentenceID;
//    }

