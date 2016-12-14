package objenome;

import org.mockito.invocation.DescribedInvocation;
import org.mockito.listeners.InvocationListener;
import org.mockito.listeners.MethodInvocationReport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/** for use with Mockito */
public class InvocationLogger implements InvocationListener {
    private Logger logger;

    private int mockInvocationsCounter = 0;

    public InvocationLogger() {
    }
    public InvocationLogger(Logger logger) {
        this.logger = logger;
    }
    public InvocationLogger(String name) {
        this.logger = LoggerFactory.getLogger(name);
    }
    public InvocationLogger(Class c) {
        this.logger = LoggerFactory.getLogger(c);
    }

    @Override
    public void reportInvocation(MethodInvocationReport methodInvocationReport) {

        DescribedInvocation invocation = methodInvocationReport.getInvocation();
        //Object[] args = ((InvocationImpl) invocation).getArguments();

        if (logger == null)
             logger = LoggerFactory.getLogger(invocation.getLocation().toString());

        mockInvocationsCounter++;

//            printStream.println("############ Logging method invocation #" + mockInvocationsCounter + " on mock/spy ########");
//            if (methodInvocationReport.getLocationOfStubbing() != null) {
//                printlnIndented("stubbed: " + methodInvocationReport.getLocationOfStubbing());
//            }

        //printStream.println(invocation.toString());

        //printlnIndented("invoked: " + invocation.getLocation().toString());
        if (methodInvocationReport.threwException()) {
            Throwable thrown = methodInvocationReport.getThrowable();
            //String message = thrown.getMessage() == null ? "" : " with message " + thrown.getMessage();
            //printlnIndented("has thrown: " + thrown.getClass() + message);
            logger.warn("call {} {} throw \"{}\" ({})", invocation, mockInvocationsCounter, thrown, thrown.getClass());
        } else {
            Object type = (methodInvocationReport.getReturnedValue() == null) ? null : methodInvocationReport.getReturnedValue().getClass();
            //printlnIndented("has returned: \"" + methodInvocationReport.getReturnedValue() + "\"" + type);
            logger.info("call {} {} return \"{}\" ({})", invocation, mockInvocationsCounter, methodInvocationReport.getReturnedValue(), type);
        }
        System.out.println(Arrays.toString(Thread.currentThread().getStackTrace()));

        //printStream.println("");



    }

}
