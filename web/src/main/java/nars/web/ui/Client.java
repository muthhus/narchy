package nars.web.ui;

import org.teavm.jso.browser.Storage;
import org.teavm.jso.browser.Window;
import org.teavm.jso.dom.html.HTMLButtonElement;
import org.teavm.jso.dom.html.HTMLDocument;
import org.teavm.jso.dom.html.HTMLElement;
import org.teavm.jso.dom.html.HTMLInputElement;


public class Client {

    private static HTMLDocument document = Window.current().getDocument();


    public static void main(String[] args) {

        HTMLButtonElement saveButton = document.getElementById("save-button").cast();
        saveButton.listenClick(e -> {
            String key = document.getElementById("key").<HTMLInputElement>cast().getValue();
            String value = document.getElementById("value").<HTMLInputElement>cast().getValue();

            if (key != null && key.length() > 0 && value != null && value.length() > 0) {
                draw();
            }
        });
        HTMLButtonElement deleteButton = document.getElementById("delete-button").cast();
        deleteButton.listenClick(e -> {
            String key = document.getElementById("key").<HTMLInputElement>cast().getValue();
            if (key != null && key.length() > 0) {
                draw();
            }
        });
        HTMLButtonElement deleteAllButton = document.getElementById("delete-all-button").cast();
        deleteAllButton.listenClick(e -> {
            draw();
        });
        draw();
    }

    private static void draw() {
        HTMLElement tbody = document.getElementById("list");

        while (tbody.getFirstChild() != null) {
            tbody.removeChild(tbody.getFirstChild());
        }


    }
}
