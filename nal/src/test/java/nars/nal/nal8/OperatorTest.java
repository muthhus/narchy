package nars.nal.nal8;

import nars.NAR;
import nars.NARS;
import nars.Narsese;
import nars.Param;
import org.junit.Test;

public class OperatorTest {


    @Test
    public void testSliceAssertEtc() throws Narsese.NarseseException {
        //https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/slice
        //_.slice(array, [start=0], [end=array.length])

        Param.DEBUG = true;

        NAR n = new NARS().get();
        n.log();
        n.input("(slice((a,b,c),2)).");
        n.input("assertEquals(c, slice((a,b,c),add(1,1)));");
        n.input("assertEquals((a,b), slice((a,b,c),(0,2)));");

        //TODO add invalid slice conditions

        n.input("(quote(x)).");
        n.input("log(quote(x));");
        n.input("assertEquals(c, c);");
        n.input("assertEquals(x, quote(x));");
        n.input("assertEquals(c, slice((a,b,c),2));");
        n.input("assertEquals(quote(slice((a,b,c),#x)), slice((a,b,c),#x));");
        n.run(5);
    }

////    @Test public void testOperatorEquality() {
////        assertNotNull( $.oper("echo") );
////        assertEquals( $.oper("echo"), $.oper("echo"));
////        assertNotEquals( $.oper("echo"), $.the("echo")); //echo vs. ^echo
////        //assertNotEquals( $.oper("echo"), $.the("^echo")); //'^echo' vs echo .. this should be disallowed
////    }
//
//    @Test public void testMustExecuteSuccess() {
//
//
//        NAR n = new Default(100, 1, 1, 1);
//        TestNAR t = new TestNAR(n);
//        t.mustExecute(0, 1, "operator");
//
//        n.input("operator()!");
//
//
//    }
//
//    @Test public void testMustExecuteFailure() {
//
//        try {
//            NAR n = new Default(100, 1, 1, 1);
//            TestNAR t = new TestNAR(n);
//            t.mustExecute(0, 1, "operator");
//
//            n.input("xoperator()!");
//
//            assertTrue(false);
//        }
//        catch (AssertionError e) {
//            //failure should occurr
//            assertTrue(true);
//        }
//    }
//
//
////
////    public void testIO(String input, String output) {
////
////        //TextOutput.out(nar);
////
////        nar.mustOutput(16, output);
////        nar.input(input);
////
////        nar.run(4);
////
////    }
////
////    @Test public void testOutputInVariablePosition() {
////        testIO("count({a,b}, #x)!",
////                "<2 --> (/,^count,{a,b},_,SELF)>. :|: %1.00;0.99%");
////    }
//
//    @Test public void testOperationIsInheritance() {
//        Compound o = $.func($.the("x"), $.p("y"));
//        assertEquals(Op.INH, o.op());
//    }

////    @Test public void testTermReactionRegistration() {
////
////        AtomicBoolean executed = new AtomicBoolean(false);
////
////        NAR n = new Default();
////        n.onExecTerm("exe", (Term[] event) -> {
////            //System.out.println("executed: " + Arrays.toString(args));
////            executed.set(true);
////            return null;
////        });
////
////        n.input("exe(a,b,c,#x)!");
////
////        n.run(1);
////
////        assertTrue(executed.get());
////
////    }
//
////    @Test public void testSynchOperator() {
////
////
////        AtomicBoolean executed = new AtomicBoolean(false);
////
////        NAR n = new Default();
////        n.onExec("exe", (exec) -> {
////            executed.set(true);
////        });
////
////        //n.trace();
////        n.input("exe(a,b,c)!");
////
////        n.run(1);
////
////        assertTrue(executed.get());
////
////        assertNotNull("should have conceptualized or linked to ^exe",
////                n.concept("^exe"));
////
////        assertNull("^exe should not conceptualize nor link to atom exe", n.index.get("exe") );
////
////
////    }
//
////    @Ignore @Test public void testCompoundOperator() {
////
////        AtomicBoolean executed = new AtomicBoolean(false);
////
////        NAR n = new Default();
////
////        n.onExec((Term)n.term("<a --> b>"), (exec) -> {
////            executed.set(true);
////        });
////
////        n.input("<a --> b>(a,b,c)!");
////
////        n.frame(1);
////
////        assertTrue(executed.get());
////
////    }
//
////    @Test public void testPatternExecution() {
////        AtomicInteger count = new AtomicInteger();
////
////        PatternOperation f = new PatternOperation("(%A,%B)") {
////            @Override
////            public List<Task> run(Task operationTask, Subst map1) {
////                //System.out.println(this.pattern + " " + operationTask + "\n\t" + map1);
////                count.getAndIncrement();
////                return null;
////            }
////        };
////        Terminal t = new Terminal(16);
////
////        Task matching = t.task("(x,y)!");
////        f.apply(matching);
////
////        assertEquals(1, count.get());
////
////        Task nonMatching = t.task("(x,y,z)!");
////        f.apply(nonMatching);
////
////        //should only be triggered once, by the matching term
////        assertEquals(1, count.get());
////    }
//
////    @Test public void testPatternAnswerer() {
////        AtomicInteger count = new AtomicInteger();
////
////        PatternAnswer f = new PatternAnswer("add(%a,%b,#x)") {
////            @Override
////            public List<Task> run(Task t, @NotNull Subst s) {
////
////                return null;
////            }
////        };
////        Terminal t = new Terminal(16);
////        t.ask($("add(%a,%b,#x)"), ETERNAL, a-> {
////            //TODO
//////            System.out.println(t + " " + s);
//////            assertEquals($("x"), s.term($("%a")));
//////            assertEquals($("y"), s.term($("%b")));
////            count.getAndIncrement();
////            return true;
////        });
////
////        Task matching = t.task("add(x,y,#x)?");
////        f.apply(matching);
////
////        assertEquals(1, count.get()); //should only be triggered once, by the matching term
////
////        Task nonMatching = t.task("add(x)?");
////        f.apply(nonMatching);
////
////        assertEquals(1, count.get()); //should only be triggered once, by the matching term
////
////        Task nonMatching3 = t.task("add(x,y,z)?");
////        f.apply(nonMatching3);
////
////        assertEquals(1, count.get()); //should only be triggered once, by the matching term
////
////        Task nonMatching2 = t.task("add(x,y,$x)?");
////        f.apply(nonMatching2);
////
////        assertEquals(1, count.get()); //should only be triggered once, by the matching term
////    }
//
////    @Ignore
////    @Test public void testPatternAnswererInNAR() {
////        NAR n = new Default(100,1,1,1);
////
////        PatternAnswer addition = new PatternAnswer("add(%a,%b,#x)") {
////            @Nullable
////            final Term A = $("%a"), B = $("%b");
////            @Override public List<Task> run(@NotNull Task question, @NotNull Subst s) {
////                int a = i(s.term(A).toString());
////                int b = i(s.term(B).toString());
////
////                return Lists.newArrayList(
////                        $.task("add(" + a + ',' + b + ',' + Integer.toString(a+b) + ')',
////                                '.', 1.0f, 0.99f)
////                            .eternal()
////                            .parent(question)
////                            .budget(question)
////                            .because("Addition")
////                );
////            }
////        };
////        n.onQuestion(addition);
////        //n.log();
////
////        TestNAR t = new TestNAR(n);
////        n.input("add(1,2,#x)?");
////        n.input("add(1,1,#x)?");
////        t.mustBelieve(8, "add(1, 1, 2)", 1.0f, 0.99f);
////        t.mustBelieve(8, "add(1, 2, 3)", 1.0f, 0.99f);
////        t.test();
////
////        assertEquals(1, n.concept("add(1, 1, 2)").beliefs().size());
////        assertEquals(1, n.concept("add(1, 1, #x)").questions().size());
////        n.concept("add(1, 1, 2)").print(System.out);
////    }
//
//
//
//
////TODO: allow this in a special eval operator
//
////    //almost finished;  just needs condition to match the occurence time that it outputs. otherwise its ok
////
////    @Test
////    public void testRecursiveEvaluation1() {
////        testIO("add( count({a,b}), 2)!",
////                "<(^add,(^count,{a,b},SELF),2,$1,SELF) =/> <$1 <-> 4>>. :|: %1.00;0.90%"
////        );
////    }
////
////    @Test public void testRecursiveEvaluation2() {
////        testIO("count({ count({a,b}), 2})!",
////                "<(^count,{(^count,{a,b},SELF),2},$1,SELF) =/> <$1 <-> 1>>. :|: %1.00;0.90%"
////        );
////    }
}
