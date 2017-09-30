package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import spacegraph.SpaceGraph;

import java.io.IOException;


public class TerminalUI extends DefaultVirtualTerminal implements Runnable {

    public MultiWindowTextGUI gui;

    public TextBox textBox;

    public TerminalUI(int c, int r) {
        super(new TerminalSize(c, r));


        //term.clearScreen();
        new Thread(this).start();
    }

    public static void main(String[] args) {
        SpaceGraph.window(new ConsoleTerminal(
                new TerminalUI(40, 20)
        ), 1000, 600);
    }

    public void commit() {
        try {
            gui.updateScreen();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {

        try {
            TerminalScreen screen = new TerminalScreen(this);
            screen.startScreen();

            gui = new MultiWindowTextGUI(
                    new SameTextGUIThread.Factory(),
                    screen);


            setCursorVisible(true);

            gui.setBlockingIO(false);
            gui.setEOFWhenNoWindows(false);

            TerminalSize size = getTerminalSize();

            final BasicWindow window = new BasicWindow();
            window.setPosition(new TerminalPosition(0, 0));
            window.setSize(new TerminalSize(size.getColumns() - 2, size.getRows() - 2));


            Panel panel = new Panel();
            panel.setPreferredSize(new TerminalSize(32, 8));


            textBox = new TextBox("", TextBox.Style.MULTI_LINE);
            textBox.takeFocus();

            panel.addComponent(textBox);
            panel.addComponent(new Button("Button", ()->{}));
            panel.addComponent(new Button("XYZ", ()->{}));


            window.setComponent(panel);


            gui.addWindow(window);
            gui.setActiveWindow(window);

            commit();

            gui.waitForWindowToClose(window);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
