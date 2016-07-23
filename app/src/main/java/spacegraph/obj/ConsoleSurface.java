package spacegraph.obj;

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
import com.jogamp.opengl.util.gl2.GLUT;
import nars.util.Util;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.render.ShapeDrawer;

import java.io.IOException;
import java.util.List;

import static spacegraph.render.JoglSpace.glut;

/**
 * Created by me on 4/1/16.
 */
public class ConsoleSurface extends Surface {



    public static void main(String[] args) throws IOException {

        DefaultVirtualTerminal vt1 = ShellTerminal.build(80, 25,
                "/bin/bash"
                //"/bin/zsh"
                //"/usr/bin/fish"
        ).input(
            //"htop\n"
            "w\nfree\n"
        );


        new SpaceGraph<VirtualTerminal>(
                new RectWidget(new ConsoleSurface(vt1), 8, 8)
        ).show(800, 800);
    }


    /**
     * http://www.java-tips.org/other-api-tips-100035/112-jogl/1689-outline-fonts-nehe-tutorial-jogl-port.html
     */
    final VirtualTerminal term;

    final static int font = GLUT.STROKE_MONO_ROMAN;
    private final float fontWidth;
    final float fontUnscale;
    private final float fontHeight;

    float bgAlpha = 0.5f;
    float fgAlpha = 0.9f;


    public ConsoleSurface(int cols, int rows) {
        this(new DefaultVirtualTerminal(cols, rows));
    }

    public static RectWidget<VirtualTerminal> widget(int cols, int rows) {
        return widget(new DefaultVirtualTerminal(cols, rows));
    }

    public static RectWidget<VirtualTerminal> widget(VirtualTerminal vt) {
        return widget(new ConsoleSurface(vt));
    }

    public static RectWidget<VirtualTerminal> widget(ConsoleSurface s) {
        return new RectWidget(s.term, s, 1, 1);
    }

    public ConsoleSurface(VirtualTerminal term) {
        this.term = term;
        fontWidth = glut.glutStrokeWidthf(font, 'X');
        fontHeight = fontWidth * 1.25f; //glut.glutStrokeLengthf(font, "X");

        fontUnscale = 1 / fontHeight;
    }


    @Override
    public void paint(GL2 gl) {
        TerminalPosition ts = term.terminalSize();

        int rows = ts.row;
        int cols = ts.col;


        float cw = 1;
        float aspect = fontHeight / fontWidth;
        float ch = 1 * aspect;

        float charUnscaleX = fontUnscale * cw;
        float charUnscaleY = fontUnscale * ch;

        float charScaleX = 1 * charUnscaleX;
        float charScaleY = 1 * charUnscaleY ;


        float dz = 0.1f;

        gl.glPushMatrix();

        float th = (rows * ch);
        float tw = (cols * cw);

        gl.glScalef(1 / tw, 1 / th, 1f);

        gl.glLineWidth(2f);

        int cury = term.cursor().row;
        int curx = term.cursor().col;

        long t = System.currentTimeMillis(); //HACK

        final int[] j = {0};

        term.view(0, rows, (List<TextCharacter> line) -> {

            gl.glPushMatrix();

            int jj = j[0];
            gl.glTranslatef(0, (rows - 1 - jj) * ch, dz);

            for (int i = 0; i < cols; i++) {


                TextCharacter c = i < line.size() ? line.get(i) : null;

                if (c!=null) {

                    TextColor backColor = c.back;
                    if (backColor!=null) {

                        gl.glColor4f(
                                backColor.red(),
                                backColor.green(), backColor.blue(), bgAlpha);
                        ShapeDrawer.rect(gl,
                                cw * i, 0,
                                cw, ch
                                ,-dz
                        );
                    }


                    char cc = displayChar(c);
                    if ((cc != 0) && (cc != ' ')) {
                        TextColor fg = c.fore;

                        gl.glColor4f(fg.red(), fg.green(), fg.blue(), fgAlpha);

                        gl.glPushMatrix();
                        gl.glTranslatef(cw*i, +charScaleY, 0);

                        gl.glScalef(charScaleX, charScaleY, 1f);
                        glut.glutStrokeCharacter(GLUT.STROKE_MONO_ROMAN, cc);
                        gl.glPopMatrix();

                    }
                }

                if ((i == curx) && (jj == cury)) {
                    float p = (1f + (float)Math.sin(t/100.0)) * 0.5f;
                    gl.glColor4f( 1f, 0.5f,0f, 0.3f + p * 0.4f);
                    float m = -(0.1f + 0.3f * p);
                    ShapeDrawer.rect(gl,
                            cw * i + m, 0+m,
                            cw-m, ch-m
                            ,-dz-m
                    );
                }

            }

            gl.glPopMatrix(); //next line

            j[0]++;
        });

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

    public static class DummyTerminal extends DefaultVirtualTerminal {
        public DummyTerminal(int c, int r) {
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
                        Util.pause(1500);
                        for (int i = 0; i < 100; i++) {
                            try {
                                put(Integer.toString(i, 2) + "\n");
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Util.pause(1000);
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
