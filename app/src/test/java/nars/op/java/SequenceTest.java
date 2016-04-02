package nars.op.java;

import nars.$;
import nars.Global;
import nars.nal.Tense;
import nars.nar.Default;
import nars.op.in.Twenglish;
import nars.task.Task;
import nars.term.Term;
import nars.term.Termed;
import nars.term.atom.Atom;
import nars.truth.Truth;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import static nars.Op.ATOM;

/**
 * Created by me on 4/1/16.
 */
public class SequenceTest {

    final static Atom hear = $.the("hear");

    public static void main(String[] args) {
        Global.DEBUG = true;

        Default n = new Default(1000, 32, 2, 2);
        n.cyclesPerFrame.set(8);
        n.conceptForgetDurations.setValue(2);
        n.activationRate.setValue(0.05f);
        n.shortTermMemoryHistory.set(3);

        n.logSummaryGT(System.out, 0.1f);

        int repeats = 7;
        int w = 0;

        int words = 2;
        Term last = null;
        List<Term> s =
                //Twenglish.tokenize("this is a sentence.");
                Twenglish.tokenize("a b c d");
        int wordTime = 5;
        for (int r = 0; r < repeats; r++) {
            for (int i = 0; i < s.size(); i++) {

                /*if (last != null)
                    n.believe(hear(last), Tense.Present, 0f, 0.99f);//.step();*/

                w = (w + 1) % words;

                last = s.get(i);
                n.believe(hear(last), Tense.Present, 1f, 0.99f).run(wordTime);

            }
        }

        n.run(wordTime*4);

        {
            //hallucinate(n, s, wordTime);
        }

        TreeSet<Task> beliefs = new TreeSet(Truth.compareConfidence);

        n.forEachConcept(c -> {
            Task t = c.beliefs().top(n.time());
            if (t!=null)
                beliefs.add(t);

        });

        beliefs.forEach(System.out::println);
        //beliefs.forEach(t->System.out.println(t.explanation()));
    }

    public static void hallucinate(Default n, List<Term> s, int wordTime) {
        Termed next = s.get(0);
        List<Termed> spoken = new ArrayList();

        for (int i = 0; i < 10; i++) {

            spoken.add(next);
            n.input(hear(next) + ". :|: %1.0;0.5%"); //subvocalization, thinking to self


            n.run(wordTime / 2);

            //n.input("(hear:{" + next + "} ==> ?x)? :/:");
            //n.input("(hear:{" + next + "} ==> ?x)?");
            //n.input(hear($.$("?X")) + "? :/:");

            n.run(wordTime / 2); //TODO integrate the concept priorities for the next set of cycles

            //List<BLink<? extends Termed>> top = new ArrayList();
            int max = 5;
            List<Termed> top = new ArrayList();
            Termed finalNext = next;
            n.core.active.topWhile(ww -> {
                Termed x = ww.get();
                if (x.op() == ATOM && !x.equals(hear) && !x.equals(finalNext))
                    top.add(x);
                return top.size() < max;
            });
            if (top.isEmpty()) {
                //..
                continue;
            }

            next = top.get(0);

            System.out.println(top);


        }

        System.out.println();
        System.out.println(spoken);
    }

    public static Term hear(Termed word) {
        //return $.$("hear:{" + word + "}");
        return $.image(1, hear, $.sete(word.term()) );
    }
}
