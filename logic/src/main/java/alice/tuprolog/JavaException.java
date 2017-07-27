package alice.tuprolog;

/**
 * @author Matteo Iuliani
 */
public class JavaException extends Throwable {
	private static final long serialVersionUID = 1L;
    // eccezione Java che rappresenta l'argomento di java_throw/1
    private final Throwable e;

    public JavaException(Throwable e) {
        this.e = e;
    }

    public Struct getException() {
        // java_exception
        String java_exception = e.getClass().getName();
        // Cause
        Throwable cause = e.getCause();
        Term causeTerm = cause != null ? new Struct(cause.toString()) : new Int(0);
        // Message
        String message = e.getMessage();
        Term messageTerm = message != null ? new Struct(message) : new Int(0);
        // StackTrace
        Struct stackTraceTerm = new Struct();
        StackTraceElement[] elements = e.getStackTrace();
        for (StackTraceElement element : elements)
            stackTraceTerm.append(new Struct(element.toString()));
        // return
        return new Struct(java_exception, causeTerm, messageTerm,
                stackTraceTerm);
    }

}
