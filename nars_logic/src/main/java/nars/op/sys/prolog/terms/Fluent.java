package nars.op.sys.prolog.terms;

import org.jetbrains.annotations.Nullable;

/**
 * A Fluent is a Prolog Object which has its own state, subject to changes over
 * time.
 * 
 */
public class Fluent extends SystemObject {

	public static final String FluentPrefix = "f";

	public Fluent(Prog p) {
		super(FluentPrefix);
		trailMe(p);
	}

	private boolean persistent;

	/**
	 * Dynamically sets the persistence status of this Fluent. A persistent
	 * Fluent will not have its stop method outomatically called upon
	 * backtracking. A typical example would be a file or socket handle saved to
	 * the database to be reused after backtracking.
	 */
	public void setPersistent(boolean persistent) {
		this.persistent = persistent;
	}

	/**
	 * returns true if this Fluent is persistent, false otherwise
	 */
	public boolean getPersistent() {
		return this.persistent;
	}

	/**
	 * Adds this Fluent to the parent Solver's trail, which will eventually call
	 * the undo method of the Fluent on backtracking.
	 */
	protected void trailMe(@Nullable Prog p) {
		if (null != p)
			p.getTrail().add(this);
	}

	public void stop() {
	}

	/**
	 * applies a non-persistent Fluent's stop() method on backtracking
	 */
	protected void undo() {
		if (!persistent)
			stop();
	}
}
