package nars.nlp;

import com.google.common.base.Joiner;
import nars.NAR;
import nars.Op;
import nars.Param;
import nars.nar.Default;
import nars.op.Command;
import nars.op.Operator;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.obj.IntTerm;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import static nars.term.Terms.compoundOrNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by me on 2/18/17.
 */
public class NLPOperatorTest {

    @Test
    public void testProductSlice() {
        //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
        //_.slice(array, [start=0], [end=array.length])

        Param.DEBUG = true;

        NAR n = new Default();

        //slice(<compound>,<selector>)
        //  selector :-
        //      a specific integer value index, from 0 to compound size
        //      (a,b) pair of integers, a range of indices

        n.on("quote", (args) -> {
            throw new RuntimeException("quote should never actually be invoked by the system");
        });
        n.on("slice", (args) -> {
            if (args.length > 1) {
                Compound x = compoundOrNull(args[0]);
                if (x != null) {
                    switch (args.length) {
                        case 2:
                            //specific index
                            Term index = args[1];
                            if (index.op() == Op.INT) {
                                int i = ((IntTerm) index).val;
                                return x.term(i);
                            }
                            break;
                        case 3:
                            //TODO
                            break;
                    }

                }
            }
            return null;
        });
        n.on("assertEquals", new Command() {
            @Override public void run(@NotNull Atomic op, @NotNull Term[] args, @NotNull NAR nar) {
                String msg = op + "(" + Joiner.on(',').join(args) + ')';
                assertEquals(msg, 2, args.length);
                assertEquals(msg, args[0], args[1]);
            }
        });

        n.log();
        n.input("(slice((a,b,c),2)).");
        n.input("(quote(x)).");
        n.input("quote(x);");
        n.input("log(quote(x));");
        n.input("assertEquals(c, c);");
        n.input("assertEquals(x, quote(x));");
        n.input("assertEquals(c, slice((a,b,c),2));");
        n.input("assertEquals(quote(slice((a,b,c),$x)), slice((a,b,c),$x));");
        n.run(5);


    }

}
