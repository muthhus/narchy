package nars;

import com.google.common.util.concurrent.RateLimiter;
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
import jcog.bag.impl.PLinkArrayBag;
import jcog.data.MutableInteger;
import jcog.event.On;
import jcog.pri.PLink;
import jcog.pri.op.PriMerge;
import nars.op.Command;
import org.apache.commons.lang3.mutable.MutableInt;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.googlecode.lanterna.gui2.BorderLayout.Location.*;

/**
 * https://github.com/mabe02/lanterna/blob/master/src/test/java/com/googlecode/lanterna/terminal/TelnetTerminalTest.java
 * https://github.com/mabe02/lanterna/blob/master/src/test/java/com/googlecode/lanterna/gui2/GUIOverTelnet.java
 */
public class TelnetServer extends TelnetTerminalServer {

    final static org.slf4j.Logger logger = LoggerFactory.getLogger(TelnetServer.class);
    private final NAR nar;

    public TelnetServer(NAR n, int port) throws IOException {
        super(port, Charset.forName("utf-8"));
        this.nar = n;

        logger.info("listen port={}", port);
        while (true) {
            TelnetTerminal conn = acceptConnection();
            if (conn != null) {
                logger.info("connect from {}", conn.getRemoteSocketAddress());
                new TelnetSession(conn).start();
            }
        }
    }

    public static void main(String[] args) throws IOException {

        NAR nar = NARS
                .realtime()
                .memory("/tmp/nars")
                .then(Hear::wiki)
                .get();

        nar.startFPS(1f);

        new TelnetServer(nar,1024);


    }




    private class TelnetSession extends Thread {


        private final TelnetTerminal terminal;
        private volatile TerminalSize size;

