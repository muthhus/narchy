package spacegraph.obj.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.jcraft.jsch.JSchException;
import com.jogamp.common.nio.ByteBufferInputStream;
import com.jogamp.common.util.Bitstream;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import nars.util.data.SimpleIntDeque;
import org.fusesource.jansi.AnsiOutputStream;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.render.Draw;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Created by me on 4/1/16.
 */
public abstract class ConsoleSurface extends Surface implements Appendable {

    public static void main(String[] args) throws InterruptedException, IOException, JSchException {


        //new DefaultVirtualTerminal();

        //new UnixTerminal();
        //= new DefaultVirtualTerminal();
                /*new ANSITerminal(new NullInputStream(1), new NullOutputStream(), Charset.defaultCharset()) {

                };*/

//        Screen s = new TerminalScreen(term);
//        s.setCursorPosition(new TerminalPosition(0,0));

        DefaultVirtualTerminal term = /*new ANSITerminal(System.in, System.out, Charset.defaultCharset()) {

        };*/ new DefaultVirtualTerminal() {

        };

        //term.enterPrivateMode();
        //term.setCursorPosition(0,0);
//        TerminalScreen s =
//                //new TerminalScreen(term);
//                new TerminalScreen(term);
//        s.startScreen();

        //final Terminal rawTerminal = new DefaultTerminalFactory().createSwingTerminal();
        //final Terminal rawTerminal = new UnixTerminal();

        Deque<Byte> outgoing = new ArrayDeque(16);

        SSH ssh = new SSH("gest", "localhost", "tseg", new InputStream() {

            @Override
            public int available() throws IOException {
                return outgoing.size();
            }

            @Override
            public int read() throws IOException {
                return outgoing.pop();
            }
        },
                new AnsiOutputStream(new OutputStream() {

                    //normal characters output
                    @Override
                    public void write(int i) throws IOException {
                        term.putCharacter((char) i);
                    }

                    @Override
                    public void flush() throws IOException {
                        term.flush();
                    }
                }) {
                    //ANSI filters
                    @Override
                    protected void processCursorTo(int row, int col) throws IOException {
                        term.setCursorPosition(col, row);
                    }


                    @Override
                    protected void processCursorDown(int count) throws IOException {
                        System.out.println("down");
                    }

                    @Override
                    protected void processCursorRight(int count) throws IOException {
                        System.out.println("right");
                    }

                    @Override
                    protected void processCursorUp(int count) throws IOException {
                        System.out.println("up");
                    }


                    @Override
                    protected void processCursorToColumn(int c) throws IOException {
                        term.setCursorPosition(c, term.getCursorPosition().getRow());
                    }

                    @Override
                    protected void processEraseLine(int eraseOption) throws IOException {

//                        protected static final int ERASE_LINE_TO_END = 0;
//                        protected static final int ERASE_LINE_TO_BEGINING = 1;
//                        protected static final int ERASE_LINE = 2;
                        //System.out.println("earse line: " + eraseOption);
                        switch (eraseOption) {
                            case 0:
                                TerminalPosition p = term.getCursorPosition();

                                //WTF lanterna why cant i access the buffer directly its private
                                int start = p.getColumn();
                                for (int i = start; i < term.getTerminalSize().getColumns(); i++)
                                    term.putCharacter(' ');

                                //return
                                term.setCursorPosition(start, p.getRow());

                                break;
                        }
                    }

                    @Override
                    protected void processEraseScreen(int eraseOption) throws IOException {
//                        protected static final int ERASE_SCREEN_TO_END = 0;
//                        protected static final int ERASE_SCREEN_TO_BEGINING = 1;
//                        protected static final int ERASE_SCREEN = 2;

                        switch (eraseOption) {
                            case ERASE_SCREEN:
                                term.clearScreen();
                                break;
                            case ERASE_SCREEN_TO_BEGINING:
                                break;
                            case ERASE_SCREEN_TO_END:
                                //TODO make sure this is correct
                                term.clearScreen();
                                break;
                        }
                    }
                }
        );


        SpaceGraph.window(new ConsoleSurface(80, 24) {
            @Override
            public TextCharacter charAt(int col, int row) {
                return term.getCharacter(col, row);
            }

            @Override
            public boolean onKey(KeyEvent e, boolean pressed) {

                //only interested on release
                if (pressed)
                    return false;

//                KeyStroke k;
//                if (e.isActionKey()) {
//                    KeyType type = null;
//                    switch (e.getKeyCode()) {
//                        case KeyEvent.VK_LEFT: type = KeyType.ArrowLeft; break;
//                        case KeyEvent.VK_RIGHT: type = KeyType.ArrowRight; break;
//                        case KeyEvent.VK_UP: type = KeyType.ArrowUp; break;
//                        case KeyEvent.VK_DOWN: type = KeyType.ArrowDown; break;
//                        case KeyEvent.VK_ENTER: type = KeyType.Enter; break;
//                        case KeyEvent.VK_ESCAPE: type = KeyType.Escape; break;
//                        case KeyEvent.VK_BACK_SPACE: type = KeyType.Backspace; break;
//                        case KeyEvent.VK_PAGE_UP: type = KeyType.PageUp; break;
//                        case KeyEvent.VK_PAGE_DOWN: type = KeyType.PageDown; break;
//                        default:
//                            System.err.println("unhandled key: "+ e.getKeyCode());
//                            break;
//                    }
//                    if (type == null)
//                        return false;
//
//                    k = new KeyStroke(type, e.isControlDown(), e.isAltDown(), e.isShiftDown());
//                } else {
//                    k = new KeyStroke(e.getKeyChar(), e.isControlDown(), e.isAltDown());
//                }
                System.out.println("in: " + e);
                //term.addInput(k);

                outgoing.add((byte) e.getKeyCode());



                return true;
            }
        }, 1000, 600);
    }


//    public static void main(String[] args) throws IOException {
//
//        Terminal terminal = new DefaultTerminalFactory().createTerminal();
//        System.out.println( new TerminalScreen(new UnixTerminal()).getFrontCharacter(0,0) );
//
//        DefaultVirtualTerminal vt1 = ShellTerminal.build(80, 25,
//                "slashem"
//                //"/bin/sh"
//                //"/bin/bash"
//                //"/bin/zsh"
//                //"/usr/bin/fish"
//        )/*.input(
//            "slashem\n"
//            //"top\n"
//            //"htop\n"
//            //"w\nfree\n"
//        );*/
//        ;
//
//
//        SpaceGraph.window(new ConsoleSurface(vt1), 1200, 900);
//    }


