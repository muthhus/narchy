package nars.web;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.gui2.table.Table;
import com.googlecode.lanterna.gui2.table.TableModel;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminalServer;
import jcog.Texts;
import nars.NAR;
import nars.Narsese;
import nars.nar.NARBuilder;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * https://github.com/mabe02/lanterna/blob/master/src/test/java/com/googlecode/lanterna/terminal/TelnetTerminalTest.java
 * https://github.com/mabe02/lanterna/blob/master/src/test/java/com/googlecode/lanterna/gui2/GUIOverTelnet.java
 */
public class TelnetServer {

    final static org.slf4j.Logger logger = LoggerFactory.getLogger(TelnetServer.class);

    public static void main(String[] args) throws IOException {
        TelnetTerminalServer server = new TelnetTerminalServer(1024,
                Charset.forName("utf-8"));

        while (true) {
            TelnetTerminal telnetTerminal = server.acceptConnection();
            if (telnetTerminal != null) {
                onConnect(telnetTerminal);
            }
        }
    }


    static void onConnect(final TelnetTerminal terminal) {
        new TelnetSession(terminal).start();
    }

    static void printString(Terminal terminal, String string) throws IOException {
        for (int i = 0; i < string.length(); i++)
            terminal.putCharacter(string.charAt(i));
        terminal.flush();
    }

    private static class TelnetSession extends Thread {


        private final TelnetTerminal terminal;
        private volatile TerminalSize size;
        private NAR nar;

        public TelnetSession(TelnetTerminal terminal) {
            this.terminal = terminal;
        }

