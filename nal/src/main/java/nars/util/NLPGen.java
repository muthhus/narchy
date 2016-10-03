package nars.util;

import nars.Narsese;
import nars.Task;
import nars.index.PatternIndex;
import nars.nar.Terminal;
import nars.term.Term;
import nars.term.subst.FindSubst;
import nars.time.Tense;

import java.util.ArrayList;
import java.util.List;

import static nars.Op.VAR_PATTERN;

/**
 * Created by me on 7/9/16.
 */
public class NLPGen {

    final static Terminal terminal = new Terminal();
    final PatternIndex index = new PatternIndex();

    public interface Rule {
        String get(Term t, float freq, float conf, Tense tense);
    }

    final List<Rule> rules = new ArrayList();

    public NLPGen() {
        train("A a B", "(A --> B).");
        train("A instancing B", "({A} --> B).");
        train("A propertied B", "(A --> [B]).");

        train("A same B", "(A <-> B).");
        train("A imply B", "(A ==> B).");
        //train("A not imply B", "(--,(A ==> B)).");

        train("A not a B", "(--,(A --> B)).");
        train("A not same B", "(--,(A <-> B)).");

        train("A or B", "(--,((--,A) && (--,B))).");
        train("A and B", "(A && B).");
        train("A, B, and C", "(&&,A,B,C).");
        train("A, B, C, and D", "(&&,A,B,C,D).");

        train("A and not B", "(A && (--,B)).");
        train("not A and not B", "((--,A) && (--,B)).");

    }


    private void train(String natural, String narsese) {
        final int maxVars = 6;
        for (int i = 0; i < maxVars; i++) {
            String v = String.valueOf((char) ('A' + i));
            narsese = narsese.replaceAll(v, "%" + v);
        }

        Task t = Narsese.the().task(narsese, terminal);

        Term pattern = index.get(t.term(), true).term();

        rules.add((tt, freq, conf, tense) -> {
            if (timeMatch(t, tense)) {
                if (Math.abs(t.freq() - freq) < 0.33f) {
                    if (Math.abs(t.conf() - conf) < 0.33f) {

                        final String[] result = {null};

                        FindSubst u = new FindSubst(terminal.concepts, VAR_PATTERN, terminal.random) {

                            @Override
                            public boolean onMatch() {


                                final String[] r = {natural};
                                xy.forEach((x, y) -> {
                                    String var = x.toString();
                                    if (!var.startsWith("%"))
                                        return;
                                    var = String.valueOf(((char) (var.charAt(1) - '1' + 'A'))); //HACK conversion between normalized pattern vars and the vars used in the input pattern
                                    r[0] = r[0].replace(var, y.toString());
                                });

                                result[0] = r[0];
                                return false; //only the first match
                            }
                        };

                        u.unifyAll(pattern, tt);

                        if (result[0]!=null)
                            return result[0];

                    }
                }
            }
            return null;
        });

    }

    private boolean timeMatch(Task t, Tense tense) {
        return t.isEternal() && tense == Tense.Eternal;
        //TODO non-eternal case
    }

    /*public String toString(Term x, boolean tru) {
        return x.toString();
    }*/

    public String toString(Term x, float freq, float conf, Tense tense) {
        for (Rule r : rules) {
            String y = r.get(x, freq, conf, tense);
            if (y != null)
                return y;
        }
        //DEFAULT
        return x.toString();
    }

    public String toString(Task x) {
        return toString(x.term(), x.freq(), x.conf(), x.isEternal() ? Tense.Eternal : Tense.Present /* TODO */);
    }


}
