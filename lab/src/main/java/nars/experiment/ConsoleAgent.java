package nars.experiment;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.jogamp.opengl.GL2;
import nars.*;
import nars.gui.Vis;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.widget.console.ConsoleTerminal;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * executes a unix shell and perceives the output as a grid of symbols
 * which can be interactively tagged by human, and optionally edited by NARS
 */
public abstract class ConsoleAgent extends NAgentX {

    final ConcurrentLinkedDeque<Task> queue = new ConcurrentLinkedDeque<Task>();

    final AtomicBoolean record = new AtomicBoolean(false);


    final ConsoleTerminal Rlabel = Vis.newInputEditor();

    /**
     * editable by gui
     */
    final MyConsoleTerminal R = new MyConsoleTerminal(new MyTextEditModel(8, 8, () -> $.the(labelize(((ConsoleTerminal.TextEditModel) (Rlabel.term)).textBox.getText()))));

    final MyTextEditModel Wmodel = new MyTextEditModel(8, 8, () -> nar.self());
    final MyConsoleTerminal W = new MyConsoleTerminal(Wmodel);


    static String labelize(String l) {
        if (l == null || l.isEmpty()) return "sth";
        return l;
    }


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

        //SpaceGraph.window(new VSplit(Rlabel, R, 0.1f), 800, 600);
        //SpaceGraph.window(new VSplit(label("context"), Rlabel, 0.1f), 800, 600);
        //SpaceGraph.window(new LabeledPane("ctx", Rlabel), 400, 200);
        SpaceGraph.window(Rlabel, 400, 200);
        SpaceGraph.window(R, 600, 600);
        SpaceGraph.window(W, 600, 600);

        //Wmodel.setBacklogSize(0);
        //Wmodel.textBox.setReadOnly(false);

        actionTriState($.inh($.p("cursor", "x"), nar.self()), (d) -> {
            int cx = W.cursorX(), cy = W.cursorY();
            switch (d) {
                case -1:
                    ((MyTextEditModel) W.term).addInput(new KeyStroke(KeyType.ArrowLeft));
                    break;

                case +1:
                    ((MyTextEditModel) W.term).addInput(new KeyStroke(KeyType.ArrowRight));
                    break;

            }
        });
        actionTriState($.inh($.p("cursor", "y"), nar.self()), (d) -> {
            int cx = W.cursorX(), cy = W.cursorY();
            switch (d) {
                case -1:
                    ((MyTextEditModel) W.term).addInput(new KeyStroke(KeyType.ArrowDown));
                    break;
                case +1:
                    ((MyTextEditModel) W.term).addInput(new KeyStroke(KeyType.ArrowUp));
                    break;

                //case +1: Wmodel.setCursorPosition(cx, Math.min(Wmodel.getTerminalSize().getRows()-2, cy+1) ); break;
            }
        });
        actionTriState($.inh($.p("write"), nar.self()), (d) -> {
            switch (d) {
                case +1:
                    ((MyTextEditModel) W.term).inject('*');
                    break;
                case -1:
                    //Wmodel.gui.handleInput(new com.googlecode.lanterna.input.KeyStroke(KeyType.Backspace));
                    ((MyTextEditModel) W.term).addInput(new KeyStroke(KeyType.Delete));
                    ((MyTextEditModel) W.term).inject(' ');
                    break;
            }

        });

        record.set(true);
    }


    @Override
    abstract protected float act();

    public static void main(String[] args) throws Narsese.NarseseException {
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

    private class MyConsoleTerminal extends ConsoleTerminal {

        private final Supplier<Term> id;

        public MyConsoleTerminal(MyTextEditModel t) {
            super(t);
            this.id = t.label;
        }

        @Override
        protected boolean setBackgroundColor(GL2 gl, TextCharacter c, int col, int row) {
            float cc = nar.pri( ((MyTextEditModel)term).pixelTerm(col, row) );
            if (cc == cc) {
                float s = 0.3f * cc;
                gl.glColor4f(s, 0, 0, 0.95f);
                return true;
            }
            return false;
        }
    }

    private class MyTextEditModel extends ConsoleTerminal.TextEditModel {

        private final Supplier<Term> label;

        public MyTextEditModel(int cols, int rows, Supplier<Term> label) {
            super(cols, rows);
            this.label = label;
        }

        @Override
        public void run() {
            super.run();
            //textBox.setCaretWarp(true);
        }

        public void inject(char c) {
            super.putCharacter(c);
        }

        @Override
        public void putCharacter(char c) {

            super.putCharacter(c);

            if (record.get()) {
                TerminalPosition p = getCursorPosition();
                Compound cc = (Compound) $.prop( pixelTerm(p), $.quote(String.valueOf(c) ));
                input($.belief(cc, 1f, 0.9f).time(now).apply(nar));
            }
        }

        public @NotNull Compound pixelTerm(TerminalPosition p) {
            //TODO cache this
            int x = p.getColumn();
            int y = p.getRow();
            return pixelTerm(x, y);
        }

        public @NotNull Compound pixelTerm(int x, int y) {
            return $.p(label.get(), $.the(x), $.the(y));
        }
    }

}
