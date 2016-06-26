package nars.gui.graph.matter;

import bulletphys.ui.ShapeDrawer;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.gui.graph.GraphSpace;
import nars.gui.graph.Surface;
import nars.gui.graph.SurfaceMount;
import nars.util.Util;

import java.io.IOException;

import static nars.util.JoglSpace.glut;

/**
 * Created by me on 4/1/16.
 */
public class ConsoleSurface extends Surface {


    public static void main(String[] args) {
        new GraphSpace<VirtualTerminal>(
                vt -> ConsoleSurface.widget(vt),
                new DefaultVirtualTerminal(80,25)
        ).show(800,800);
    }


    /**
     * http://www.java-tips.org/other-api-tips-100035/112-jogl/1689-outline-fonts-nehe-tutorial-jogl-port.html
     */
    final VirtualTerminal term;

    final static int font = GLUT.STROKE_MONO_ROMAN;
    private final float fontWidth;
    final float fontUnscale;
    private final float fontHeight;

    public ConsoleSurface(int cols, int rows) {
        this(new DefaultVirtualTerminal(cols, rows));
    }

    public static SurfaceMount<VirtualTerminal> widget(int cols, int rows) {
        return widget(new DefaultVirtualTerminal(cols, rows));
    }

    public static SurfaceMount<VirtualTerminal> widget(VirtualTerminal vt) {
        return widget(new ConsoleSurface(vt));
    }

    public static SurfaceMount<VirtualTerminal> widget(ConsoleSurface s) {
        return new SurfaceMount(s.term, s);
    }

    public ConsoleSurface(VirtualTerminal term) {
        this.term = term;
        fontWidth = glut.glutStrokeWidthf(font, 'X');
        fontHeight = fontWidth * 1.6f; //glut.glutStrokeLengthf(font, "X");

        fontUnscale = 1 / fontWidth;
        //term = new TerminalANSI(w, h, 1) {
                /*@Override
                public void repaint() {
                    System.out.println("repaint " + this);
                    super.repaint();
                }*/
        //};

        new Thread(() -> {

            Screen screen = null;
            try {

                //term.clearScreen();

                screen = new TerminalScreen(term);
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

                term.setCursorVisible(true);
                term.fore(TextColor.ANSI.YELLOW);
                term.back(TextColor.ANSI.BLUE);
                term.moveCursorTo(0,0);
                term.put("XYZ\nABC\nCDDFS");


                new Thread(()->{
                    Util.pause(1500);
                    for (int i= 0; i < 100; i++) {
                        try {
                            term.put(Integer.toString(i, 2) + "\n");
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




//        void renderText(GL2 gl, int font, String string) {
//            // Center Our Text On The Screen
//            //float width = glut.glutStrokeLength(font, string);
//            //gl.glTranslatef(-width / 2f, 0, 0);
//            // Render The Text
//            for (int i = 0; i < string.length(); i++) {
//                char c = string.charAt(i);
//                glut.glutStrokeCharacter(font, c);
//            }
//        }

    public void paint(GL2 gl) {
        TerminalPosition ts = term.terminalSize();

        int rows = ts.row;
        int cols = ts.column;

        float cw = 1;
        float aspect = fontHeight / fontWidth;
        float ch = 1 * aspect;
        float charUnscaleX = fontUnscale * cw;
        float charUnscaleY = fontUnscale * ch;

        gl.glPushMatrix();

        float th = (rows * ch);
        float tw = (cols * cw);

        gl.glScalef(1/tw, 1/th, 1f);

        gl.glLineWidth(2f);

        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {

                float x = i;
                int r = rows - 1 - j;
                float y = j;

                gl.glPushMatrix();

                gl.glTranslatef(x * cw, y * ch, 0);

                TextCharacter c = term.getView(i, r);

                TextColor backColor = c.back;

                //draw.drawSolidRect(x, y, cw, ch, -0.1f, bg.getRed()/256f, bg.getGreen()/256f, bg.getBlue()/256f);
                float bgAlpha = 0.75f;

                gl.glColor4f(
                        backColor.red(),
                        backColor.green(), backColor.blue(), bgAlpha);

                ShapeDrawer.rect(gl,
                        0, 0,
                        cw, ch
                        //,-1f
                );

                char cc = displayChar(c);
                if ((cc != 0) && (cc != ' ')) {
                    TextColor fg = c.fore;
                    gl.glColor3f(fg.red(), fg.green(), fg.blue());
                    // Center Our Text On The Screen
                    //float width = glut.glutStrokeLength(font, string);
                    //gl.glTranslatef(-width / 2f, 0, 0);
                    // Render The Text


                    gl.glPushMatrix();

                    gl.glScalef(charUnscaleX, charUnscaleY, 1f);
                    glut.glutStrokeCharacter(GLUT.STROKE_MONO_ROMAN, cc);
                    gl.glPopMatrix();

                }

                gl.glPopMatrix();
            }
        }

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
