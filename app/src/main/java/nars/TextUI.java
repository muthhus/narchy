package nars;

import com.google.common.collect.Sets;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.SimpleTheme;
import com.googlecode.lanterna.graphics.ThemeDefinition;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.MouseCaptureMode;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.ansi.ANSITerminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminal;
import com.googlecode.lanterna.terminal.ansi.TelnetTerminalServer;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import jcog.bag.impl.PLinkArrayBag;
import jcog.data.MutableInteger;
import jcog.event.On;
import jcog.pri.PLink;
import jcog.pri.Prioritized;
import jcog.pri.op.PriMerge;
import nars.control.Activate;
import nars.nlp.Hear;
import nars.op.Command;
import nars.task.ITask;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.googlecode.lanterna.gui2.BorderLayout.Location.*;
import static com.googlecode.lanterna.gui2.Window.Hint.NO_POST_RENDERING;

/**
 * https://github.com/mabe02/lanterna/blob/master/src/test/java/com/googlecode/lanterna/terminal/TelnetTerminalTest.java
 * https://github.com/mabe02/lanterna/blob/master/src/test/java/com/googlecode/lanterna/gui2/GUIOverTelnet.java
 */
public class TextUI {

    final static org.slf4j.Logger logger = LoggerFactory.getLogger(TextUI.class);
    private final NAR nar;

    final Set<TelnetSession> sessions = Sets.newConcurrentHashSet();

    public TextUI(NAR n) {
        this.nar = n;
    }

    public DefaultVirtualTerminal session() {
        DefaultVirtualTerminal vt = new DefaultVirtualTerminal(new TerminalSize(80, 25));
        TelnetSession session = new TelnetSession(vt);
        sessions.add(session);
        session.start();
        return vt;
    }

    public TextUI(NAR n, int port) throws IOException {
        this(n);
        TelnetTerminalServer server = new TelnetTerminalServer(port, Charset.forName("utf-8"));
        logger.info("listen port={}", port);
        while (true) {
            TelnetTerminal conn = server.acceptConnection();
            if (conn != null) {
                logger.info("connect from {}", conn.getRemoteSocketAddress());
                TelnetSession session = new TelnetSession(conn);
                session.setUncaughtExceptionHandler((t, e) -> session.end());
                sessions.add(session);
                session.start();
            }
        }
    }

    public static void main(String[] args) throws IOException {

        NAR nar = NARS
                .realtime()
                .memory("/tmp/nars")
                .then(Hear::wiki)
                .get();

        nar.startFPS(2f);

        try {
            InterNAR i = new InterNAR(nar);
            i.recv.amplitude(0.1f);
            i.runFPS(2);
        } catch (Exception e) {
            e.printStackTrace();
        }

        new TextUI(nar, 1024);


    }


    private class TelnetSession extends Thread {


        private static final long GUI_UPDATE_MS = 250;

        private final Terminal terminal;
        private TerminalScreen screen;


        public TelnetSession(Terminal terminal) {
            this.terminal = terminal;
        }

        @Override
        public void run() {


            try {
                if (terminal instanceof ANSITerminal)
                    ((ANSITerminal)terminal).setMouseCaptureMode(MouseCaptureMode.CLICK);

                screen = new TerminalScreen(terminal);
                screen.startScreen();


                final MultiWindowTextGUI textGUI = new MultiWindowTextGUI(screen);
                textGUI.setBlockingIO(false);
                //textGUI.setEOFWhenNoWindows(true);


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
                window.setHints(List.of(Window.Hint.FULL_SCREEN, NO_POST_RENDERING));
                window.setEnableDirectionBasedMovements(true);


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


                ActionListBox menu = new ActionListBox();
                Runnable defaultMenu;
                menu.addItem("Tasks", defaultMenu = () -> {
                    p.addComponent(new TaskListBox(64), CENTER);
                });

                menu.addItem("Concepts", () -> {
                    p.addComponent(new BagListBox<Activate>(64) {
                        @Override
                        public void update() {
                            nar.forEachConceptActive(this::add);
                            super.update();
                        }
                    }, CENTER);
                });
                menu.addItem("Activity", () -> {
                    p.addComponent(new BagListBox<ITask>(64) {
                        @Override
                        public void update() {
                            nar.forEachProtoTask(this::add);
                            super.update();
                        }
                    }, CENTER);
                });

                menu.addItem("Stats", () -> {
                    p.addComponent(new EmotionDashboard(), CENTER);
                });


                final InputTextBox input = new InputTextBox();
                p.addComponent(menu, LEFT);
                p.addComponent(input, BOTTOM);
                window.setComponent(p);

                textGUI.getGUIThread().invokeLater(defaultMenu);
                textGUI.getGUIThread().invokeLater(input::takeFocus);

                textGUI.addWindowAndWait(window);

            } catch (IOException e) {
                e.printStackTrace();
            } finally {

                end();
            }
        }

