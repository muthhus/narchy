package nars.analyze;

import com.google.common.base.Joiner;
import nars.$;
import nars.NAR;
import nars.Narsese;
import nars.nar.NARS;
import nars.term.Compound;
import org.jetbrains.annotations.NotNull;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by me on 9/10/15.
 */
public class TestAggregator extends RunListener {


    private final NAR nar;

    String testName;
    @NotNull
    Set<Description> success = new HashSet();

    @Override
    public void testRunStarted(Description description) throws Exception {
        testName = "nartest" + description.getTestClass() + "_" + System.currentTimeMillis();
    }

    @Override
    public void testRunFinished(Result result) throws Exception {
        for (Description d : success) {
            describe(d, true);
        }
    }

    @Override
    public void testStarted(@NotNull Description d) throws Exception {

        String si = '<' + getDescriptionTerm(d) + " --> " + testName + ">.";
        nar.input(si);
    }


    @Override
    public void testFinished(Description d) throws Exception {

        success.add(d);

        //System.err.println("finished: " + d.getAnnotations());

        //System.out.println(failure);
        //System.out.println(JSON.stringFrom(failure));
    }

    @NotNull
    public String getDescriptionTerm(@NotNull Description d) {
        String[] meth = d.getMethodName().split("[\\[\\]]");
        String m = Joiner.on("\",\"").join(meth);

        return "{\"" +d.getTestClass().getSimpleName() /*.replace(".",",")*/
                + "\",\"" + m + "\"}";
    }

    protected void describe(@NotNull Description d, boolean success) throws Narsese.NarseseException {
        Compound x = $.negIf($.func("ok", getDescriptionTerm(d)), success);
        System.out.println(x);
        nar.believe(x);
    }

    @Override
    public void testFailure(@NotNull Failure failure) throws Exception {

        Description d = failure.getDescription();

        success.remove(d);

        describe(d, false);

        //System.out.println(failure);
        //System.out.println(JSON.stringFrom(failure));
    }

    @Override
    public void testAssumptionFailure(Failure failure) {
    }

    @Override
    public void testIgnored(Description description) throws Exception {
    }

    public TestAggregator(NAR nar, @NotNull String... classnames) {

        this.nar = nar;

        JUnitCore core = new JUnitCore();

        core.addListener(this);
        for (String c : classnames) {
            try {
                core.run(Class.forName(c));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

    }
    public static void main(String[] args) throws Narsese.NarseseException {
        //this.activeTasks = activeTasks;
        NAR da = new NARS().get();
        //da.memory.realTime();
        NAR nar = da;
        nar.log();

        //nar.input("<?x --> [fail]>?");
        //nar.input("<?x --> [ok]>?");
        nar.input("<(--,ok($x)) ==> fix($x)>.");


//        nar.input("<{nal1,nal2,nal3,nal4} --> nal>.");
//        nar.input("<nars --> [nal]>.");

        //new Thread( () -> {
            new TestAggregator(nar, "nars.nal.nal1.NAL1Test");
        //}).start();
        //new Thread( () -> {
            new TestAggregator(nar, "nars.nal.nal2.NAL2Test");
        //}).start();

//        new NARStream(nar).forEachFrame(() -> {
//            //System.out.println(new MemoryBudget(nar));
//        });

        //TextOutput.out(nar).setOutputPriorityMin(0.5f);

        for (int i = 0; i < 100; i++) {
            nar.run(100);
            //System.out.println(new MemoryBudget(nar));
        }


    }
}
