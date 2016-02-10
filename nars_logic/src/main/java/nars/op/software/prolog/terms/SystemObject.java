package nars.op.software.prolog.terms;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * A SystemObject is a Nonvar with system assigned name
 * 
 */
public class SystemObject extends Nonvar {

	final static AtomicInteger soSerial = new AtomicInteger(1);
	@Deprecated /* this is slow as fuck */ static String soName() {

		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		StackTraceElement constructorInvocation = st[3];
		String c = constructorInvocation.getClassName();

		return soName(c);
	}

	@NotNull
	static String soName(String prefix) {
		int i = soSerial.incrementAndGet();
		//TODO base64+  TODO thread ID/UUID-like prefixed?
		return '{' + prefix + '.' + Integer.toString(i,36) + '}';
	}

	public SystemObject() {
		super(soName());
	}
	public SystemObject(String prefix) {
		super(soName(prefix));
	}

	boolean bind_to(PTerm that, Trail trail) {
		return super.bind_to(that, trail)
				&& name.equals(((SystemObject) that).name);
	}

	public String toString() {
		return name;
	}

	public final int arity() {
		return PTerm.JAVA;
	}
}
