package nars.op.in;

import nars.NAR;
import nars.task.flow.Input;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TextInput subclass that only inputs when the next input value changes from
 * previous
 */
public class ChangedTextInput {

	private final NAR nar;
	@Nullable
	private Input last = null;
	private boolean allowRepeats = false;

	public ChangedTextInput(NAR n) {
		nar = n;
	}

	public boolean set(@NotNull String s) {
		if (!enable())
			return false;
		return set(new TextInput(nar, s));
	}

	public boolean set(@NotNull Input s) {
		if (!enable())
			return false;
		if (allowRepeats() || (last == null) || (!last.equals(s))) {
			nar.input(s);
			last = s;
			return true;
		}
		// TODO option to, when else, add with lower budget ?
		return false;
	}

	public boolean allowRepeats() {
		return allowRepeats;
	}
	public boolean enable() {
		return true;
	}

	public void setAllowRepeatInputs(boolean b) {
		allowRepeats = b;
	}
}
