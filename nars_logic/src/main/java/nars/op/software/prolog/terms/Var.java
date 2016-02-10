package nars.op.software.prolog.terms;

import nars.op.software.prolog.io.Base64Codec;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Part of the Term hierarchy implmenting logical variables. They are subject to
 * reset by application of and undo action keep on the trail stack.
 */
public final class Var extends Term {
	protected Term val;

	final static AtomicInteger varSerial = new AtomicInteger(1);
	static String varName(int i) {

		char[] chars = new char[7];
		chars[0] = '_';
		Base64Codec.encode2chars((byte)((i & 0xff000000) >> 24),
								 (byte)((i & 0x00ff0000) >> 16),
								 chars, 1);
		Base64Codec.encode2chars((byte)((i & 0x0000ff00) >> 8),
								 (byte)(i & 0x000000ff),
								 chars, 4);

		return new String(chars);

		//return "_" + Integer.toString(i,36); //TODO base64+  TODO thread ID/UUID-like prefixed?
	}

	public Var() {
		super(varName(varSerial.incrementAndGet()));
		val = this;
	}

	public int arity() {
		return Term.VAR;
	}

	public final Term ref() {
		Term v = this.val;
		return (v == this) ? this : v.ref();
	}

	public boolean bind_to(Term x, Trail trail) {
		val = x;
		trail.add(this);
		return true;
	}

	protected void undo() {
		val = this;
	}

	boolean unify_to(Term that, Trail trail) {
		// expects: this, that are dereferenced
		// return (this==that)?true:val.bind_to(that,trail);
		return val.bind_to(that, trail);
	}

	public boolean eq(Term x) { // not a term compare!
		return ref() == x.ref();
	}

	public String getKey() {
		Term t = ref();
		if (t instanceof Var)
			return null;
		else
			return t.getKey();
	}

	Term reaction(Term agent) {

		Term R = agent.action(ref());

		if (!(R instanceof Var)) {
			R = R.reaction(agent);
		}

		return R;
	}


	public String toString() {
		Term t = ref();
		return t == this ? name : t.toString();
	}
}