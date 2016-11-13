package spacegraph.obj.widget;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.ShellTerminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import nars.util.Util;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.Cuboid;
import spacegraph.render.Draw;

import java.io.IOException;
import java.util.List;

/**
 * Created by me on 4/1/16.
 */
public class ConsoleSurface extends Surface implements Appendable {



    public static void main(String[] args) throws IOException {

        DefaultVirtualTerminal vt1 = ShellTerminal.build(80, 25,
                "/bin/bash"
                //"/bin/zsh"
                //"/usr/bin/fish"
        ).input(
            //"top\n"
            "htop\n"
            //"w\nfree\n"
        );


        SpaceGraph.window(new ConsoleSurface(vt1), 800, 800);
    }


    /**
     * http://www.java-tips.org/other-api-tips-100035/112-jogl/1689-outline-fonts-nehe-tutorial-jogl-port.html
     */
    public final VirtualTerminal term;

    /** height/width of each cell */
    final float charAspect = 1.25f;

    /** percent of each grid cell width filled with the character */
    float charScaleX = 0.75f;

    /** percent of each grid cell height filled with the character */
    float charScaleY = 0.6f;


    float bgAlpha = 0.5f;
    float fgAlpha = 0.9f;


    public ConsoleSurface(int cols, int rows) {
        this(
            new DefaultVirtualTerminal(cols, rows)
            //new LoggerTerminal(cols, rows)
            //new ANSITerminal(cols, rows)
        );
    }

    public static Cuboid<VirtualTerminal> widget(int cols, int rows) {
        return widget(new DefaultVirtualTerminal(cols, rows));
    }

    public static Cuboid<VirtualTerminal> widget(VirtualTerminal vt) {
        return widget(new ConsoleSurface(vt));
    }

    public static Cuboid<VirtualTerminal> widget(ConsoleSurface s) {
        return new Cuboid(s.term, s, 1, 1);
    }

    public ConsoleSurface(VirtualTerminal term) {
        this.term = term;
    }


    @Override
    public void paint(GL2 gl) {
        TerminalPosition ts = term.terminalSize();

        int rows = ts.row;
        int cols = ts.col;

        float charScaleX = (float) 1 * this.charScaleX;
        float charScaleY = charAspect * this.charScaleY;

        float dz = 0f;

        TerminalPosition cursor = term.cursor();
        int cury = cursor.row;
        int curx = cursor.col;

        long t = System.currentTimeMillis(); //HACK

        final int[] j = {0};

        gl.glPushMatrix();

        gl.glScalef(1f / cols, 1f / (rows * charAspect), 1f);

        gl.glLineWidth(3f);

        term.view(0, rows, (List<TextCharacter> line) -> {

            gl.glPushMatrix();

            int jj = j[0];

            Draw.textStart(gl,
                    charScaleX, charScaleY,
                    //0, (rows - 1 - jj) * charAspect,
                    0.5f, (rows - 1 - jj) * charAspect,
                    dz);

            for (int i = 0; i < cols; i++) {


                TextCharacter c = i < line.size() ? line.get(i) : null;
                if (c!=null) {

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


                    char cc = displayChar(c);
                    if ((cc != 0) && (cc != ' ')) {
                        TextColor fg = c.fore;

                        //TODO: if (!fg.equals(previousFG))
                        gl.glColor4f(fg.red(), fg.green(), fg.blue(), fgAlpha);

                        Draw.textNext(gl, cc, i);
                    }
                }



            }

            gl.glPopMatrix(); //next line

            j[0]++;
        });


        //DRAW CURSOR
        float p = (1f + (float)Math.sin(t/100.0)) * 0.5f;
        gl.glColor4f( 1f, 0.5f,0f, 0.3f + p * 0.4f);
        float m = (0.5f + 2f * p);
        Draw.rect(gl,
                (float) charScaleX * (curx + 0.5f + m/2f), charScaleY * (rows - 1 - cury + 0.5f + m/2f),
                (float) charScaleX * (1 - m), charScaleY * (1 - m)
                ,-dz
        );

        gl.glPopMatrix();

    }

    public char displayChar(TextCharacter c) {
        //HACK: un-ANSIfy

        char cc = c.c;
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
        this.term.put(c);
        return this;
    }


    public static class EditTerminal extends DefaultVirtualTerminal {
        public MultiWindowTextGUI gui;

