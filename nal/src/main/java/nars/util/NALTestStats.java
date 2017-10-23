package nars.util;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class NALTestStats implements BeforeTestExecutionCallback,
        AfterTestExecutionCallback, AfterAllCallback {

//    private static final ExtensionContext.Namespace NAMESPACE = ExtensionContext.Namespace
//            .create("nars", NALTestStats.class.getSimpleName());

    {
        System.out.println("NALTESTSTAT");
    }

    @Override
    public void afterTestExecution(ExtensionContext context) throws Exception {
        System.out.println(context);

    }

    @Override
    public void beforeTestExecution(ExtensionContext context) throws Exception {

    }

    @Override
    public void afterAll(ExtensionContext context) throws Exception {

    }
}
