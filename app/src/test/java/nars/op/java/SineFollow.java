package nars.op.java;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.Op;
import nars.concept.Concept;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.Termed;
import nars.truth.TruthWave;
import nars.util.Texts;
import org.jetbrains.annotations.NotNull;

import static java.lang.System.out;

/**
 * Created by me on 4/19/16.
 */
public class SineFollow {

    public float freq = 0.04f;
    public int resolution = 6; //pixels seen

    float fadeFactor = 0;

    //final Termed[] pixelTerm;
    int lookahead = (int)Math.round(Math.PI * 2f + (1f / freq) * 0.6f  /* phase */);

    private final NAR nar;

    public float hidden(int dt) {
        return (float)(Math.sin((dt + time()) * freq) / 2f + 0.5f);
    }
    /** which pixel the hidden value activates currently [0..resolution-1] */
    public int hiddenPixel(int dt) {
        float h = hidden(dt);
        return pixel(h);
    }

    public int pixel(float v /* 0..1.0 */) {
        return Math.round(v * (resolution-1));
    }

    public float unpixel(float v /* 0..1.0 */) {
        return v / (resolution-1f);
    }

    long time() { return nar.time(); }

    public SineFollow(NAR n, int cycles) {
        this.nar = n;

        //pixelTerm = (Termed[]) IntStream.range(0, resolution).mapToObj(i -> term(i)).toArray(r -> new Termed[r]);

        for (int i = 0; i < cycles; i++) {
            input();
            n.step();
            evaluate();
        }

        //report beliefs
        for (int i = 0; i < resolution; i++) {
            Concept c = n.concept( pixel(i, true ) );
            c.print();
            TruthWave tw = new TruthWave(c.beliefs(), n);
            System.out.println(tw);

        }
    }

    public Termed term(int i) {
        return nar.term("(" + i + ")");
    }
    void input() {
        int hp = hiddenPixel(0);

        long now = time();

        if (!inputs(now)) {
            ///System.out.println("--");
            return;
        }

        for (int i = 0; i < resolution; i++) {
            //float f = i == hp ? 1 : 0;


            float c = 0.95f;

            nar.believe(pixel(i, i == hp), Tense.Present, 1f, c);
            //nar.believe(pixel(i, i != hp), Tense.Present, 0f, c);


            //nar.ask(pixel(i, true), '?', now + lookahead);
            //nar.ask(pixel(i, false), '?', time() + lookahead);
        }
    }

    boolean inputs(long time) {
        if (fadeFactor == 0) return true;
        else
            return Math.random() < 1f / (1f + (time / fadeFactor));
    }

    private Termed pixel(int i, boolean on) {
        //return $.p("p" + i, on ? "1" : "0");

        Compound oo = $.p("p" + i);
        return on ? oo : $.neg(oo);

        /*return
                $.inh
                //$.prop
                    ($.the(i),$.the(on ? "Y" : "N"));*/
    }

    void evaluate() {
        //String e = "";

        long now = time();
        long future = future();
        //int hp = hiddenPixel(lookahead);


        int best = -1;
        float bestVal = Float.NEGATIVE_INFINITY;

        //float estimated = 0;
        //float estDen = 0;

        for (int i = 0; i < resolution; i++) {
            //Concept off = nar.concept(pixel(i, false));
            Concept on = nar.concept(pixel(i, true));

            //float eNow = c.belief(now).expectation();
            float onNext = on!=null && on.hasBeliefs() ? on.belief(future, now).expectation() : 0;
            if (onNext < 0.5f) continue;

            //float offNext = off!=null && off.hasBeliefs() ? off.belief(future, now).expectation() : 0;
            //float offNext = 0;

            //float denom = (offNext + onNext);
            /*if (denom > 0)*/ {
                float delta = (onNext);// - offNext);// / denom;
                //float delta = eNext - eNow;
                //e += Texts.n2(eNow) + "+-" + Texts.n4(delta) + "\t";
            /*e += (hp == i ? "*":"_") +
                    //Texts.n1(onNext) + ":" + Texts.n1(offNext) +
                    Texts.n1(delta)
                    + "\t\t";*/


                //estimated += delta * i;
                //estDen += delta;

                if (delta > bestVal) {
                    bestVal = delta;
                    best = i;
                }

            }
        }

        /*if (estDen > 0) {
            estimated /= estDen;
        } else {
            estimated = -1;
        }*/


        boolean print = true, printData = false , extra = true;

        if (printData) {
            //out.println(now + ":\t\t" + e + " " + estimated);
        }

        if ( print) {


                int cols = 80;
                int colNow = Math.round(cols * hidden(0));
                int colFuture = Math.round(cols * hidden(lookahead));

                //int colWeightEst = Math.round(cols * unpixel(estimated) );
                int colBestEst = Math.round(cols * unpixel(best) );

                for (int i = 0; i <= cols; i++) {
                    char c;
                    if (i == colFuture && colFuture == colBestEst) {
                        c = '@'; //win
                    } else if (i == colFuture) {
                        c = '#';
                    } else if (i == colBestEst) {
                        c = '|';
                    /*} else if (extra && i == colWeightEst) {
                        c = ':';
                    }*/ } else if (extra && i == colNow) {
                        c = '+';
                    } else {
                        c = '.';
                    }

                    out.print(c);
                }

                /*out.print(" \t<:" + n2(diffness.get())  +
                        " \t:" + n2(move.motivation(n)));*/
                out.println();
            }

    }

    private long future() {
        return time() + lookahead /** lookahead */;
    }

    public static void main(String[] args) {
        Global.DEBUG = true;

        Default d = new Default(1024, 8, 3, 2);
        d.cyclesPerFrame.set(8);
        d.shortTermMemoryHistory.set(2);
//        d.log();
//        d.eventTaskProcess.on(tt -> {
//            if (tt.lastLogged().toString().equals("Immediaternalized"))
//                return; //skip immediaternalize
//
//           if (tt.isEternal() && tt.op()!= Op.IMPLICATION) {
//               System.err.println(tt.explanation());
//               System.err.println();
//           }
//        });

        new SineFollow(d, 1560);


    }
}
