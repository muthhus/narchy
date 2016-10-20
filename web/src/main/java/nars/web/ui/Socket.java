package nars.web.ui;

import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;

/*
 * The console object provides access to the browser's debugging console. The
 * specifics of how it works vary from browser to browser, but there is a
 * factual set of features that are typically provided.
 */
public final class Socket implements JSObject {

	private Socket() {
    }

	@JSBody(params = {"host", "port","path"}, script = "return new WebSocket('ws://' + host + ':' + port + '/' + path);")
	public static native String socket(String host, String port, String path);


}