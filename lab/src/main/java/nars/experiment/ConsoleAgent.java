package nars.experiment;

import com.googlecode.lanterna.TextCharacter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import jcog.Util;
import nars.*;
import nars.concept.ActionConcept;
import nars.concept.GoalActionConcept;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.widget.console.ConsoleSurface;
import spacegraph.widget.console.ConsoleTerminal;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

import static nars.Op.BELIEF;

/**
 * executes a unix shell and perceives the output as a grid of symbols
 * which can be interactively tagged by human, and optionally edited by NARS
 */
public abstract class ConsoleAgent extends NAgentX {

    final ConcurrentLinkedDeque<Task> queue = new ConcurrentLinkedDeque<Task>();


    final ConsoleTerminal Rlabel = Vis.newInputEditor();

    final TestConsole R = new TestConsole(
            $.the("it"),
            true,
            4, 1);
    final TestConsole W;





    protected void input(Task t) {
        queue.add(t);
    }

    @Override
    protected Stream<Task> nextInput(long when) {
        List<Task> q = $.newArrayList(queue.size());
        Iterator<Task> qq = queue.iterator();
        while (qq.hasNext()) {
            q.add(qq.next());
            qq.remove();
        }

        return Stream.concat(
                super.nextInput(when),
                q.stream()
        );
    }

    public ConsoleAgent(NAR nar) {
        super("term", nar);

        //senseNumberDifference( $.func("cursor", R., $.the("x")), () -> R.term.getCursorPosition().getColumn());
        //senseNumberDifference( $.func("cursor", $.the("term"), $.the("y")), () -> R.term.getCursorPosition().getRow());

//        new TaskRule("(term(%1,(%2,%3)) && term(%4,(%5,%3)))",
//                "((seq(%1,%4)-->(%2,%3)) && equal(sub(%5,%2),1))", nar) {
//            @Override
//            public boolean test(@NotNull Task task) {
//                return true;
//            }
//        };
//        nar.input("(term(f,(3,1)) && term(f,(4,1))).");
//        nar.input("(term(f,(3,1)) && term(f,(4,1))).");

        W = new TestConsole(
                nar.self(),
                true,
                R.width(), R.height()).write('a','b',' ');

        //SpaceGraph.window(new VSplit(Rlabel, R, 0.1f), 800, 600);
        //SpaceGraph.window(new VSplit(label("context"), Rlabel, 0.1f), 800, 600);
        //SpaceGraph.window(new LabeledPane("ctx", Rlabel), 400, 200);
        SpaceGraph.window(Rlabel, 400, 200);
        SpaceGraph.window(R, 600, 600);
        SpaceGraph.window(W, 600, 600);

        senseNumberDifference($.func((Atomic)id, $.the("joy")), happy);

    }


    @Override
    abstract protected float act();

    public static void main(String[] args) {
        Default n = NARBuilder.newMultiThreadNAR(4,
                new RealTime.CS(true).durSeconds(0.1f));
        n.setSelf("I");
        //n.logBudgetMin(System.out, 0.25f);
        //n.log();

        @NotNull ConsoleAgent a = new ConsoleAgent(n) {
            @Override
            protected float act() {
                //copy
                return  (Util.sqr(similarity(R.chars, W.chars) ) );
            }
        };

        NAgentX.chart(a);

        a.runRT(0);
    }

    private static float similarity(char[][] a, char[][] b) {
        int total = 0, equal = 0;
        for (int j = 0; j < a[0].length; j++) {
            for (int i = 0; i < a.length; i++) {
                equal+= (a[i][j] == b[i][j]) ? 1 : 0;
                total++;
            }
        }
        return (equal)/((float)total);
    }

    private class TestConsole extends ConsoleSurface {

        final char[][] chars;
        final Term terms[][];
        private final boolean read;
        int c[] = new int[2];
        private boolean write;


        public TestConsole(Term id, boolean read, int w, int h) {
            super(w, h);
            this.chars = new char[w][h];
            this.terms = new Term[w][h];
            for (int x = 0; x< w; x++) {
                for (int y = 0; y < h; y++) {
                    chars[x][y] = ' ';
                    terms[x][y] = $.inst($.p($.the(x), $.the(y)), id);
                }
            }
            c[0] = 0;
            c[1] = 0;
            this.read = read;


        }