    /**
     * height/width of each cell
     */
    final float charAspect = 1.25f;
    private int cols, rows;

    /**
     * cursor position
     */
    int curx, cury;

    /**
     * percent of each grid cell width filled with the character
     */
    float charScaleX = 0.75f;

    /**
     * percent of each grid cell height filled with the character
     */
    float charScaleY = 0.6f;


    float bgAlpha = 0.5f;
    float fgAlpha = 0.9f;


    public ConsoleSurface(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
    }


    @Override
    public void paint(GL2 gl) {


        float charScaleX = (float) 1 * this.charScaleX;
        float charScaleY = charAspect * this.charScaleY;

        float dz = 0f;


        long t = System.currentTimeMillis(); //HACK


        gl.glPushMatrix();

        gl.glScalef(1f / cols, 1f / (rows * charAspect), 1f);

        gl.glLineWidth(3f);


        for (int row = 0; row < rows; row++) {

            gl.glPushMatrix();

            Draw.textStart(gl,
                    charScaleX, charScaleY,
                    //0, (rows - 1 - jj) * charAspect,
                    0.5f, (rows - 1 - row) * charAspect,
                    dz);

            for (int col = 0; col < cols; col++) {


                TextCharacter c = charAt(col, row);
                if (c == null)
                    continue;

                //TODO: Background color

//                    TextColor backColor = c.getBackgroundColor();
//                    if (backColor!=null) {
//
//                        gl.glColor4f(
//                                backColor.get(),
//                                backColor.green(), backColor.blue(), bgAlpha);
//                        Draw.rect(gl,
//                                (float) i, 0,
//                                (float) 1*16, charAspect*20
//                                ,-dz
//                        );
//                    }


                char cc = visible(c.getCharacter());
                if ((cc != 0) && (cc != ' ')) {
                    //TextColor fg = c.fore;

                    //TODO: if (!fg.equals(previousFG))
                    //gl.glColor4f(fg.red(), fg.green(), fg.blue(), fgAlpha);
                    gl.glColor3f(1f, 1f, 1f);

                    Draw.textNext(gl, cc, col);
                }
            }


            gl.glPopMatrix(); //next line


        }


        //DRAW CURSOR
        float p = (1f + (float) Math.sin(t / 100.0)) * 0.5f;
        gl.glColor4f(1f, 0.5f, 0f, 0.3f + p * 0.4f);
        float m = (0.5f + 2f * p);
        Draw.rect(gl,
                (float) charScaleX * (curx + 0.5f + m / 2f), charScaleY * (rows - 1 - cury + 0.5f + m / 2f),
                (float) charScaleX * (1 - m), charScaleY * (1 - m)
                , -dz
        );

        gl.glPopMatrix();

    }

