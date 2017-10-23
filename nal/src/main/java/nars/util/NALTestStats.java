package nars.util;

import nars.control.MetaGoal;
import org.junit.jupiter.api.extension.*;

public class NALTestStats implements AfterEachCallback, AfterAllCallback {

//    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
//            .create("nars", NALTestStats.class.getSimpleName());

    final MetaGoal.Report metagoals = new MetaGoal.Report();




    @Override
    public void afterAll(ExtensionContext context) throws Exception {

        metagoals.print(System.out);

    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
         NALTest n = ((NALTest)context.getTestInstance().get());

        context.publishReportEntry(context.getUniqueId() + " NAR stats",
                n.nar.stats().toString());

        if (n.metagoals!=null)
            metagoals.add(n.metagoals);
    }
}
