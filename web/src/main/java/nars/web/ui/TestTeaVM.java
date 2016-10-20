package nars.web.ui;

import org.teavm.jso.JSBody;
import org.teavm.jso.browser.Window;
import org.teavm.jso.core.JSDate;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;


public final class TestTeaVM {


    public static void main(String[] args) {
        final HTMLDocument doc = Window.current().getDocument();
        Socket.socket("localhost", Integer.toString(8080), "active");
        HTMLElement div = doc.createElement("button");
        div.appendChild(doc.createTextNode("_y_zz zc__" + JSDate.now() ));
        doc.getBody().appendChild(div);

    }


    @JSBody(params = {/*"host", "port", "path"*/}, script =
            //"console.log(\"ws://\"+ host + \":\" + port + \"/\" + path)")
            "console.log(2)")
    public static native void socket(/*String host, int port, String path*/);

    public static class WebSocket  {
        //final Object socket;

        public WebSocket(String host, int port, String path) {
            //socket = socket(/*host, port, path*/);
            HTMLDocument doc = HTMLDocument.current();
            doc.appendChild(doc.createTextNode("ws://" + host + ':' + port + '/' + path));
        }

    }
}


