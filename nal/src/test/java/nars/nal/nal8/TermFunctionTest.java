package nars.nal.nal8;

import nars.Global;
import nars.nal.nal8.operator.TermFunction;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.TermIndex;
import nars.term.variable.Variable;
import nars.util.signal.MotorConcept;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;


public class TermFunctionTest {

    @Test
    public void testAdd1() {
        Default d = new Default();
        d.log();
        d.input("add(1,2,#x)!");
        d.run(16);
        d.input("add(3,4,#x)!");
        d.run(16);
    }

    @Test
    public void testAdd1Temporal() {
        Default d = new Default();
        d.log();
        d.input("add(1,2,#x)! :|:");
        d.run(16);
        d.input("add(3,4,#x)! :|:");
        d.run(16);
    }

//    @Test
//    public void testExecutionREsultIsCondition() {
//        Default d = new Default();
//        d.log();
//        d.input("(add(#x,1,#y) <=> inc(#x,#y)).");
//        d.input("add(1,2,#x)! :|:");
//        d.run(16);
//        d.input("add(3,4,#x)! :|:");
//        d.run(16);
//    }

}
