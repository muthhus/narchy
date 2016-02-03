package nars.op.software.prolog.io;
/*
 * Copyright (C) Paul Tarau 1996-1999
 */

public interface IOPeer {

	void print(String s);

	void traceln(String s);

	void println(String s);

	// public abstract boolean addReader(Thread readThread);

	// public abstract String getReadString();

	String readln();

	void halt();
}