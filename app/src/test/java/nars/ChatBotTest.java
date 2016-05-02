package nars;

import nars.nal.nal8.operator.TermFunction;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.variable.Variable;
import org.jetbrains.annotations.Nullable;

/**
 * Created by me on 5/2/16.
 */
public class ChatBotTest {

    public static void main(String[] args) {
        Global.DEBUG = true;

        Default n = new Default();

        n.cyclesPerFrame.set(64);

        n.onExecution("say", x -> {

            Term w = x.get(0).term().subterm(0, 0);
            if (!(w instanceof Variable) && (0 == w.vars()) && (!w.hasAny(Op.ImageBits))) {
                System.out.println("SAY: " + x);
                n.input("say(" + w + "). :/:");
            }
        });
        n.onExec(new TermFunction("last") {
            @Nullable
            @Override
            public Object function(Compound arguments, TermIndex i) {
                Term w = arguments.term(0);
                if (w instanceof Compound) {
                    return ((Compound)w).last();
                }
                return null;
            }
        });



        //n.onQuestion()

//        n.onExec(new TermFunction("in") {
//
//            @Nullable
//            @Override
//            public Object function(Compound arguments, TermIndex i) {
//                System.out.println("in(? " + arguments);
//                try {
//                    Term needle = arguments.term(0);
//                    Compound haystack = (Compound) arguments.term(1);
//                    return haystack.containsTermRecursively(needle) ? "true" : "false";
//                } catch (Throwable t) {
//
//                }
//                return null;
//            }
//        });

        //<<($1,$2,$3,$4,$5,$6) --> sentence> ==>+0 (&|,<$1 --> word>,<$2 --> word>,<$3 --> word>,<$4 --> word>,<$5 --> word>,<$6 --> word>)>.
//        n.input("((in($1, $2, true) && ($2 --> words)) ==> ($1 --> words)).");

        //if something is a sentence, and NARS tell it, then NARS is active
        n.input("(<$1 --> words> ==>+0 say($1))!");
        n.input("(say(#1) ==>+0 <SELF --> [active]>).");

        //if something is a sentence and NARS tells thanks for telling then NARS is active
        n.input("(words:$1 <=>+0 words:(\"thank you for telling me\",$1) ).");

        //n.input("( (&&, ($1<->$2), ($1 -->words), ($2 --> words)) <=> words:($1,is,$2) ).");
        n.input("( ($1<->$2) ==> words:($1,is,$2) ).");
        n.input("( (words:#x && last(#x, \"?\")) ==> words:(\"the question\", #x))!");

        //if tell,me,something,about is told, then it is a sign for curiosity
        //n.input("((<$1 --> word> && say(tell,me,something,about,$1)) ==>+0 <SELF --> [curious]>).");
        //n.input("((<$1 --> words> && say((why,$1))) ==>+0 <SELF --> [curious]>).");


//the things which are said are sentences and vice versa
        //n.input("(say($1) <=>+0 <$1 --> words>).");

//some grammar evidence:
        n.input("((<$1 --> words> && <$2 --> words>) ==> <($1,and,$2) --> words>).");

        //n.input("({earth, life, truth} --> words).");


        //n.input("((--,<SELF --> [active]>) ==>+0 <SELF --> [curious]>)!");

        //n.log();
        //n.input("words:(hello,now).");
        //n.input("words:(what,the,fuck).");

        n.input("words:(is,this,sentence,\"?\").");
        n.input("words:(this,is,sentence).");
        n.input("words:(it,is,now).");
        //n.input("words:(i,am,speaking).");

        n.input("<SELF --> [active]>!");
        //n.input("<SELF --> [curious]>!");


        n.run(100024);
    }

}
