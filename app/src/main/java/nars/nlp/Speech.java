package nars.nlp;

import com.google.common.collect.TreeBasedTable;
import jcog.Loop;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.nlp.Twenglish;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.time.Tense;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.function.Consumer;

public class Speech {

    public static final Term PREPOSITION = $.the("preposition");
    public static final Term PRONOUN = $.the("pronoun");
    /**
     * when, word, truth
     */
    final TreeBasedTable<Long, Term, TruthAccumulator> vocalize = TreeBasedTable.create();
    private final NAR nar;
    private final Consumer<Term> speak;
    private float cyclesPerWord, energy;
    private float expectationThreshold = 0.75f;

    public Speech(NAR nar, float cyclesPerWord, Consumer<Term> speak) {
        this.cyclesPerWord = cyclesPerWord;
        this.nar = nar;
        this.speak = speak;
        this.energy = 0;
        nar.onCycle((n) ->{
            energy = Math.min(1f, energy + 1f/(this.cyclesPerWord));
            if (energy >= 1f) {
                energy = 0;
                next();
            }
        });
    }

    public void speak(@Nullable Term word, long when, @Nullable Truth truth) {
        if (word == null)
            return;

        //String wordString = word instanceof Atom ? $.unquote(word) : word.toString();

//        if (Twenglish.prepositions.contains(wordString))
//            nar.believe($.instprop(word, PREPOSITION), Tense.Eternal);
//        if (Twenglish.personalPronouns.contains(wordString))
//            nar.believe($.instprop(word, PRONOUN), Tense.Eternal);

        if (when < nar.time()) {
            return;
        }

        TruthAccumulator ta;
        synchronized (vocalize) {
            ta = vocalize.get(when, word);
            if (ta == null) {
                ta = new TruthAccumulator();
                vocalize.put(when, word, ta);
            }
        }

        ta.add(truth);
    }


    public boolean next() {

        //long start = nar.time();
        float dur = 1f/cyclesPerWord;
        long end = Math.round(nar.time() + dur);


        FasterList<Pair<Term, Truth>> pending = new FasterList<>(0);
        synchronized (vocalize) {
            SortedSet<Long> tt = vocalize.rowKeySet().headSet(end);
            tt.forEach(t -> {
                vocalize.row(t).entrySet().forEach(e -> {
                    Truth x = e.getValue().commitAverage();
                    if (x.expectation() > expectationThreshold)
                        pending.add(Tuples.pair(e.getKey(), x));
                });
                vocalize.row(t).clear();
            });
        }
        if (pending.isEmpty())
            return true;


        //TODO decide word..
        Term spoken = decide(pending);
        if (spoken!=null)
            speak.accept(spoken);

//            {
//                //n.believe(tt, Tense.Present);
//                //System.out.println(wordString);
//                bot.send(wordString);
//
//            }

        return true;
    }

    /** default greedy decider by truth expectation */
    @Nullable private Term decide(FasterList<Pair<Term, Truth>> pending) {
        return pending.max((a,b)->{
           float ta = a.getTwo().expectation();
           float tb = b.getTwo().expectation();
           int tab = Float.compare(ta, tb);
           if (ta > tb) {
               return tab;
           } else {
               return a.getOne().compareTo(b.getOne());
           }
        }).getOne();
    }
}
