package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.Window;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import spacegraph.SpaceGraph;

import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.List;


public class TextEdit extends DefaultVirtualTerminal implements Runnable {

    public MultiWindowTextGUI gui;

    public TextBox textBox;

    public TextEdit(int c, int r) {
        super(new TerminalSize(c, r));

//
//        //term.clearScreen();
        new Thread(this).start();
    }

    @Override
    public void close() {
        super.close();
        gui.removeWindow(gui.getActiveWindow());
    }

    public static void main(String[] args) {
        SpaceGraph.window(new ConsoleTerminal(
                new TextEdit(40, 20)
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



//            SimpleTheme st = SimpleTheme.makeTheme(
//                                /*SimpleTheme makeTheme(
//                                    boolean activeIsBold,
//                                     TextColor baseForeground,            TextColor baseBackground,
//                                            TextColor editableForeground,            TextColor editableBackground,
//                                                       TextColor selectedForeground,            TextColor selectedBackground,
//                                                              TextColor guiBackground) {*/
//                    true,
//                    TextColor.ANSI.DEFAULT, TextColor.ANSI.BLACK,
//                    TextColor.ANSI.YELLOW, TextColor.ANSI.BLACK,
//                    TextColor.ANSI.WHITE, TextColor.ANSI.BLUE,
//                    TextColor.ANSI.BLACK
//            );
//            //st.setWindowPostRenderer(null);
//            gui.setTheme(st);


            TerminalSize size = getTerminalSize();

            final BasicWindow window = new BasicWindow();
            window.setPosition(new TerminalPosition(0, 0));
            window.setSize(new TerminalSize(size.getColumns() - 2, size.getRows() - 2));

            window.setHints(List.of(Window.Hint.FULL_SCREEN));
            window.setEnableDirectionBasedMovements(true);



            //Panel panel = new Panel();
            //panel.setPreferredSize(new TerminalSize(32, 8));
            //panel.set


            textBox = new TextBox("", TextBox.Style.MULTI_LINE) {
                final Toolkit toolkit = Toolkit.getDefaultToolkit();
                final Clipboard clipboard = toolkit.getSystemClipboard();

                {
                    setBacklogSize(16);
                }

                @Override
                public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
                    if (keyStroke.getKeyType()== KeyType.Enter && keyStroke.isCtrlDown()) {
                        //ctrl-enter
                        onControlEnter();
                    }
                    if (keyStroke.getKeyType()== KeyType.Insert&& keyStroke.isShiftDown()) {
                        //shift-insert, paste
                        try {
                            String result = (String) clipboard.getData(DataFlavor.stringFlavor);


                            for (char c : result.toCharArray()) //HACK
                                addInput(KeyStroke.fromString(String.valueOf(c)));

                        } catch (UnsupportedFlavorException | IOException e) {
                            e.printStackTrace();
                        }
                    }

                    return super.handleKeyStroke(keyStroke);
                }
            };
            textBox.setPreferredSize(new TerminalSize(window.getSize().getColumns()-2, window.getSize().getRows()-2));

            textBox.takeFocus();

            window.setComponent(textBox);
//            panel.addComponent(new Button("Button", ()->{}));
//            panel.addComponent(new Button("XYZ", ()->{}));


            //window.setComponent(panel);


            gui.addWindow(window);
            gui.setActiveWindow(window);

            gui.setEOFWhenNoWindows(true);

//            flush();

            gui.waitForWindowToClose(window);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void onControlEnter() {


    }
}
