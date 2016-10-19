package nars.web;

import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;


public class Client {

    public static void main(String[] args) {

        HTMLDocument doc = HTMLDocument.current();

        HTMLElement div = doc.createElement("div");
        div.appendChild(doc.createTextNode("TeaVM generated element"));

        doc.getBody().appendChild(div);

    }

}