        public TelnetSession(TelnetTerminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public void run() {

            Screen screen = null;
            try {
                screen = new TerminalScreen(terminal);
                screen.startScreen();

                final MultiWindowTextGUI textGUI = new MultiWindowTextGUI(screen);
                textGUI.setBlockingIO(false);
                textGUI.setEOFWhenNoWindows(true);


                TextColor.Indexed limegreen = TextColor.ANSI.Indexed.fromRGB(127, 255, 0);
                TextColor.Indexed orange = TextColor.ANSI.Indexed.fromRGB(255, 127, 0);
                TextColor.Indexed darkblue = TextColor.ANSI.Indexed.fromRGB(5, 5, 80);
                TextColor.Indexed darkgreen = TextColor.ANSI.Indexed.fromRGB(5, 80, 5);

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
                        orange, darkgreen,
                        white, darkblue,
                        TextColor.ANSI.BLACK
                );


                st.setWindowPostRenderer(null);

                textGUI.setTheme(st);

                final BasicWindow window = new BasicWindow();
                window.setHints(Collections.singletonList(Window.Hint.FULL_SCREEN));




                //p.setSize(new TerminalSize(screen.getTerminalSize().getColumns(), screen.getTerminalSize().getRows())); //TODO update with resize

                //final Table<String> table2 = new Table<String>("Pri", "Term", "Truth");
                //table.setCellSelection(true);

                //final TableModel<String> model = table.getTableModel();
//                model.addRow("Row1", "Row1", "Row1");
//                model.addRow("Row2", "Row2", "Row2");
//                model.addRow("Row3", "Row3", "Row3");





//                p.setLayoutManager(new LinearLayout(Direction.VERTICAL));
//                p.addComponent(new Button("Button", () -> {
//                    final BasicWindow messageBox = new BasicWindow("Response");
//                    messageBox.setComponent(Panels.vertical(
//                            new Label("Hello!"),
//                            new Button("Close", messageBox::close)));
//                    textGUI.addWindow(messageBox);
//                }).withBorder(Borders.singleLine("This is a button")));



                Panel p = new Panel(new BorderLayout());

                Component options = Panels.horizontal(
                        new CheckBox("Pause"),
                        new ComboBox<>("Log", "Concepts")
                );

                p.addComponent(Panels.vertical(
                        options
                ), TOP);

                TaskListBox table = new TaskListBox(64);
                p.addComponent(Panels.grid(2,
                    table, new EmotionDashboard()
                ), CENTER);

                final InputTextBox input = new InputTextBox();
                textGUI.getGUIThread().invokeLater(input::takeFocus);
                p.addComponent(input, BOTTOM);

                //p.addComponent(new Button("end", window::close));

                window.setComponent(p);


                textGUI.addWindowAndWait(window);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
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


        private class InputTextBox extends TextBox {
            public InputTextBox() {
                super(new TerminalSize(20, 2));
            }

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
        }


    }

    private class TaskListBox extends AbstractListBox {

        public final PLinkArrayBag<Task> tasks;


        public final AtomicBoolean paused = new AtomicBoolean(false);
        private final AtomicBoolean changed = new AtomicBoolean(false);
        public final MutableInteger visible = new MutableInteger();
        private final On onCycle, onTask;

        //final RateLimiter widgetUpdateRate = RateLimiter.create(0.25f);

        public TaskListBox(int capacity) {
            super();

            setListItemRenderer(new TaskListRenderer());

            visible.setValue(capacity);

            tasks = new PLinkArrayBag(capacity*2, PriMerge.max, new ConcurrentHashMap());

            onTask = nar.eventTaskProcess.on/*Weak*/(t -> {
                tasks.put(new PLink<>(t, t.priElseZero()));
                update();
            });
            onCycle = nar.eventCycleStart.on/*Weak*/((n)->this.update());

        }

        public void update() {

            if (changed.compareAndSet(false, true)) {
                TextGUI gui = getTextGUI();
                if (gui!=null) {
                    TextGUIThread guiThread = gui.getGUIThread();
                    if (guiThread!=null) {
                        guiThread.invokeLater(this::render);
                    }
                }
            }
        }


        protected void render() {

            changed.set(false);
            tasks.commit();
            clearItems();

            tasks.forEach(visible.intValue(), t -> addItem(t.get()));


            //                        model.addRow(
            //                                Texts.n4(t.pri()),
            //                                t.term().toString() + Character.valueOf((char) t.punc()),
            //                                t.truth()!=null ? t.truth().toString() : "");
            //setViewTopRow(Math.max(0,model.getRowCount()-getVisibleRows()));


        }

    }

    private class TaskListRenderer extends AbstractListBox.ListItemRenderer {

        @Override
        public void drawItem(TextGUIGraphics graphics, AbstractListBox table, int index, Object item, boolean selected, boolean focused) {

            Task t = (Task) item; //HACK use generic variables

            ThemeDefinition themeDefinition = table.getTheme().getDefinition(AbstractListBox.class);

            if (selected && focused) {
                graphics.applyThemeStyle(themeDefinition.getSelected());
            } else {
                graphics.applyThemeStyle(themeDefinition.getNormal());
                int c = (int) (t.pri() * 200 + 55);
                int r, g, b;
                if (t.isBelief()) {
                    r = g = b = c;
                } else if (t.isGoal()) {
                    g = c;
                    r = b = 5;
                } else if (t.isQuestOrQuestion()) {
                    b = c;
                    r = g = 5;
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
    }

    private class EmotionDashboard extends Panel {
        private final TextBox stats;
        private final On on;
        private AtomicBoolean busy = new AtomicBoolean(false);

        public EmotionDashboard() {
            super(new BorderLayout());


            stats = new TextBox(new TerminalSize(40,30),
                    TextBox.Style.MULTI_LINE);
            stats.setReadOnly(true);

            addComponent(stats, CENTER);

            on = nar.eventCycleStart.on/*Weak*/((n)->this.update());
        }

        private final StringBuilder sb = new StringBuilder(1024);

        protected void update() {
            if (busy.compareAndSet(false, true)) {
                getTextGUI().getGUIThread().invokeLater(() -> {
                    sb.setLength(0);
                    nar.stats(sb);
                    busy.set(false);
                    stats.setText(sb.toString());
                });
            }
        }


    }
}

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

//        //@Override
//        public void run1() {
//            try {
//                final String string = this.toString();
//                Random random = new Random();
//                terminal.enterPrivateMode();
//                terminal.clearScreen();
//                terminal.addResizeListener((terminal1, newSize) -> {
//                    System.err.println("Resized to " + newSize);
//                    size = newSize;
//                });
//                size = terminal.getTerminalSize();
//
//                while (true) {
//                    KeyStroke key = terminal.pollInput();
//                    if (key != null) {
//                        System.out.println(key);
//                        if (key.getKeyType() == KeyType.Escape) {
//                            terminal.exitPrivateMode();
//                            //logger.info("{} stop: Escape key", this, e.getMessage());
//                            return;
//                        }
//                    }
//
//                    TextColor.Indexed foregroundIndex = TextColor.Indexed.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));
//                    TextColor.Indexed backgroundIndex = TextColor.Indexed.fromRGB(random.nextInt(255), random.nextInt(255), random.nextInt(255));
//
//                    terminal.setForegroundColor(foregroundIndex);
//                    terminal.setBackgroundColor(backgroundIndex);
//                    terminal.setCursorPosition(random.nextInt(size.getColumns() - string.length()), random.nextInt(size.getRows()));
//                    printString(terminal, string);
//
//                    try {
//                        Thread.sleep(200);
//                    } catch (InterruptedException e) {
//                    }
//                }
//            } catch (IOException e) {
//                logger.info("{} stop: {}", this, e.getMessage());
//            } finally {
//                try {
//                    terminal.close();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }