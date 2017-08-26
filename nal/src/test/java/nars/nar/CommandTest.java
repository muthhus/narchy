package nars.nar;

import nars.*;
import nars.op.DebouncedCommand;
import nars.op.Operator;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.time.Tense;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static nars.Op.COMMAND;
import static org.junit.Assert.*;


@Ignore
public class CommandTest {

    @Test
    public void testEcho() throws Narsese.NarseseException {
        NAR n = new NARS().get();
        AtomicBoolean invoked = new AtomicBoolean();
        n.on("c", (args) -> {
            assertEquals("(x)", args.toString());
            invoked.set(true);
            return null;
        });
        Task t = Narsese.parse().task("c(x);", n);
        assertNotNull(t);
        assertEquals(COMMAND, t.punc());
        assertTrue(t.isCommand());
        assertEquals("c(x);", t.toString());

        n.input(t);
        n.run(1);

        assertTrue(invoked.get());


    }

    @Test
    public void testGoal1() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
//        n.on(new Command((Atom) $.the("x"), n) {
//            @Override
//            public @Nullable Task run(@NotNull Task t, @NotNull NAR nar) {
//                System.out.println(t);
//                return super.run(t, nar);
//            }
//        });
        final int[] count = {0};
        n.on(new DebouncedCommand((Atom) $.the("x"), 2, 0.66f, n) {
            @Override
            public void accept(Task x, NAR nar) {
                System.err.println("INVOKE " + x);
                count[0] ++;
            }
        });
        n.run(1);
        n.input("x(1)! :|:");
        n.input("x(\"too soon\")! :|:"); //<- not invoked because x(1) is
        n.run(1);
        n.run(10);
        n.input("x(3)! :|:");
        n.run(10);
        assertEquals(2, count[0]);
    }

    @Test
    public void testChoose() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.time.dur(10);
        n.on(new DebouncedCommand((Atom) $.the("x"), 1, 0.51f, n) {
            @Override
            public void accept(Task x, NAR nar) {
                Term[] args = Operator.args(x);
                if (args.length > 0) {
                    Term r;
                    if ($.the(1).equals(args[0])) {
                        System.err.println("YES");
                        r = $.the("good");
                    } else if ($.the(0).equals(args[0])) {
                        r = $.the("good").neg();
                    } else {
                        return;
                    }

                    n.believe($.impl(x.term(), r), Tense.Present);
                }

            }
        });
        n.log();
        n.input("x(1)! :|:");
        n.run(4);
        n.input("x(0)! :|:");
        n.run(4);
        n.goal("good");
        n.run(1000);
    }

    @Test
    public void testGoal2() throws Narsese.NarseseException {
        NAR n = NARS.tmp();
        n.on(new DebouncedCommand((Atom) $.the("x"), 2, 0.66f, n) {
            @Override
            public void accept(Task t, NAR nar) {
                Term x = t.term();
                Term[] args = Operator.args(t);
                Term y = $.func("args", args);
                Term xy = $.impl(x, y);
                n.believe(xy, Tense.Present);
            }
        });
        n.log();
        n.run(1);
        n.input("x(1)! :|:");
        n.run(1);
        n.run(10);
        n.input("x(3)! :|:");
        n.run(10);
    }

}
