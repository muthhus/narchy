package nars.nlp;

import com.google.common.collect.TreeBasedTable;
import jcog.list.FasterList;
import nars.$;
import nars.NAR;
import nars.term.Term;
import nars.truth.Truth;
import nars.truth.TruthAccumulator;
import org.eclipse.collections.api.tuple.Pair;
import org.eclipse.collections.impl.tuple.Tuples;
import org.jetbrains.annotations.Nullable;

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
    private float durationsPerWord, energy;
    private float expectationThreshold = 0.75f;

    public Speech(NAR nar, float durationsPerWord, Consumer<Term> speak) {
        this.durationsPerWord = durationsPerWord;
        this.nar = nar;
        this.speak = speak;
        this.energy = 0;
        nar.onCycle((n) ->{
            energy = Math.min(1f, energy + 1f/(this.durationsPerWord*nar.dur()));
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

        if (when < nar.time() - nar.dur() * durationsPerWord) {
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
        float dur = nar.dur() * durationsPerWord;
        long now = nar.time();
        long startOfNow = now - (int) Math.ceil(dur);
        long endOfNow = now + (int)Math.floor(dur);


        FasterList<Pair<Term, Truth>> pending = new FasterList<>(0);
        synchronized (vocalize) {
            //vocalize.rowKeySet().tailSet(startOfNow-1).clear();
            SortedSet<Long> tt = vocalize.rowKeySet().headSet(endOfNow);
            if (!tt.isEmpty()) {
                tt.forEach(t -> {
                    if (t >= startOfNow) {
                        vocalize.row(t).entrySet().forEach(e -> {
                            Truth x = e.getValue().commitAverage();
                            if (x.expectation() > expectationThreshold)
                                pending.add(Tuples.pair(e.getKey(), x));
                        });
                    }
                    vocalize.row(t).clear();
                });
            }
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
