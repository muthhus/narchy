package nars.util.experiment;

import nars.$;
import nars.Global;
import nars.NAR;
import nars.nar.Default;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atom;
import nars.util.signal.TestNAR;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;


public class DeductiveMeshTest {

    @NotNull
    public final Compound q;
    @NotNull
    public final List<Compound> coords;





    public DeductiveMeshTest(@NotNull NAR n, int[] dims, int timeLimit) {
        this(new TestNAR(n), dims, timeLimit);
    }

    public DeductiveMeshTest(@NotNull TestNAR n, int[] dims, int timeLimit) {

        if (dims.length!=2)
            throw new UnsupportedOperationException("2-D only implemented");

        coords = Global.newArrayList();
        for (int x = 0; x < dims[0]; x++) {
            for (int y = 0; y < dims[1]; y++) {
                Compound c = c(x, y);
                if (x > 0)
                    n.nar.believe( link(c, c(x-1, y)) );
                if (y > 0)
                    n.nar.believe( link(c, c(x, y-1)) );
                if (x < dims[0]-1)
                    n.nar.believe( link(c, c(x+1, y)) );
                if (y < dims[1]-1)
                    n.nar.believe( link(c, c(x, y+1)) );

            }
        }


        n.nar.ask( q = (Compound) link(c(0,0), c(dims[0]-1, dims[1]-1)));


        n.mustBelieve(timeLimit, q.toString(), 1f, 1f, 0.01f, 1f);

    }

    private Term link(Term a, Term b) {
        return $.prop($.p(a, b), $.the("X"));
    }

    public @NotNull Compound c(int x, int y) {
        return $.p($.the(x), $.the(y));
    }


    @NotNull
    public static Atom a(int i) {
        return Atom.the((byte)('a' + i));
    }


    public static void main(String[] args) {

        Global.DEBUG = true;


        Default n = new Default(1024, 1, 1, 3);
        //n.nal(5);
        n.log();


        n.onFrame(x -> {
            if (n.time() == 1000) {
                NAR.printTasks(n, true);
                NAR.printTasks(n, false);
                System.out.println(Arrays.toString(n.core.active.priHistogram(10)));
            }
        });

        test(n, new int[] { 3, 1 }, 2500);



    }

    static void test(@NotNull NAR n, int[] dims, int cycles) {


        TestNAR testnar = new TestNAR(n);
        DeductiveMeshTest test = new DeductiveMeshTest(testnar, dims, cycles) {
//            @Override
//            public TestNAR mustBelieve(long withinCycles, String term, float confidence, float x, float y, float z) throws InvalidInputException {
//                return this;
//            }
        };

        System.out.print(DeductiveMeshTest.class.getSimpleName() + " test: "
                + test.q + "?\t");

        final long start = System.currentTimeMillis();

//        new AnswerReaction(n) {
//
//            @Override
//            public void onSolution(Task belief) {
//                if (belief.getTerm().equals(test.q)) {
//                    System.out.println(belief + " " + timestamp(start) + " " +
//                            n.concepts().size() + " concepts");
//                    System.out.println(belief.getExplanation());
//                    System.out.println();
//                }
//            }
//        };


        testnar.run(false);


        //n.stdout();
        //n.frame(5000);

        int nc = ((Default) n).core.active.size();
        String ts = timestamp(start);
        long time = n.time();

        //n.stdout();
        //n.frame(55); //to print the ending

        //while (true) {

//        Report report = new Report(test, test.error);
//
//
//
//        test.requires.forEach(report::add);
//
//
//        Report r = report;
//
//        System.out.println(
//                (r.isSuccess() ? "OK" : "ERR") +
//                "\t@" + time + " (" + ts + "ms) " +
//                nc + 'C');


        //TextOutput.out(n).setOutputPriorityMin(0.85f);

//        while (true) {
//
//            n.run(500);
//            //sleep(20);
//
//            if (n.time() % printEvery == 0) {
//                System.out.println(n.time() + " " + timestamp(start) + " " +
//                        n.memory().size());
//            }
//        }


    }

    @NotNull
    private static String timestamp(long start) {
        return (System.currentTimeMillis() - start) + " ms";
    }
}
