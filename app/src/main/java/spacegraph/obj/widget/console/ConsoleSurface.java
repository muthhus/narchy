package spacegraph.obj.widget.console;

import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.jcraft.jsch.JSchException;
import com.jogamp.opengl.GL2;
import org.apache.commons.io.output.TeeOutputStream;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.render.Draw;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by me on 4/1/16.
 */
public abstract class ConsoleSurface extends Surface implements Appendable {

    public static void main(String[] args) throws InterruptedException, IOException, JSchException {


        DefaultVirtualTerminal term = new DefaultVirtualTerminal();

        SpaceGraph.window(new ConsoleSurface(80, 24) {

            @Override
            public char charAt(int col, int row) {
                TextCharacter tc = term.getCharacter(col, row);
                if (tc != null) {
                    return tc.getCharacter();
                }
                return 0;
            }
        }, 800, 800);

        //final Terminal rawTerminal = new DefaultTerminalFactory().createSwingTerminal();
        //final Terminal rawTerminal = new UnixTerminal();

        SSH s = new SSH("gest", "localhost", "tseg", System.in, new TeeOutputStream(System.out, new OutputStream() {
            @Override
            public void write(int i) throws IOException {
                term.putCharacter((char) i);
            }

            @Override
            public void flush() throws IOException {
                term.flush();
            }
        }));

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


                //TextCharacter c = i < line.size() ? line.get(i) : null;
                char c = charAt(col, row);

                //TODO: Background color
//
//                    TextColor backColor = c.back;
//                    if (backColor!=null) {
//
//                        gl.glColor4f(
//                                backColor.red(),
//                                backColor.green(), backColor.blue(), bgAlpha);
//                        Draw.rect(gl,
//                                (float) i, 0,
//                                (float) 1*16, charAspect*20
//                                ,-dz
//                        );
//                    }


                char cc = visible(c);
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

    abstract public char charAt(int col, int row);

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
