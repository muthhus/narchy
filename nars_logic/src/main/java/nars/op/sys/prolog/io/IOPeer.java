package nars.op.sys.prolog.io;
/*
 * Copyright (C) Paul Tarau 1996-1999
 */

import org.jetbrains.annotations.NotNull;

public interface IOPeer {

	void print(String s);

	void traceln(String s);

	void println(String s);

	// public abstract boolean addReader(Thread readThread);

	// public abstract String getReadString();

	@NotNull String readln();

	void halt();
}