    abstract public TextCharacter charAt(int col, int row);

    private String[] lines() {
        return new String[]{"wtf, wtf", "xxkjv"};
    }

    public char visible(char cc) {
        //HACK: un-ANSIfy

        switch (cc) {
            case 9474:
                cc = '|';
                break;
            case 9472:
                cc = '-';
                break;
            case 9492:
                //..
            case 9496:
                cc = '*';
                break;
        }
        return cc;
    }

    @Override
    public Appendable append(CharSequence charSequence) throws IOException {
        for (int i = 0; i < charSequence.length(); i++)
            append(charSequence.charAt(i));
        return this;
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i1) throws IOException {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Appendable append(char c) throws IOException {
        //this.term.put(c);
        return this;
    }


//    public static class EditTerminal extends DefaultVirtualTerminal {
//        public MultiWindowTextGUI gui;
//
//        public EditTerminal(int c, int r) {
//            super(c, r);
//
//
//            //term.clearScreen();
//            new Thread(() -> {
//
//                try {
//                    TerminalScreen screen = new TerminalScreen(this);
//                    screen.startScreen();
//                    gui = new MultiWindowTextGUI(
//                            new SeparateTextGUIThread.Factory(),
//                            screen);
//
//
//                    setCursorVisible(true);
//
//                    gui.setBlockingIO(false);
//                    gui.setEOFWhenNoWindows(false);
//
//
//                    final BasicWindow window = new BasicWindow();
//                    window.setPosition(new TerminalPosition(0,0));
//                    window.setSize(new TerminalPosition(c-2,r-2));
//
//
//                    TextBox t = new TextBox("", TextBox.Style.MULTI_LINE);
//                    t.setPreferredSize(new TerminalPosition(c-3,r-3));
//
//                    t.takeFocus();
//                    window.setComponent(t);
//
//
//                    gui.addWindow(window);
//                    gui.setActiveWindow(window);
//
//                    refresh();
//                    gui.waitForWindowToClose(window);
//
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }).start();
//
//        }
//
//        public void refresh() {
//            try {
//                gui.updateScreen();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//    }