        public TestConsole write(char... vocabulary) {
            write = true;
                actionTriState($.func("cursor", $.the("x"), id), (d) -> {
                    switch (d) {
                        case -1:
                            left();
                            break;

                        case +1:
                            right();
                            break;
                    }
                });
                actionTriState($.func("cursor", $.the("y"), id), (d) -> {
                    switch (d) {
                        case -1:
                            up();
                            break;
                        case +1:
                            down();
                            break;

                        //case +1: Wmodel.setCursorPosition(cx, Math.min(Wmodel.getTerminalSize().getRows()-2, cy+1) ); break;
                    }
                });
                for (char c : vocabulary) {
                    Compound ct = $.func($.the("write"), $.quote(String.valueOf(c)), id);

//                    ActionConcept m = new GoalActionConcept(ct, nar(), (b, d) -> {
//                        boolean next = d != null && d.expectation() > 0.75f;
//                        if (next) {
//                            write(c);
//                            return $.t(1f, nar.confidenceDefault(BELIEF));
//                        }
//                        return $.t(0f, nar.confidenceDefault(BELIEF));
//                    });
//                    actions().add(m);

                    actionToggle(ct, d -> {
                        if (d) write(c);
                    });
                }

            return this;
        }

        @Override
        public int[] getCursorPos() {
            return c;
        }


        //final TextCharacter space = new TextCharacter(' ');

        @Override
        public TextCharacter charAt(int col, int row) {
            char c = chars[col][row];
            return new TextCharacter(c);
        }

        @Override
        public Appendable append(char c) throws IOException {
            //ignore
            return this;
        }

        @Override
        public Appendable append(CharSequence csq) throws IOException {
            //ignore
            return this;
        }

        @Override
        public Appendable append(CharSequence csq, int start, int end) throws IOException {
            //ignore
            return this;
        }


        @Override
        protected boolean setBackgroundColor(GL2 gl, TextCharacter c, int col, int row) {
            float cc = nar.pri( terms[col][row] );
            if (cc == cc) {
                float s = 0.3f * cc;
                gl.glColor4f(s, 0, 0, 0.95f);
                return true;
            }
            return false;
        }

        public void left() {
            c[0] = Math.max(0, c[0]-1);
        }
        public void up() {
            c[1] = Math.max(0, c[1]-1);
        }
        public void down() {
            c[1] = Math.min(chars[0].length-1, c[1]+1);
        }
        public void right() {
            c[0] = Math.min(chars.length-1, c[0]+1);
        }

        public void write(char value) {
            char prev = chars[this.c[0]][this.c[1]];

            chars[this.c[0]][this.c[1]] = value;

            if (read) {
                Term t = terms[this.c[0]][this.c[1]];
                if (prev!=value) {
                    input($.belief((Compound)$.prop( t, $.quote(String.valueOf(prev))), 0f, 0.9f).time(now-nar.dur()).apply(nar));
                }
                input($.belief((Compound)$.prop( t, $.quote(String.valueOf(value) )), 1f, 0.9f).time(now).apply(nar));
            }
        }

        @Override
        public boolean onKey(KeyEvent e, boolean pressed) {
            if (write) return false; //ignore own

            if (pressed) {
                if (!e.isPrintableKey()) {
                    switch (e.getKeyCode()) {
                        case KeyEvent.VK_DOWN:
                            down();
                            return true;
                        case KeyEvent.VK_UP:
                            up();
                            return true;
                        case KeyEvent.VK_LEFT:
                            left();
                            return true;
                        case KeyEvent.VK_RIGHT:
                            right();
                            return true;
                    }
                    return false;
                } else {
                    char c = e.getKeyChar();
                    write(c);
                    return true;
                }
            }


            return false;

        }

        public int height() {
            return chars[0].length;
        }
        public int width() {
            return chars.length;
        }
    }

//    private class MyTextEditModel extends ConsoleTerminal.TextEditModel {
//
//        private final Supplier<Term> label;
//
//        public MyTextEditModel(int cols, int rows, Supplier<Term> label) {
//            super(cols, rows);
//            this.label = label;
//        }
//
//        @Override
//        public void run() {
//            super.run();
//            //textBox.setCaretWarp(true);
//        }
//
//        public void inject(char c) {
//            super.putCharacter(c);
//        }
//
//        @Override
//        public void putCharacter(char c) {
//
//            super.putCharacter(c);
//
//            if (record.get()) {
//                TerminalPosition p = getCursorPosition();
//                Compound cc = (Compound) $.prop( pixelTerm(p), $.quote(String.valueOf(c) ));
//                input($.belief(cc, 1f, 0.9f).time(now).apply(nar));
//            }
//        }
//
//        public @NotNull Compound pixelTerm(TerminalPosition p) {
//            //TODO cache this
//            int x = p.getColumn();
//            int y = p.getRow();
//            return pixelTerm(x, y);
//        }
//
//        public @NotNull Compound pixelTerm(int x, int y) {
//            return $.p(label.get(), $.the(x), $.the(y));
//        }
//    }

}