        public EditTerminal(int c, int r) {
            super(c, r);


            //term.clearScreen();
            new Thread(() -> {

                try {
                    TerminalScreen screen = new TerminalScreen(this);
                    screen.startScreen();
                    gui = new MultiWindowTextGUI(
                            new SeparateTextGUIThread.Factory(),
                            screen);


                    setCursorVisible(true);

                    gui.setBlockingIO(false);
                    gui.setEOFWhenNoWindows(false);


                    final BasicWindow window = new BasicWindow();
                    window.setPosition(new TerminalPosition(0,0));
                    window.setSize(new TerminalPosition(c-2,r-2));


                    TextBox t = new TextBox("", TextBox.Style.MULTI_LINE);
                    t.setPreferredSize(new TerminalPosition(c-3,r-3));

                    t.takeFocus();
                    window.setComponent(t);


                    gui.addWindow(window);
                    gui.setActiveWindow(window);

                    refresh();
                    gui.waitForWindowToClose(window);

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();

        }

        public void refresh() {
            try {
                gui.updateScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Demo extends DefaultVirtualTerminal {
        public Demo(int c, int r) {
            super(c, r);

            new Thread(() -> {

                Screen screen = null;
                try {

                    //term.clearScreen();

                    screen = new TerminalScreen(this);
                    screen.startScreen();
                    MultiWindowTextGUI gui = new MultiWindowTextGUI(
                            new SeparateTextGUIThread.Factory(),
                            screen);

                    gui.setBlockingIO(false);
                    gui.setEOFWhenNoWindows(false);


                    final BasicWindow window = new BasicWindow("Grid layout test");
                    addSampleWindow(gui, window);
                    gui.updateScreen();

//                TextGraphics tGraphics = screen.newTextGraphics();
////
//                tGraphics.setForegroundColor(new TextColor.RGB(0, 125, 255));
//                tGraphics.setBackgroundColor(new TextColor.RGB(255, 125, 5));
//                tGraphics.drawRectangle(
//                        new TerminalPosition(3,3), new TerminalPosition(10,10), '*');


//                PrintStream ps = new PrintStream(new ByteArrayOutputStream());
//                WriteInput w = new WriteInput(ps, term);
//                w.start();

                    setCursorVisible(true);
                    fore(TextColor.ANSI.YELLOW);
                    back(TextColor.ANSI.BLUE);
                    moveCursorTo(0, 0);
                    put("XYZ\nABC\nCDDFS");


                    new Thread(() -> {

                        while(true) {
                            try {
                                put(Integer.toString((int)(10000 * Math.random()), 36));
                                if (Math.random() < 0.5f)
                                    put(' ');
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Util.pause(100);
                        }
                    }).start();
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }).start();


        }

        public static void addSampleWindow(MultiWindowTextGUI gui, final BasicWindow window) {
            Panel leftGridPanel = new Panel();
            leftGridPanel.setLayoutManager(new com.googlecode.lanterna.gui2.GridLayout(4));
            add(leftGridPanel);
            add(leftGridPanel, TextColor.ANSI.BLUE, 4, 2);
            add(leftGridPanel, TextColor.ANSI.CYAN, 4, 2);
            add(leftGridPanel, TextColor.ANSI.GREEN, 4, 2);

            leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.MAGENTA, new TerminalPosition(4, 2))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.BEGINNING, GridLayout.Alignment.CENTER, true, false, 4, 1)));
            leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.RED, new TerminalPosition(4, 2))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, true, false, 4, 1)));
            leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.YELLOW, new TerminalPosition(4, 2))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.END, GridLayout.Alignment.CENTER, true, false, 4, 1)));
            leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLACK, new TerminalPosition(4, 2))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.CENTER, true, false, 4, 1)));

            Panel rightGridPanel = new Panel();
            rightGridPanel.setLayoutManager(new GridLayout(5));
            TextColor.ANSI c = TextColor.ANSI.BLACK;
            int columns = 4;
            int rows = 2;
            add(rightGridPanel, c, columns, rows);
            rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.MAGENTA, new TerminalPosition(4, 2))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.BEGINNING, false, true, 1, 4)));
            rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.RED, new TerminalPosition(4, 2))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.CENTER, false, true, 1, 4)));
            rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.YELLOW, new TerminalPosition(4, 2))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.END, false, true, 1, 4)));
            rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLACK, new TerminalPosition(4, 2))
                    .setLayoutData(GridLayout.createLayoutData(GridLayout.Alignment.CENTER, GridLayout.Alignment.FILL, false, true, 1, 4)));
            add(rightGridPanel, TextColor.ANSI.BLUE, 4, 2);
            add(rightGridPanel, TextColor.ANSI.CYAN, 4, 2);
            add(rightGridPanel, TextColor.ANSI.GREEN, 4, 2);

            Panel contentPanel = new Panel();
            contentPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
            contentPanel.addComponent(Panels.horizontal(leftGridPanel, new EmptySpace(TerminalPosition.ONE), rightGridPanel));
            contentPanel.addComponent(new EmptySpace(TerminalPosition.ONE));
            contentPanel.addComponent(new Button("Close", new Runnable() {
                @Override
                public void run() {
                    window.close();
                }
            }));
            window.setComponent(contentPanel);
            gui.addWindow(window);
        }

        public static void add(Panel rightGridPanel, TextColor.ANSI c, int columns, int rows) {
            rightGridPanel.addComponent(new EmptySpace(c, new TerminalPosition(columns, rows)));
        }

        public static void add(Panel leftGridPanel) {

            add(leftGridPanel, TextColor.ANSI.BLACK, 4, 2);
        }

    }
}
