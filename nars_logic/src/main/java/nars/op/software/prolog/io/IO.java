package nars.op.software.prolog.io;

/*
 * Copyright (C) Paul Tarau 1996-1999
 */

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;

public class IO {

	public static IOPeer peer = null;

	public static final boolean showOutput = true;

	public static final boolean showErrors = true;

	public final static int showTrace = 0;

	public static long maxAnswers = 0; // 0 means all, >0 means ask

	public static final Reader input = toReader(System.in);

	public static final Writer output = toWriter(System.out);

	static Reader toReader(InputStream f) {
		return new BufferedReader(new InputStreamReader(f));
	}

	public static Reader toFileReader(String fname) {
		return url_or_file(fname);
	}

	static Writer toWriter(OutputStream f) {
		return new BufferedWriter(new OutputStreamWriter(f));
	}

	static Writer toFileWriter(String s) {
		Writer f = null;
		// mes("HERE"+s);
		try {
			f = toWriter(new FileOutputStream(s));
		} catch (IOException e) {
			error("write error, to: " + s);
		}
		return f;
	}

	public static Reader getStdInput() {
		return input;
	}

	public static Writer getStdOutput() {
		return output;
	}

	// synchronized
	static public final void print(Writer f, String s) {
		if (!showOutput)
			return;
		if (peer == null) {
			try {
				f.write(s);
				f.flush();
			} catch (IOException e) {
				System.err.println("*** error in printing: " + e);
			}
		} else
			peer.print(s);
		return;
	}

	public static final void println(Writer o, String s) {
		print(o, s + '\n');
	}

	public static final void print(String s) {
		print(output, s);
	}

	public static final void println(String s) {
		println(output, s);
	}

	// for now just stubs: usable if IO comes from elswere i.e. sockets
	static final String read_from(Reader f) {
		return readln(f);
	}

	// for now just stubs: usable if IO comes from elswere i.e. sockets
	static final void write_to(Writer f, String s) {
		println(f, s);
	}

	static final int MAXBUF = 1 << 30;

	static String readLine(Reader f) throws IOException {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < MAXBUF; i++) {
			int c = f.read();
			if (c == '\0' || c == '\n' || c == -1
					|| (c == '\r' && '\n' == f.read())) {
				if (i == 0 && c == -1)
					return null;
				break;
			}
			s.append(c);
		}
		return s.toString();
	}

	static final String readln(Reader f) {
		trace(2, "READLN TRACE: entering");
		String s = null;
		try {
			if (f instanceof BufferedReader) {
				s = ((BufferedReader) f).readLine();
			} else
				s = readLine(f);
		} catch (IOException e) {
			error("error in readln: e.toString()");
		}
		trace(2, "READLN TRACE:" + '<' + s + '>');
		return s;
	}

	public static final String readln() {
		String s;
		if (peer == null) {
			s = readln(input);
		} else {
			s = peer.readln();
		}
		return s;
	}

	public static final String promptln(String prompt) {
		print(prompt);
		return readln();
	}

	public static final void mes(String s) {
		println(output, s);
	}

	public static final void trace(int level, String s) {
		if (!showOutput || showTrace < level)
			return;
		if (peer == null) {
			println(output, s);
		} else
			peer.traceln(s);
	}

	public static final boolean trace() {
		return showTrace >= 1;
	}
	public static final void trace(String s) {
		if (showTrace >= 1) {
			println(output, s);
		}
	}

	public static void printStackTrace(Throwable e) {
		if (showErrors) {
			// ByteArrayOutputStream b=new ByteArrayOutputStream();
			// PrintWriter fb=new PrintWriter(b);
			CharArrayWriter b = new CharArrayWriter();
			PrintWriter fb = new PrintWriter(b);
			e.printStackTrace(fb);
			IO.error(b.toString());
			fb.close();
		}
	}

	public static final void error(String s) {
		if (showErrors) {
			println(output, s);
		}
	}

	// synchronized
	public static final void error(String s, Throwable e) {
		error(s);
		printStackTrace(e);
	}

	public static final void assertion(String Mes) {
		IO.error("assertion failed", (new Exception(Mes)));
	}

	public static final int system(String cmd) {
		// IO.mes("executing: <"+cmd+">");
		try {
			Runtime.getRuntime().exec(cmd);
		} catch (IOException e) {
			IO.error("error in system cmd: " + cmd, e);
			return 0;
		}
		return 1;
	}

	public static final Reader url2stream(String f) {
		return url2stream(f, false);
	}

	public static final Reader url2stream(String f, boolean quiet) {
		Reader stream = null;
		try {
			URL url = new URL(f);
			stream = toReader(url.openStream());
		} catch (MalformedURLException e) {
			if (quiet)
				return null;
			IO.error("bad URL: " + f, e);
		} catch (IOException e) {
			if (quiet)
				return null;
			IO.error("unable to read URL: " + f, e);
		}

		return stream;
	}

	public static String getBaseDir() {

		return "";

	}

	public static final Reader url_or_file(String s) {
		Reader stream = null;
		try {

			if (null == stream)
				stream = url2stream(s, true);

			if (null == stream)
				stream = toReader(new FileInputStream(s));
		} catch (IOException e) {
		}
		return stream;
	}

	public static final Reader string_to_stream(String s) {
		StringReader stream = new StringReader(s);
		return stream;
	}

	public static final URL find_url(String s) {
		String valid = null;
		Reader stream;

		String baseDir = getBaseDir();
		valid = baseDir + s;
		stream = url2stream(valid, true);

		if (null == stream) {
			valid = s;
			stream = url2stream(valid, true);
		}
		try {
			stream.close();
		} catch (IOException e) {
			valid = null;
		}

		URL url = null;

		try {
			url = new URL(valid);
		} catch (MalformedURLException e) {
		}

		return url;
	}
}