//    public static class Demo extends DefaultVirtualTerminal {
//        public Demo(int c, int r) {
//            super(c, r);
//
//            new Thread(() -> {
//
//                Screen screen = null;
//                try {
//
//                    //term.clearScreen();
//
//                    screen = new TerminalScreen(this);
//                    screen.startScreen();
//                    MultiWindowTextGUI gui = new MultiWindowTextGUI(
//                            new SeparateTextGUIThread.Factory(),
//                            screen);
//
//                    gui.setBlockingIO(false);
//                    gui.setEOFWhenNoWindows(false);
//
//
//                    final BasicWindow window = new BasicWindow("Grid layout test");
//                    addSampleWindow(gui, window);
//                    gui.updateScreen();
//
////                TextGraphics tGraphics = screen.newTextGraphics();
//////
////                tGraphics.setForegroundColor(new TextColor.RGB(0, 125, 255));
////                tGraphics.setBackgroundColor(new TextColor.RGB(255, 125, 5));
////                tGraphics.drawRectangle(
////                        new TerminalPosition(3,3), new TerminalPosition(10,10), '*');
//
//
////                PrintStream ps = new PrintStream(new ByteArrayOutputStream());
////                WriteInput w = new WriteInput(ps, term);
////                w.start();
//
//                    setCursorVisible(true);
//                    fore(TextColor.ANSI.YELLOW);
//                    back(TextColor.ANSI.BLUE);
//                    moveCursorTo(0, 0);
//                    put("XYZ\nABC\nCDDFS");
//
//
//                    new Thread(() -> {
//
//                        while(true) {
//                            try {
//                                put(Integer.toString((int)(10000 * Math.random()), 36));
//                                if (Math.random() < 0.5f)
//                                    put(' ');
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                            Util.pause(100);
//                        }
//                    }).start();
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//
//            }).start();
//
//
//        }
//
//        public static void addSampleWindow(MultiWindowTextGUI gui, final BasicWindow window) {
//            Panel leftGridPanel = new Panel();
//            leftGridPanel.setLayoutManager(new com.googlecode.lanterna.gui2.GridLayout(4));
//            add(leftGridPanel);
//            add(leftGridPanel, TextColor.ANSI.BLUE, 4, 2);
//            add(leftGridPanel, TextColor.ANSI.CYAN, 4, 2);
//            add(leftGridPanel, TextColor.ANSI.GREEN, 4, 2);
//
//            leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.MAGENTA, new TerminalPosition(4, 2))
//                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER, true, false, 4, 1)));
//            leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.RED, new TerminalPosition(4, 2))
//                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, false, 4, 1)));
//            leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.YELLOW, new TerminalPosition(4, 2))
//                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, true, false, 4, 1)));
//            leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLACK, new TerminalPosition(4, 2))
//                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.CENTER, true, false, 4, 1)));
//
//            Panel rightGridPanel = new Panel();
//            rightGridPanel.setLayoutManager(new GridLayout(5));
//            TextColor.ANSI c = TextColor.ANSI.BLACK;
//            int columns = 4;
//            int rows = 2;
//            add(rightGridPanel, c, columns, rows);
//            rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.MAGENTA, new TerminalPosition(4, 2))
//                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.BEGINNING, false, true, 1, 4)));
//            rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.RED, new TerminalPosition(4, 2))
//                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, false, true, 1, 4)));
//            rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.YELLOW, new TerminalPosition(4, 2))
//                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.END, false, true, 1, 4)));
//            rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLACK, new TerminalPosition(4, 2))
//                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.FILL, false, true, 1, 4)));
//            add(rightGridPanel, TextColor.ANSI.BLUE, 4, 2);
//            add(rightGridPanel, TextColor.ANSI.CYAN, 4, 2);
//            add(rightGridPanel, TextColor.ANSI.GREEN, 4, 2);
//
//            Panel contentPanel = new Panel();
//            contentPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
//            contentPanel.addComponent(Panels.horizontal(leftGridPanel, new EmptySpace(TerminalPosition.ONE), rightGridPanel));
//            contentPanel.addComponent(new EmptySpace(TerminalPosition.ONE));
//            contentPanel.addComponent(new Button("Close", new Runnable() {
//                @Override
//                public void run() {
//                    window.close();
//                }
//            }));
//            window.setComponent(contentPanel);
//            gui.addWindow(window);
//        }
//
//        public static void add(Panel rightGridPanel, TextColor.ANSI c, int columns, int rows) {
//            rightGridPanel.addComponent(new EmptySpace(c, new TerminalPosition(columns, rows)));
//        }
//
//        public static void add(Panel leftGridPanel) {
//
//            add(leftGridPanel, TextColor.ANSI.BLACK, 4, 2);
//        }
//
//    }
}
