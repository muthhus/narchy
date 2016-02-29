package nars.op.sys.prolog.terms;

import nars.Op;
import nars.term.Compound;
import nars.term.SubtermVisitor;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

/**
 * Part of the Term hierarchy implmenting logical variables. They are subject to
 * reset by application of and undo action keep on the trail stack.
 */
public final class Var extends PTerm {
	protected PTerm val;

	final static AtomicInteger varSerial = new AtomicInteger(1);
	@NotNull
	static String varName(int i) {
//
//		char[] chars = new char[7];
//		chars[0] = '_';
//		Base64Codec.encode2chars((byte)((i & 0xff000000) >> 24),
//								 (byte)((i & 0x00ff0000) >> 16),
//								 chars, 1);
//		Base64Codec.encode2chars((byte)((i & 0x0000ff00) >> 8),
//								 (byte)(i & 0x000000ff),
//								 chars, 4);
//
//		return new String(chars);

		//return "_" + Integer.toString(i,36); //TODO base64+  TODO thread ID/UUID-like prefixed?


		return "_" + i; //TODO base64+  TODO thread ID/UUID-like prefixed?
	}

	public Var() {
		super(varName(varSerial.incrementAndGet()));
		val = this;
	}

	public int arity() {
		return PTerm.VAR;
	}

	@NotNull
	public final PTerm ref() {
		PTerm v = this.val;
		return (v == this) ? this : v.ref();
	}

	public boolean bind_to(PTerm x, @NotNull Trail trail) {
		val = x;
		trail.add(this);
		return true;
	}

	protected void undo() {
		val = this;
	}

	boolean unify_to(PTerm that, Trail trail) {
		// expects: this, that are dereferenced
		// return (this==that)?true:val.bind_to(that,trail);
		return val.bind_to(that, trail);
	}

	public boolean eq(@NotNull PTerm x) { // not a term compare!
		return ref() == x.ref();
	}

	public String getKey() {
		PTerm t = ref();
		return t instanceof Var ? null : t.getKey();
	}

	PTerm reaction(@NotNull PTerm agent) {

		PTerm R = agent.action(ref());

		if (!(R instanceof Var)) {
			R = R.reaction(agent);
		}

		return R;
	}

	@Override
	public final String toString() {
		PTerm t = ref();
		return t == this ? name : t.toString();
	}

	@Override
	public Op op() {
		return null;
	}

	@Override
	public int volume() {
		return 0;
	}

	@Override
	public int complexity() {
		return 0;
	}

	@Override
	public int structure() {
		return 0;
	}

	@Override
	public int size() {
		return 0;
	}

	@Override
	public boolean containsTerm(Term t) {
		return false;
	}

	@Override
	public boolean and(Predicate<? super Term> v) {
		return false;
	}

	@Override
	public boolean or(Predicate<? super Term> v) {
		return false;
	}

	@Override
	public void recurseTerms(SubtermVisitor v, Compound parent) {

	}

	@Override
	public boolean isCommutative() {
		return false;
	}

	@Override
	public int varIndep() {
		return 0;
	}

	@Override
	public int varDep() {
		return 0;
	}

	@Override
	public int varQuery() {
		return 0;
	}

	@Override
	public int varPattern() {
		return 0;
	}

	@Override
	public int vars() {
		return 0;
	}

	@Override
	public void append(Appendable w) throws IOException {

	}

	//	@Override
//	public String toString() {
//		return null;
//	}

	@Override
	public boolean isCompound() {
		return false;
	}

	@Override
	public int compareTo(Object o) {
		return 0;
	}
}