package nars.nlp;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.Task;
import nars.nar.Default;
import nars.op.Command;
import nars.op.Operator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.obj.IntTerm;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static nars.term.Terms.compoundOrNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 2/18/17.
 */
public class NLPOperatorTest {


    @Test
    public void testNLP1() {
        //Param.DEBUG = true;

        NAR n = new Default(1024,10,2,4);


        //n.log();

        n.log(System.out, x -> x instanceof Task && ((Task)x).isGoal());

        n.input(
            "( (RANGE:{$range} && SENTENCE:$x) ==> FRAG:slice($x, $range)).",

            "((VERB:{$V} && FRAG($X,$V,$Y)) ==> (((/,MEANS,$X,_),(/,MEANS,$Y,_)) --> (/,MEANS,$V,_))).",

            //"((FRAG:$XY && ($XY <-> flat((FRAG($X), and, FRAG($Y))))) <=> ((/,MEANS,$X,_) && (/,MEANS,$Y,_))).",



            "(FRAG<->SENTENCE).", //sentence is also its own fragment

            "RANGE:{(0,3),(1,4),(4,7),(0,7)}.",
                //"RANGE:{(0,3)}.",
                //"RANGE:{0,1,2,3,4,(0,1),(0,2),(0,3)}.",

            "VERB:{is,maybe,isnt,was,willBe,wants,can}.",
            //"VERB:{is,maybe,isnt}.",

            "SENTENCE(yes,wants,no).",
            "SENTENCE(yes,is,no).",
            "SENTENCE(yes,isnt,no).",

            "SENTENCE(it,is,my,good).",
            "SENTENCE(it,is,bad,and,it,can,good).",
            "SENTENCE(good,was,bad).",
            "SENTENCE(right,willBe,wrong).",
            "SENTENCE(true,can,false).",



            //"$0.9$ (?y --> (/,MEANS,?x,_))?",

            "$0.9$ SENTENCE(?z,?x,?y)?"

            //"$0.9$ SENTENCE:{?y}?",

            //"$0.9$ (SENTENCE:{$y} ==> say($y)).",
            //"$0.9;0.9$ (SENTENCE(#y) && say(#y))!",

            //"$0.9;0.9$ say(#y)!"
        );

        n.run(250);
    }

}