        final void end() {

            if (screen != null) {
                try {
                    screen.stopScreen();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            if (terminal != null) {
                try {
                    terminal.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            sessions.remove(this);
        }


        private class InputTextBox extends TextBox {
            public InputTextBox() {
                super(new TerminalSize(20, 1));
                setVerticalFocusSwitching(true);
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


        On newGUIUpdate(/* TODO AtomicBoolean busy, */Runnable r) {
            return nar.eventCycle.on(GUI_UPDATE_MS, (n) -> r.run());
        }

        private class TaskListRenderer extends AbstractListBox.ListItemRenderer {

            final ThemeDefinition themeDefinition;

            private TaskListRenderer(AbstractListBox table) {
                this.themeDefinition = table.getTheme().getDefinition(AbstractListBox.class);
            }

            @Override
            public void drawItem(TextGUIGraphics graphics, AbstractListBox table, int index, Object item, boolean selected, boolean focused) {

                Task t = (Task) item; //HACK use generic variables


                if (selected && focused) {
                    graphics.applyThemeStyle(themeDefinition.getSelected());
                } else {
                    //graphics.applyThemeStyle(themeDefinition.getNormal());
                    int c = (int) (t.priElseZero() * 200 + 55);
                    int r, g, b;
                    if (t.isBelief()) {
                        r = g = b = c / 2;
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
                int cols = graphics.getSize().getColumns() - 1;
                label = TerminalTextUtils.fitString(label, cols);
                int w = TerminalTextUtils.getColumnWidth(label);
                while (w < cols) {
                    label += " ";
                    w++;
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


                stats = new TextBox(" \n \n");
                stats.setReadOnly(true);
                stats.setHorizontalFocusSwitching(true);
                stats.setVerticalFocusSwitching(true);


                addComponent(stats, CENTER);

                on = newGUIUpdate(this::update);
            }

            @Override
            public synchronized void onRemoved(Container container) {
                on.off();
            }

            private final StringBuilder sb = new StringBuilder(1024);

            protected void update() {
                if (busy.compareAndSet(false, true)) {
                    nar.stats(sb);

                    String s = sb.toString();
                    sb.setLength(0);
                    s.replace('\t', ' ');

                    getTextGUI().getGUIThread().invokeLater(() -> {
                        stats.setText(s);
                        busy.set(false);
                    });
                }
            }


        }

        class BagListBox<X extends Prioritized> extends AbstractListBox {
            protected final PLinkArrayBag<X> bag;


            protected final AtomicBoolean paused = new AtomicBoolean(false);
            protected final AtomicBoolean changed = new AtomicBoolean(false);
            protected final MutableInteger visible = new MutableInteger();

            protected final On onCycle;
            float priInfluenceRate = 1f;
            private boolean autoupdate = true;

            public BagListBox(int capacity) {
                this(capacity, true);
            }

            public BagListBox(int capacity, boolean autoupdate) {

                this.autoupdate = autoupdate;
                visible.setValue(capacity);

                bag = new PLinkArrayBag(capacity * 2, PriMerge.replace, new ConcurrentHashMap());
                onCycle = newGUIUpdate(this::update);

            }

            public void add(X x) {
                add(new PLink<>(x, priInfluenceRate * x.priElseZero()));
            }
            public void add(PLink<X> p) {
                if (bag.put(p)!=null) {

                }

                changed.set(true); //update anyway since merges and forgetting may have shifted ordering
            }

            public void update() {

                if (autoupdate || changed.compareAndSet(true, false)) {
                    TextGUI gui = getTextGUI();
                    if (gui != null) {
                        TextGUIThread guiThread = gui.getGUIThread();
                        if (guiThread != null) {

                            next.clear();
                            bag.commit();
                            bag.forEach(visible.intValue(), t -> next.add(t.get()) );

                            guiThread.invokeLater(this::render);
                        }
                    }
                }
            }

            @Override
            public synchronized void onRemoved(Container container) {
                super.onRemoved(container);
                onCycle.off();
            }

            final List<X> next = $.newArrayList();

            protected void render() {

                clearItems();
                next.forEach(this::addItem);



                //                        model.addRow(
                //                                Texts.n4(t.pri()),
                //                                t.term().toString() + Character.valueOf((char) t.punc()),
                //                                t.truth()!=null ? t.truth().toString() : "");
                //setViewTopRow(Math.max(0,model.getRowCount()-getVisibleRows()));


            }

        }

        class TaskListBox extends BagListBox<Task> {

            private final On onTask;


            @Override
            public synchronized void onRemoved(Container container) {
                super.onRemoved(container);
                onTask.off();
            }

            public TaskListBox(int capacity) {
                super(capacity, false);

                setListItemRenderer(new TaskListRenderer(this));

                onTask = nar.eventTask.on(t -> {
                    add(t);
                    changed.set(true);
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