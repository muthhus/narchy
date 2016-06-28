package nars.experiment;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import com.google.common.base.Joiner;
import com.google.common.io.CharStreams;
import com.google.common.io.LineProcessor;
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
import nars.op.time.MySTMClustered;
import nars.task.Task;
import nars.term.Compound;
import nars.term.Operator;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.term.variable.Variable;
import nars.time.RealtimeMSClock;
import nars.util.Util;
import nars.util.data.MultiOutputStream;
import nars.util.data.map.CapacityLinkedHashMap;
import nars.util.data.map.RUCache;
import nars.util.data.random.XorShift128PlusRandom;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;

import static nars.$.$;

/**
 * Created by me on 6/27/16.
 */
public class Talk {

    final static Operator hear = $.operator("hear");

    long wordDelay = 100; //in milisec

    public static NAR nar() {
        Random rng = new XorShift128PlusRandom(1);
        Default nar = new Default(
                1024, 4, 2, 3, rng,
                new CaffeineIndex(new DefaultConceptBuilder(rng), true)
                //new InfinispanIndex(Terms.terms, new DefaultConceptBuilder(rng))
                //new Indexes.WeakTermIndex(256 * 1024, rng)
                //new Indexes.SoftTermIndex(128 * 1024, rng)
                //new Indexes.DefaultTermIndex(128 *1024, rng)
                //,new FrameClock()
                ,new RealtimeMSClock()
        );
        nar.DEFAULT_BELIEF_PRIORITY = 0.2f;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f;
        nar.DEFAULT_QUESTION_PRIORITY = 0.5f;

        nar.DEFAULT_GOAL_PRIORITY = 0.7f;
        nar.DEFAULT_GOAL_DURABILITY = 0.75f;

        nar.conceptActivation.setValue(0.25f);
        nar.cyclesPerFrame.set(8);

        //nar.logSummaryGT(System.out, 0.2f);

        //new MySTMClustered(nar, 32, '.');

        return nar;
    }



    public void goals() {

        //ECHO
        //nar.goal("(hear(#x,#c) &&+0 hear(#x,union(#c,[I])))", Tense.Present, 1f, 0.9f);
        //nar.goal("(hear(#x,(#c,I)) &&+1 hear(#x,(I,#c)))", Tense.Present, 1f, 0.5f);
        //nar.believe("((hear(#x,(#c,I)) &&+1 hear(#y,(#c,I))) ==>+0 (hear(#x,(I,#d)) &&+1 hear(#y,(I,#d))))", Tense.Present, 1f, 0.75f);
        nar.goal("(hear(#x,(#c,I)) &&+1 hear(#x,(I,#c)))", Tense.Present, 1f, 0.75f);

        //nar.ask($("(hear(?x,?c) ==> hear(?y, ?c))"), '?', nar.time());
        //nar.ask($("hear(#y, (I,#c))"), '@', nar.time()+wordDelay*10); //what would i like to hear myself saying to someone


        //WORD ANALYSIS
        nar.goal($("(hear(#x,#c1) &&+1 wordInfo(#x,#z))"), Tense.Eternal, 1f, 0.75f);
        //nar.believe($("(hear(#x,#c1) &&+1 wordCompare({#x,#y},#z)))"));

        //nar.goal("((hear(#x,?c1) ==> hear(#y,?c2)) &&+0 wordCompare({#x,#y},#z))", Tense.Present, 1f, 0.9f);
        //nar.ask($("(&&, hear(#x,#c1), hear(#y,#c2), wordCompare({#x,#y},#z))"), '?', nar.time());
        //nar.believe($("((hear(#x,#c1) &&+0 hear(#y,#c1)) ==>+0 wordCompare({#x,#y},#z)))"));

    }

    private final NAR nar;


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
                        hear(m, nar.self, nar.self, 0.5f);
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
                        say(x, content, (Compound) context);
                    }
                }

            }
        });
        nar.onExec(new WordInfo());

        nar.logSummaryGT(System.out, 0.5f);

        Compound sehToMe = $.p($.the("seh"), nar.self);

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

        hear(corpus, sehToMe, 0.75f);


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

    public List<Term> hear(String text, Term from, Term to, float pri) {
        return hear(text, $.p(from, to), pri);
    }

    public List<Term> hear(String[] text, Compound context, float pri) {
        return hear(Joiner.on(" ").join(text), context, pri);
    }

    public List<Term> hear(String text, Compound context, float pri) {
        List<Term> tokens = Twenglish.tokenize(text.toLowerCase());
        if (!tokens.isEmpty())
            hear(context, tokens, pri);
        return tokens;
    }

    public Thread hear(Compound context, List<Term> tokens, float pri) {
        Thread t = new Thread(()-> {
            for (Term x : tokens) {
                hear(context, nar.time(), x, pri);
                Util.pause(wordDelay);
            }
        });
        t.start();
        return t;
    }

    public @Nullable Task hear(Term context, float tt, Term x, float pri) {
        //System.err.println("HEAR: " + x + " (" + context + ")");
        return nar.believe(pri * nar.priorityDefault('.'),
                    $.exec(hear, x, context ),
                    Math.round(tt),
                    1f, 0.5f + 0.5f * nar.confidenceDefault('.')*pri);
    }

    public void say(OperationConcept o, Term content, Compound context) {
        long now = nar.time();
        float exp = o.goals().expectation(now);
        System.err.println("SAY: " + content + " (" + context + ")  " + exp );

        Task belief = hear(context, now, content, exp);

        //apply negative-feedback for that output
        nar.goal(belief.pri(), belief.term(), belief.occurrence(), 1f-belief.freq(), belief.conf());
    }


    private static class WordInfo extends TermFunction<Term> {

        final CapacityLinkedHashMap<Term,Object> x = new CapacityLinkedHashMap(512);

        public WordInfo() {
            super("wordInfo");
        }

        private static final Logger logger = LoggerFactory.getLogger(WordInfo.class);

        @Nullable
        @Override
        public Term function(Compound arguments, TermIndex i) {
            Term the = arguments.term(0);


            if (the instanceof Atom) {

                if (x.containsKey(the)) {
                    return null; //recently looked-up
                }

                String s = ((Atom)the).toStringUnquoted();
                logger.info("lookup: {}", s);
                int l = s.length();
                Term result = $.seti( $.the(l + "_chars") );

                x.put(the, result);

                return result;
            }
            return null;
        }

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

