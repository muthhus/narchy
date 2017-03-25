package nars.experiment;

import com.googlecode.lanterna.TerminalPosition;
import nars.*;
import nars.nar.Default;
import nars.nar.NARBuilder;
import nars.term.Compound;
import nars.term.atom.Atomic;
import nars.time.RealTime;
import nars.util.Loop;
import nars.util.task.TaskRule;
import org.jetbrains.annotations.NotNull;
import spacegraph.SpaceGraph;
import spacegraph.widget.console.ConsoleTerminal;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * executes a unix shell and perceives the output as a grid of symbols
 * which can be interactively tagged by human, and optionally edited by NARS
 */
public class TextConsoleAgent extends NAgentX {

    final ConcurrentLinkedDeque<Task> queue = new ConcurrentLinkedDeque<Task>();

    final AtomicBoolean record = new AtomicBoolean(false);

    final ConsoleTerminal.TextEditModel term = new ConsoleTerminal.TextEditModel( 40, 20) {

        @Override
        public void putCharacter(char c) {

            super.putCharacter(c);

            if (record.get()) {
                TerminalPosition p = term.getCursorPosition();
                Compound cc = $.func((Atomic) id, $.the(String.valueOf(c)),
                        $.p($.the(p.getColumn()), $.the(p.getRow())));
                input($.belief(cc, 1f, 0.9f).time(now).apply(nar));
            }
        }
    };

    final ConsoleTerminal console = new ConsoleTerminal(term);


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

    public TextConsoleAgent(NAR nar) throws Narsese.NarseseException {
        super("term", nar);

        new TaskRule("(term(%1,(%2,%3)) && term(%4,(%5,%3)))",
                "((seq(%1,%4)-->(%2,%3)) && equal(sub(%5,%2),1))", nar) {
            @Override
            public boolean test(@NotNull Task task) {
                return true;
            }
        };
        nar.input("(term(f,(3,#1)) && term(f,(4,#1))).");

        SpaceGraph.window(console, 800, 600);

        record.set(true);
    }

    public void append(String s) {
        synchronized (console) {
            console.append(s);
        }
    }

    @Override
    protected float act() {
        return 0;
    }

    public static void main(String[] args) throws Narsese.NarseseException {
        Default n = NARBuilder.newMultiThreadNAR(2, new RealTime.DSHalf(true).durSeconds(0.1f));
        n.setSelf("I");
        n.logBudgetMin(System.out, 0.25f);

        @NotNull TextConsoleAgent a = new TextConsoleAgent(n);

        NAgentX.chart(a);

        a.runRT(20);
    }

}
