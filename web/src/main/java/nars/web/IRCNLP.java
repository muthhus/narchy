
package nars.web;

import com.google.common.base.Joiner;
import jcog.Util;
import jcog.random.XorShift128PlusRandom;
import nars.*;
import nars.bag.impl.ArrayBag;
import nars.bag.leak.LeakOut;
import nars.budget.BLink;
import nars.budget.BudgetMerge;
import nars.budget.RawBLink;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.map.CaffeineIndex;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.op.Command;
import nars.op.mental.Inperience;
import nars.op.stm.MySTMClustered;
import nars.term.Compound;
import nars.time.RealTime;
import nars.time.Tense;
import nars.util.exe.MultiThreadExecutor;
import org.jetbrains.annotations.NotNull;
import org.pircbotx.hooks.events.MessageEvent;
import org.pircbotx.hooks.events.PrivateMessageEvent;
import org.pircbotx.hooks.types.GenericMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.net.IRC;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

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
 * ((($x --> (/,hear,#c,_)) &&+1 ($y --> (/,hear,#c,_))) ==> bigram($x,$y)).
 */
public class IRCNLP extends IRC {
    private static final Logger logger = LoggerFactory.getLogger(IRCAgent.class);

    //private final Talk talk;
    private final NAR nar;
    //private float ircMessagePri = 0.9f;

    private final boolean hearTwenglish = true;

    final int wordDelayMS = 25; //for serializing tokens to events: the time in millisecond between each perceived (subvocalized) word, when the input is received simultaneously
    private final String[] channels;

    boolean trace;

    final ArrayBag<String> out = new ArrayBag<String>(16, BudgetMerge.maxBlend, new ConcurrentHashMap());
    final ArrayBag<String> prevOut = new ArrayBag<String>(512, BudgetMerge.maxBlend, new ConcurrentHashMap());

    public IRCNLP(NAR nar, String nick, String server, String... channels) throws Exception {
        super(nick, server, channels);

        this.nar = nar;
        this.channels = channels;

        new Thread(()->{
            while (true) {
                if (!out.isEmpty()) {
                    try {
                        BLink<String> next = out.pop();
                        String s = next.get();
                        send(channels, s).run();
                        prevOut.commit();
                        prevOut.put(next);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                Util.pause(5500);
            }
        }).start();

        new MyLeakOut(nar, channels);


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

    /** identical with IRCAgent, TODO share them */
    private class MyLeakOut extends LeakOut {
        private final NAR nar;
        private final String[] channels;

        public MyLeakOut(NAR nar, String... channels) {
            super(nar, 8, 1f);
            this.nar = nar;
            this.channels = channels;
        }

        @Override
        protected float send(Task task) {
            boolean cmd = task.isCommand();
            if (cmd || (trace && !task.isDeleted())) {
                String s = (!cmd) ? task.toString() : task.term().toString();
                Runnable r = IRCNLP.this.send(channels, s);
                if (r!=null) {
                    nar.runLater(r);
                    if (Param.DEBUG && !task.isCommand())
                        logger.info("{}\n{}", task, task.proof());
                } else {
                    //..?
                }
                return cmd ? 0 : 1; //no cost for command outputs
            }
            return 0;
        }

        @Override
        protected void in(@NotNull Task t, Consumer<BLink<Task>> each) {
            if (trace || t.isCommand())
                super.in(t, each);
        }
    }

    public void setTrace(boolean trace) {
        this.trace = trace;
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

    void hear(String text, String src) throws Narsese.NarseseException {
        Hear.hear(nar, text, src, (t) -> {
            Compound f = $.func("SENTENCE", Hear.tokenize(t));
            nar.believe(0.5f, f, Tense.Present, 1f, 0.9f );
            return null;
        });
    }

    @Override
    public void onPrivateMessage(PrivateMessageEvent event) throws Exception {
        //hear(event.getMessage(), event.getUser().toString());
    }

    @Override
    public void onGenericMessage(GenericMessageEvent event) throws Exception {

        if (event instanceof MessageEvent) {
            MessageEvent pevent = (MessageEvent) event;

//            if (pevent.getUser().equals(irc.getUserBot())) {
//                return; //ignore own messages (echo)
//            }

            String msg = pevent.getMessage().trim();

            String src = pevent.getUser().getNick(); //TODO use hostmask ?
            String channel = pevent.getChannel().getName();

            try {

                hear(msg, src);

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

        MultiThreadExecutor exe = new MultiThreadExecutor(3, 1024, true);

        Default nar = new Default(activeConcepts, conceptsPerFrame, 3, random,

                new CaffeineIndex(new DefaultConceptBuilder(), 128 * 1024, false, exe),
                //new TreeTermIndex.L1TreeIndex(new DefaultConceptBuilder(), 400000, 64 * 1024, 3),

                new RealTime.DS(true).durSeconds(1f),
                exe
        );


        //        //Multi nar = new Multi(3,512,
//        Default nar = new Default(2048,
//                conceptsPerCycle, 2, 2, rng,
//                //new CaffeineIndex(new DefaultConceptBuilder(rng), 1024*1024, volMax/2, false, exe)
//                new TreeIndex.L1TreeIndex(new DefaultConceptBuilder(new XORShiftRandom(3)), 400000, 64*1024, 3)
//
//                , new FrameClock(), exe);


        nar.beliefConfidence(0.9f);
        nar.goalConfidence(0.9f);

        float p = 1f;
        nar.DEFAULT_BELIEF_PRIORITY = 0.6f * p;
        nar.DEFAULT_GOAL_PRIORITY = 0.8f * p;

        nar.DEFAULT_QUESTION_PRIORITY = 0.5f * p;
        nar.DEFAULT_QUEST_PRIORITY = 0.5f * p;

        nar.DEFAULT_QUESTION_QUALITY = 0.5f;
        nar.DEFAULT_QUEST_QUALITY = 0.5f;

        nar.confMin.setValue(0.01f);
        nar.termVolumeMax.setValue(64);
        //nar.linkFeedbackRate.setValue(0.005f);


        MySTMClustered stm = new MySTMClustered(nar, 32, BELIEF, 8, true, 3);
        //MySTMClustered stm2 = new MySTMClustered(nar, 32, '.', 2, true, 2);

        //new Abbreviation(nar, "_", 3, 12, 0.001f, 8);
        new Inperience(nar, 0.25f, 8);

        nar.loop(framesPerSecond);

        return nar;
    }

    public static void main(String[] args) throws Exception {

        //Param.DEBUG = true;

        @NotNull Default n = //newRealtimeNAR(2048, 0, 2000);
                NARBuilder.newMultiThreadNAR(3, new RealTime.DSHalf(false));

//        Control c = n.getControl();
//        n.setControl(new ChainedControl(c) {
////            @Override
////            public void activate(Termed term, float priToAdd) {
////
////                synchronized(this) {
////                    System.out.print(term + " " + priToAdd + "\t===");
////                    super.activate(term, priToAdd);
////                    System.out.println(pri(term));
////                }
////            }
//        });


        Hear.wiki(n);

        IRCNLP bot = new IRCNLP(n,
                "experiment1", "irc.freenode.net",
                //"#123xyz"
                //"#netention"
                "#nars"
        );

        n.truthResolution.setValue(0.01f);

        n.on("say", (Command)(x, aa, nn) -> {
            String msg = Joiner.on(' ').join(
                Stream.of(aa).map(t -> {
                    if (t.op()== Op.VAR_DEP) {
                        return "a";
                    }
                    return $.unquote(t);
                }).toArray()
            );

            System.out.println(Arrays.toString(aa));

            if (!bot.prevOut.contains(msg)) {
                bot.out.commit();
                bot.out.put(new RawBLink<String>(msg, 1f, 0.5f));
            }
        });

        //n.log();
        //n.logBudgetMin(System.out, 0.6f);
        //n.log(System.out, x -> x instanceof Task && ((Task)x).isGoal());

        n.input(
                "( (RANGE:{$range} && SENTENCE:$x) ==> SENTENCE:slice($x, $range)).",

                //"((VERB:{$V} && FRAG($X,$V,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",

                //"((FRAG:$XY && ($XY <-> flat((FRAG($X), and, FRAG($Y))))) <=> ((/,MEANS,$X,_) && (/,MEANS,$Y,_))).",

                "((VERB:{$V} && SENTENCE($X,$V))                   ==> (((/,MEANS,$X,_),(/,MEANS,$X,_)) --> (/,MEANS,$V,_))).",
                "((VERB:{$V} && SENTENCE($X,$V,$Y))                ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
                "((&&, VERB:{$V}, ADV:{$A}, SENTENCE($X,$V,$A,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,($V|$A),_))).",
                "((&&, VERB:{$V}, PREP:{$P}, SENTENCE($X,$V,$P,$Y)) ==> (((/,MEANS,$X,_),$P,(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",
                "((&&, VERB:{$V}, DET:{#a}, SENTENCE($X,$V,#a,$Y)) ==> (((/,MEANS,$X,_),{(/,MEANS,$Y,_)}) --> (/,MEANS,$V,_))).",
                "((&&, VERB:{$V}, DET:{#a}, SENTENCE(#a,$X,$V,$Y)) ==> (({(/,MEANS,$X,_)},(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",

                "(SENTENCE($x,\"is\",$y) <=> ($x --> $y)).",
                "(SENTENCE($x,\"is\",\"a\",$y) <=> ({$x} --> $y)).",
                "(SENTENCE($x,\"has\",$y) <=> ($x --> [$y])).",
                "(SENTENCE($x,\"isnt\",$y) <=> (--,($x --> $y))).",
                "(SENTENCE($x,\"and\",$y,\"are\",$z) <=> (($x & $y)-->$z)).",
                "(SENTENCE($x,\"and\",$y) <=> ($x && $y)).",
                "(SENTENCE($x,\"or\",$y) <=> ($x || $y)).",
                "(SENTENCE($x,\"then\",$y) <=> ($x &&+1 $y)).",
                "(SENTENCE($x,\"after\",$y) <=> ($x &&-1 $y)).",
                "(($x --> \"verb\") <=> VERB:{$x}).",
                "(($x --> \"noun\") <=> NOUN:{$x}).",
                "(($x --> \"adverb\") <=> ADV:{$x}).",
                "(($x --> \"preposition\") <=> PREP:{$x}).",
                "(#x <-> \"it\"). %0.5;0.5%",
                "(#x <-> \"that\"). %0.5;0.5%",
                "(#x <-> \"the\"). %0.5;0.5%",
                "(#x <-> \"what\"). %0.5;0.5%",
                "(#x <-> \"who\"). %0.5;0.5%",
                "(#x <-> \"where\"). %0.5;0.5%",
                "(#x <-> \"which\"). %0.5;0.5%",

                //NAL9 integration
                "(believe <-> \"believe\").",
                "(wonder <-> \"wonder\").",
                "(want <-> \"want\").",

                //"((WHERE_PREP:{#in} && FRAG(#in,$where)) ==> WHERE:(/,MEANS,$where,_))."

                //"(FRAG<->SENTENCE).", //sentence is also its own fragment

                "RANGE:{(0,2),(0,3),(1,4)}.",
                //"RANGE:{(0,3),(1,4),(4,7),(0,7)}.",
                //"RANGE:{(0,3)}.",
                //"RANGE:{0,1,2,3,4,(0,1),(0,2),(0,3)}.",

                "VERB:{\"is\",\"maybe\",\"isnt\",\"was\",\"willBe\",\"wants\",\"can\",\"likes\"}.",
                "DET:{\"a\",\"the\"}.",
                "PREP:{\"for\",\"on\",\"in\",\"with\"}.",
                "ADV:{\"never\",\"always\",\"maybe\"}.",

//                "SENTENCE(tom,is,never,sky).",
//                "SENTENCE(tom,is,always,cat).",
//                "SENTENCE(tom,is,cat).",
//                "SENTENCE(tom,is,a,cat).",
//                "SENTENCE(tom,likes,the,sky).",
//                "SENTENCE(tom,likes,maybe,cat).",
//                "SENTENCE(the,sky,is,blue).",
//                "SENTENCE(a,cat,likes,blue).",
//                "SENTENCE(sky,wants,the,blue).",
//                "SENTENCE(sky,is,always,blue).",
//
//            "SENTENCE(yes,wants,no).",
//            "SENTENCE(yes,is,no).",
//            "SENTENCE(yes,isnt,no).",
//
//            "SENTENCE(it,is,my,good).",
//            //"SENTENCE(it,is,bad,and,it,can,good).",
//            "SENTENCE(good,was,bad).",
//            "SENTENCE(right,willBe,wrong).",
//            "SENTENCE(true,can,false).",



                //"$0.9$ (?y --> (/,MEANS,?x,_))?",

                //"$0.9$ SENTENCE(?z,?x,?y)?"

                //"$0.9$ SENTENCE(?y)?",

                //"$0.9;0.9$ (VERB:{$v} ==> say(DO, $v))."
                //"$0.9;0.9$ say(#y)!",

                "$0.9;0.9$ (SENTENCE:#y ==> say:#y).",
                "$0.9;0.9$ (SENTENCE:#y && say:#y)!"
                //"say(cat,is,blue)!",
                //"say(#x,is,#y)!"


        );

        n.loop();

        bot.start();


    }

    public void send(@NotNull String target, String l) {
        irc.send().message(target, l);
    }

}
