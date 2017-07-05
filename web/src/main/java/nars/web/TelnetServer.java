package nars.web;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.ThemeDefinition;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminalServer;
import nars.NAR;
import nars.Narsese;
import nars.Task;
import nars.nar.NARBuilder;
import nars.op.Command;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
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
        NAR nar = new NARBuilder().get();
        Hear.wiki(nar);
        new TelnetSession(terminal, nar).start();
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

        public TelnetSession(TelnetTerminal terminal, NAR nar) {
            this.terminal = terminal;
            this.nar = nar;
        }

        public void run() {
            nar.startFPS(3f);

            Screen screen = null;
            try {
                screen = new TerminalScreen(terminal);
                screen.startScreen();

                final MultiWindowTextGUI textGUI = new MultiWindowTextGUI(screen);
                textGUI.setBlockingIO(false);
                textGUI.setEOFWhenNoWindows(true);


                TextColor.Indexed limegreen = TextColor.ANSI.Indexed.fromRGB(127, 255, 0);
                TextColor.Indexed orange = TextColor.ANSI.Indexed.fromRGB(255, 127, 0);

                TextColor.Indexed white = TextColor.ANSI.Indexed.fromRGB(255, 255, 255);
                SimpleTheme st = SimpleTheme.makeTheme(
                                /*SimpleTheme makeTheme(
                                    boolean activeIsBold,
                                     TextColor baseForeground,            TextColor baseBackground,
                                            TextColor editableForeground,            TextColor editableBackground,
                                                       TextColor selectedForeground,            TextColor selectedBackground,
                                                               TextColor guiBackground) {*/
                        true,
                        white, TextColor.ANSI.BLACK,
                        TextColor.ANSI.BLACK, limegreen,
                        TextColor.ANSI.BLACK, orange,
                        TextColor.ANSI.BLACK
                );


                st.setWindowPostRenderer(null);

                textGUI.setTheme(st);

                final BasicWindow window = new BasicWindow();
                window.setHints(Collections.singletonList(Window.Hint.FULL_SCREEN));


                Panel p = new Panel(new BorderLayout());

                AbstractListBox table = new AbstractListBox() {

                };
                table.setListItemRenderer(new AbstractListBox.ListItemRenderer() {
                    public void drawItem(TextGUIGraphics graphics, AbstractListBox listBox, int index, Object item, boolean selected, boolean focused) {

                        Task t = (Task) item; //HACK use generic variables

                        ThemeDefinition themeDefinition = table.getTheme().getDefinition(AbstractListBox.class);

                        if (selected && focused) {
                            graphics.applyThemeStyle(themeDefinition.getSelected());
                        } else {
                            graphics.applyThemeStyle(themeDefinition.getNormal());
                            int c = (int) (t.pri() * 200 + 55);
                            int r, g, b;
                            if (t.isBelief()) {
                                r = g = b = c / 2;
                            } else if (t.isGoal()) {
                                g = c;
                                r = b = 55;
                            } else if (t.isQuestOrQuestion()) {
                                b = c;
                                r = g = 55;
                            } else {
                                r = g = b = 255; //command
                            }

                            graphics.setForegroundColor(
                                    TextColor.Indexed.fromRGB(r, g, b)
                            );
                        }

                        String label = t.toString(nar).toString();
                        //getLabel(listBox, index, item);
                        int cols = graphics.getSize().getColumns();
                        label = TerminalTextUtils.fitString(label, cols);
                        while (TerminalTextUtils.getColumnWidth(label) < cols) {
                            label += " ";
                        }
                        graphics.putString(0, 0, label);
                    }
                });

                //p.setSize(new TerminalSize(screen.getTerminalSize().getColumns(), screen.getTerminalSize().getRows())); //TODO update with resize

                //final Table<String> table2 = new Table<String>("Pri", "Term", "Truth");
                //table.setCellSelection(true);

                //final TableModel<String> model = table.getTableModel();
//                model.addRow("Row1", "Row1", "Row1");
//                model.addRow("Row2", "Row2", "Row2");
//                model.addRow("Row3", "Row3", "Row3");


                int maxRows = 24;
                nar.onTask(t -> {
                    table.getTextGUI().getGUIThread().invokeLater(() -> {

                        if (table.getItemCount() + 1 >= maxRows) {
                            table.removeItem(0);
                        }
                        table.addItem(t);
//                        model.addRow(
//                                Texts.n4(t.pri()),
//                                t.term().toString() + Character.valueOf((char) t.punc()),
//                                t.truth()!=null ? t.truth().toString() : "");
                        //table.setViewTopRow(Math.max(0,model.getRowCount()-table.getVisibleRows()));

                    });
//                    synchronized (model) {
//                    }
                });

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
                        Result r = super.handleKeyStroke(keyStroke);
                        if (keyStroke.getKeyType() == KeyType.Enter) {
                            String t = getText().trim();
                            setText("");
                            setCaretPosition(0);


                            if (!t.isEmpty()) {
                                try {
                                    nar.input(t);
                                } catch (Narsese.NarseseException e) {
                                    Command.log(nar, e);
                                }
                            }
                        }
                        return r;
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

                Component options = Panels.horizontal(
                        new CheckBox("Pause"),
                        new ComboBox<>("Log", "Concepts")
                );


                p.addComponent(Panels.vertical(
                        table,
                        options
                ), BorderLayout.Location.TOP);

                p.addComponent(table, BorderLayout.Location.CENTER);

                p.addComponent(
                        textBox,
                        BorderLayout.Location.BOTTOM);
                //p.addComponent(new Button("end", window::close));

                window.setComponent(p);

                textGUI.getGUIThread().invokeLater(textBox::takeFocus);

                textGUI.addWindowAndWait(window);
            } catch (
                    IOException e)

            {
                e.printStackTrace();
            } finally

            {

                if (screen != null) {
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
