package nars.op.java;

import nars.$;
import nars.NAR;
import nars.concept.Concept;
import nars.nal.Tense;
import nars.nar.Default;
import nars.term.Termed;
import nars.util.Texts;

import static java.lang.System.out;

/**
 * Created by me on 4/19/16.
 */
public class SineFollow {

    public float freq = 0.1f;
    public int resolution = 6; //pixels seen
    //final Termed[] pixelTerm;
    int lookahead = 2;

    private final NAR nar;

    public float hidden(int dt) {
        return (float)(Math.sin((dt + time()) * freq) / 2f + 0.5f);
    }
    /** which pixel the hidden value activates currently [0..resolution-1] */
    public int hiddenPixel(int dt) {
        float h = hidden(dt);
        return Math.round(h * (resolution-1));
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
    }

    public Termed term(int i) {
        return nar.term("(" + i + ")");
    }
    void input() {
        int hp = hiddenPixel(0);

        long now = time();
        long future = future();

        for (int i = 0; i < resolution; i++) {
            //float f = i == hp ? 1 : 0;

            float c = 0.9f;

            nar.believe(pixel(i, i == hp ? true : false), Tense.Present, 1f, c);
            nar.believe(pixel(i, i != hp ? true : false), Tense.Present, 0f, c);

            //nar.ask(p, '?', future);
        }
    }

    private Termed pixel(int i, boolean on) {
        return $.p("p" + i, on ? "1" : "0");
    }

    void evaluate() {
        String e = "";

        long now = time();
        long future = future();
        int hp = hiddenPixel(lookahead);


        float best = -1;
        float bestVal = 0;

        float estimated = 0;
        float estDen = 0;

        for (int i = 0; i < resolution; i++) {
            Concept off = nar.concept(pixel(i, false));
            Concept on = nar.concept(pixel(i, true));

            //float eNow = c.belief(now).expectation();
            float onNext = on!=null && on.hasBeliefs() ? on.belief(future).expectation() : 0;
            float offNext = off!=null && off.hasBeliefs() ? off.belief(future).expectation() : 0;
            float delta = (onNext) / (offNext + onNext);
            //float delta = eNext - eNow;
            //e += Texts.n2(eNow) + "+-" + Texts.n4(delta) + "\t";
            e += (hp == i ? "*":"_") +
                    //Texts.n1(onNext) + ":" + Texts.n1(offNext) +
                    Texts.n1(delta)
                    + "\t\t";

            estimated += delta * i;
            estDen += delta;
            if (delta > bestVal) {
                bestVal = delta;
                best = i;
            }
        }

        if (estDen > 0) {
            estimated /= estDen;
        } else {
            estimated = -1;
        }


        boolean print = true, printData = false;

        if (printData) {
            out.println(now + ":\t\t" + e + " " + estimated);
        }

        if ( print) {


                int cols = 50;
                int colActual = (int) Math.round(cols * hidden(lookahead));
                int colEst = (int) Math.round(
                        //cols * estimated
                        cols * best / resolution
                );
                for (int i = 0; i <= cols; i++) {

                    char c;
                    if (i == colActual && colActual == colEst) {
                        c = '@'; //win
                    } else if (i == colActual) {
                        c = '#';
                    } else if (i == colEst) {
                        c = '|';
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
        Default d = new Default();
        d.cyclesPerFrame.set(128);
        //d.log();

        new SineFollow(d, 128);
    }
}
