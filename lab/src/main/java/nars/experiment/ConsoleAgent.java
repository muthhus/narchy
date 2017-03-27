package nars.experiment;

import com.googlecode.lanterna.TextCharacter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import nars.*;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.term.Term;
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

/**
 * executes a unix shell and perceives the output as a grid of symbols
 * which can be interactively tagged by human, and optionally edited by NARS
 */
public abstract class ConsoleAgent extends NAgentX {

    final ConcurrentLinkedDeque<Task> queue = new ConcurrentLinkedDeque<Task>();


    final ConsoleTerminal Rlabel = Vis.newInputEditor();

    final TestConsole R = new TestConsole(
            $.the("it"),
            true, false,
            8, 8);
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
                true, true,
                8, 8);

        //SpaceGraph.window(new VSplit(Rlabel, R, 0.1f), 800, 600);
        //SpaceGraph.window(new VSplit(label("context"), Rlabel, 0.1f), 800, 600);
        //SpaceGraph.window(new LabeledPane("ctx", Rlabel), 400, 200);
        SpaceGraph.window(Rlabel, 400, 200);
        SpaceGraph.window(R, 600, 600);
        SpaceGraph.window(W, 600, 600);

        //Wmodel.setBacklogSize(0);
        //Wmodel.textBox.setReadOnly(false);

    }


    @Override
    abstract protected float act();

    public static void main(String[] args) {
        Default n = NARBuilder.newMultiThreadNAR(3, new RealTime.DSHalf(true).durSeconds(0.1f));
        n.setSelf("I");
        //n.logBudgetMin(System.out, 0.25f);
        //n.log();

        @NotNull ConsoleAgent a = new ConsoleAgent(n) {
            @Override
            protected float act() {
                return 0;
            }
        };

        NAgentX.chart(a);

        a.runRT(0);
    }

    private class TestConsole extends ConsoleSurface {

        final char[][] chars;
        final Term terms[][];
        private final boolean read;
        int c[] = new int[2];
        private boolean write;


        public TestConsole(Term id, boolean read, boolean write, int w, int h) {
            super(w, h);
            this.chars = new char[w][h];

            terms = new Term[w][h];
            for (int x = 0; x< w; x++) {
                for (int y = 0; y < h; y++) {
                    terms[x][y] = $.prop($.p($.the(x), $.the(y)), id);
                }
            }
            c[0] = 0;
            c[1] = 0;
            this.read = read;
            this.write = write;

            if (write) {
                actionTriState($.inh($.p("cursor", "x"), id), (d) -> {
                    switch (d) {
                        case -1:
                            left();
                            break;

                        case +1:
                            right();
                            break;
                    }
                });
                actionTriState($.inh($.p("cursor", "y"), id), (d) -> {
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
                actionTriState($.inh($.p("write"), nar.self()), (d) -> {
                    switch (d) {
                        case +1:
                            write('*');
                            break;
                        case -1:
                            //Wmodel.gui.handleInput(new com.googlecode.lanterna.input.KeyStroke(KeyType.Backspace));
                            write(' ');
                            break;
                    }

                });
            }

        }

        @Override
        public int[] getCursorPos() {
            return c;
        }


        final TextCharacter space = new TextCharacter(' ');

        @Override
        public TextCharacter charAt(int col, int row) {
            char c = chars[col][row];
            if (c == 0)
                return space;
            else
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
            c[1] = Math.min(chars.length-1, c[1]+1);
        }
        public void right() {
            c[0] = Math.min(chars[0].length-1, c[0]+1);
        }

        public void write(char value) {
            chars[c[0]][c[1]] = value;

            if (read) {
                Term t = terms[c[0]][c[1]];
                Compound cc = (Compound) $.prop( t, $.quote(String.valueOf(value) ));
                input($.belief(cc, 1f, 0.9f).time(now).apply(nar));
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