        public void run() {
            this.nar = new NARBuilder().get();
            nar.startFPS(1f);

            Screen screen = null;
            try {
                screen = new TerminalScreen(terminal);
                screen.startScreen();

                final MultiWindowTextGUI textGUI = new MultiWindowTextGUI(screen);
                textGUI.setBlockingIO(false);
                textGUI.setEOFWhenNoWindows(true);


                TextColor.Indexed limegreen = TextColor.ANSI.Indexed.fromRGB(127, 255, 0);
                TextColor.Indexed orange = TextColor.ANSI.Indexed.fromRGB(255, 127, 0);

                textGUI.setTheme(
                        SimpleTheme.makeTheme(
                                /*SimpleTheme makeTheme(
                                    boolean activeIsBold,
                                     TextColor baseForeground,            TextColor baseBackground,
                                            TextColor editableForeground,            TextColor editableBackground,
                                                       TextColor selectedForeground,            TextColor selectedBackground,
                                                               TextColor guiBackground) {*/
                                true,
                                TextColor.ANSI.Indexed.fromRGB(255, 255, 255), TextColor.ANSI.BLACK,
                                TextColor.ANSI.BLACK, limegreen,
                                TextColor.ANSI.BLACK,
                                    //TextColor.ANSI.Indexed.fromRGB(255, 127, 0),
                                    orange,
                                TextColor.ANSI.BLACK
                        )
                );


                final BasicWindow window = new BasicWindow("Text GUI over Telnet");

                Panel p = new Panel();


                final Table<String> table = new Table<String>("Column 1", "Column 2", "Column 3");
                final TableModel<String> model = table.getTableModel();
                model.addRow("Row1", "Row1", "Row1");
                model.addRow("Row2", "Row2", "Row2");
                model.addRow("Row3", "Row3", "Row3");
                int maxRows = 128;
                nar.onTask(t -> {
                    if (model.getRowCount() > maxRows) {
                        model.removeRow(0);
                    }
                    model.addRow(
                            Texts.n4(t.pri()),
                            t.term().toString(),
                            t.punc() + " " + t.truth());
                });
                p.addComponent(table);

//                p.setLayoutManager(new LinearLayout(Direction.VERTICAL));
//                p.addComponent(new Button("Button", () -> {
//                    final BasicWindow messageBox = new BasicWindow("Response");
//                    messageBox.setComponent(Panels.vertical(
//                            new Label("Hello!"),
//                            new Button("Close", messageBox::close)));
//                    textGUI.addWindow(messageBox);
//                }).withBorder(Borders.singleLine("This is a button")));

                final TextBox textBox = new TextBox(new TerminalSize(20, 2)) {
                    @Override
                    public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
                        if (keyStroke.getKeyType() == KeyType.Enter) {
                            String t = getText();
                            setText("");
                            try {
                                nar.input(t);
                            } catch (Narsese.NarseseException e) {
                                e.printStackTrace();
                            }
                        }
                        return super.handleKeyStroke(keyStroke);
                    }
                    //                    @Override
//                    public Result handleKeyStroke(KeyStroke keyStroke) {
//                        try {
//                            return super.handleKeyStroke(keyStroke);
//                        } finally {
//                            for (TextBox box : ALL_TEXTBOXES) {
//                                if (this != box) {
//                                    box.setText(getText());
//                                }
//                            }
//                        }
//                    }
                };



                p.addComponent(textBox.withBorder(Borders.singleLine("Text editor")));

//                p.addComponent(new AbstractInteractableComponent() {
//                    String text = "Press any key";
//
//                    @Override
//                    protected InteractableRenderer createDefaultRenderer() {
//                        return new InteractableRenderer() {
//                            @Override
//                            public TerminalSize getPreferredSize(Component component) {
//                                return new TerminalSize(30, 1);
//                            }
//
//                            @Override
//                            public void drawComponent(TextGUIGraphics graphics, Component component) {
//                                graphics.putString(0, 0, text);
//                            }
//
//                            @Override
//                            public TerminalPosition getCursorLocation(Component component) {
//                                return TerminalPosition.TOP_LEFT_CORNER;
//                            }
//                        };
//                    }
//
//                    @Override
//                    public Result handleKeyStroke(KeyStroke keyStroke) {
//                        if (keyStroke.getKeyType() == KeyType.Tab ||
//                                keyStroke.getKeyType() == KeyType.ReverseTab) {
//                            return super.handleKeyStroke(keyStroke);
//                        }
//                        if (keyStroke.getKeyType() == KeyType.Character) {
//                            text = "Character: " + keyStroke.getCharacter() + (keyStroke.isCtrlDown() ? " (ctrl)" : "") +
//                                    (keyStroke.isAltDown() ? " (alt)" : "");
//                        } else {
//                            text = "Key: " + keyStroke.getKeyType() + (keyStroke.isCtrlDown() ? " (ctrl)" : "") +
//                                    (keyStroke.isAltDown() ? " (alt)" : "");
//                        }
//                        return Result.HANDLED;
//                    }
//                }.withBorder(Borders.singleLine("Custom component")));

                p.addComponent(new Button("Close", window::close));
                window.setComponent(p);

                textGUI.addWindowAndWait(window);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                if (screen!=null) {
                    try {
                        screen.stopScreen();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                try {
                    terminal.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        //@Override
        public void run1() {
            try {
                final String string = this.toString();
                Random random = new Random();
                terminal.enterPrivateMode();
                terminal.clearScreen();
                terminal.addResizeListener((terminal1, newSize) -> {
                    System.err.println("Resized to " + newSize);
                    size = newSize;
                });
                size = terminal.getTerminalSize();

                while (true) {
                    KeyStroke key = terminal.pollInput();
                    if (key != null) {
                        System.out.println(key);
                        if (key.getKeyType() == KeyType.Escape) {
                            terminal.exitPrivateMode();
                            //logger.info("{} stop: Escape key", this, e.getMessage());
                            return;
                        }
                    }

                    TextColor.Indexed foregroundIndex = TextColor.Indexed.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));
                    TextColor.Indexed backgroundIndex = TextColor.Indexed.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));

                    terminal.setForegroundColor(foregroundIndex);
                    terminal.setBackgroundColor(backgroundIndex);
                    terminal.setCursorPosition(random.nextInt(size.getColumns() - string.length()), random.nextInt(size.getRows()));
                    printString(terminal, string);

                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                    }
                }
            } catch (IOException e) {
                logger.info("{} stop: {}", this, e.getMessage());
            } finally {
                try {
                    terminal.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
