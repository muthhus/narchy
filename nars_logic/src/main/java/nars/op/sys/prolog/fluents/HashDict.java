package nars.op.sys.prolog.fluents;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;

/**
 * General purpose dictionary
 */
public class HashDict extends HashMap {

	@NotNull
	public String name() {
		return getClass().getName() + hashCode();
	}

	// public String stat() {
	// return "BlackBoard: "+size();
	// }
}
