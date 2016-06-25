package bulletphys.ui;

import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.gui2.*;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.util.JoglSpace;

import java.awt.*;


/**
 * Created by me on 4/1/16.
 */
public class Console {

    public static void main(String[] args) {

        //new FoodSpawnWorld1(sim, 128, 48, 48, 0.5f);


        Terminal terminal = new Terminal(50, 20, 0.05f);



//        sim.game.createEntity().edit()
//                .add(terminal)
//                .add(new DrawAbove(terminal)); //HACK


        new Thread(() -> {

            Screen screen = null;
            try {
                screen = new TerminalScreen(terminal.term);
                screen.startScreen();

                MultiWindowTextGUI gui = new MultiWindowTextGUI(new SeparateTextGUIThread.Factory(), screen);


                final BasicWindow window = new BasicWindow("Grid layout test");


                com.googlecode.lanterna.gui2.Panel leftGridPanel = new com.googlecode.lanterna.gui2.Panel();
                leftGridPanel.setLayoutManager(new com.googlecode.lanterna.gui2.GridLayout(4));
                leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLACK, new TerminalSize(4, 2)));
                leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLUE, new TerminalSize(4, 2)));
                leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.CYAN, new TerminalSize(4, 2)));
                leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.GREEN, new TerminalSize(4, 2)));

                leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.MAGENTA, new TerminalSize(4, 2))
                        .setLayoutData(com.googlecode.lanterna.gui2.GridLayout.createLayoutData(com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING, com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, true, false, 4, 1)));
                leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.RED, new TerminalSize(4, 2))
                        .setLayoutData(com.googlecode.lanterna.gui2.GridLayout.createLayoutData(com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, true, false, 4, 1)));
                leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.YELLOW, new TerminalSize(4, 2))
                        .setLayoutData(com.googlecode.lanterna.gui2.GridLayout.createLayoutData(com.googlecode.lanterna.gui2.GridLayout.Alignment.END, com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, true, false, 4, 1)));
                leftGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLACK, new TerminalSize(4, 2))
                        .setLayoutData(com.googlecode.lanterna.gui2.GridLayout.createLayoutData(com.googlecode.lanterna.gui2.GridLayout.Alignment.FILL, com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, true, false, 4, 1)));

                com.googlecode.lanterna.gui2.Panel rightGridPanel = new com.googlecode.lanterna.gui2.Panel();
                rightGridPanel.setLayoutManager(new com.googlecode.lanterna.gui2.GridLayout(5));
                rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLACK, new TerminalSize(4, 2)));
                rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.MAGENTA, new TerminalSize(4, 2))
                        .setLayoutData(com.googlecode.lanterna.gui2.GridLayout.createLayoutData(com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, com.googlecode.lanterna.gui2.GridLayout.Alignment.BEGINNING, false, true, 1, 4)));
                rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.RED, new TerminalSize(4, 2))
                        .setLayoutData(com.googlecode.lanterna.gui2.GridLayout.createLayoutData(com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, false, true, 1, 4)));
                rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.YELLOW, new TerminalSize(4, 2))
                        .setLayoutData(com.googlecode.lanterna.gui2.GridLayout.createLayoutData(com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, com.googlecode.lanterna.gui2.GridLayout.Alignment.END, false, true, 1, 4)));
                rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLACK, new TerminalSize(4, 2))
                        .setLayoutData(com.googlecode.lanterna.gui2.GridLayout.createLayoutData(com.googlecode.lanterna.gui2.GridLayout.Alignment.CENTER, com.googlecode.lanterna.gui2.GridLayout.Alignment.FILL, false, true, 1, 4)));
                rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.BLUE, new TerminalSize(4, 2)));
                rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.CYAN, new TerminalSize(4, 2)));
                rightGridPanel.addComponent(new EmptySpace(TextColor.ANSI.GREEN, new TerminalSize(4, 2)));

                com.googlecode.lanterna.gui2.Panel contentPanel = new com.googlecode.lanterna.gui2.Panel();
                contentPanel.setLayoutManager(new LinearLayout(Direction.VERTICAL));
                contentPanel.addComponent(Panels.horizontal(leftGridPanel, new EmptySpace(TerminalSize.ONE), rightGridPanel));
                contentPanel.addComponent(new EmptySpace(TerminalSize.ONE));
                contentPanel.addComponent(new com.googlecode.lanterna.gui2.Button("Close", new Runnable() {
                    @Override
                    public void run() {
                        window.close();
                    }
                }));
                window.setComponent(contentPanel);
                gui.addWindow(window);

                //TextGraphics tGraphics = screen.newTextGraphics();
                //screen.startScreen();
                //screen.clear();
                /*tGraphics.drawRectangle(
                        new TerminalPosition(3,3), new TerminalSize(10,10), '*');*/
                //screen.refresh();

                //PrintStream ps = new PrintStream(new ByteArrayOutputStream());
                //new WriteInput(ps, terminal.term).start();


                /*terminal.term.putString("abcd\nsjdfhkjsd\n\ndsfjsdflksdjf");
                terminal.term.putCharacter('\n');
                terminal.term.putCharacter('x');
                terminal.term.putCharacter('y');*/

                //screen.readInput();
                //screen.stopScreen();


                AsynchronousTextGUIThread guiThread = (AsynchronousTextGUIThread) gui.getGUIThread();
                guiThread.start();
                guiThread.waitForStop();


                screen.stopScreen();

            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();

        new JoglSpace() {

            @Override
            public void display(GLAutoDrawable drawable) {
                terminal.render(gl);
            }

            @Override
            public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {

            }
        }.show(500,500);

        //sim.run(25);
    }

    /**
     * http://www.java-tips.org/other-api-tips-100035/112-jogl/1689-outline-fonts-nehe-tutorial-jogl-port.html
     */
    public static class Terminal  {
        final TerminalVirtual term;
        private final float scale;
        final GLUT glut = new GLUT();
        final static int font = GLUT.STROKE_MONO_ROMAN;
        private final float fontWidth;
        final float fontScale;
        private final float fontHeight;

        public Terminal(int w, int h, float scale) {
            this.scale = scale;
            term = new TerminalVirtual(w, h);
            fontWidth = glut.glutStrokeWidthf(font, 'X');
            fontHeight = glut.glutStrokeLengthf(font, "X");

            fontScale = 1 / fontWidth;
            //term = new TerminalANSI(w, h, 1) {
                /*@Override
                public void repaint() {
                    System.out.println("repaint " + this);
                    super.repaint();
                }*/
            //};

        }


        void renderText(GL2 gl, float x, float y, float cw, float ch, char c) {
            // Center Our Text On The Screen
            //float width = glut.glutStrokeLength(font, string);
            //gl.glTranslatef(-width / 2f, 0, 0);
            // Render The Text

            gl.glPushMatrix();


            float hw = 0.5f;

            gl.glTranslatef(x - hw, y - hw, 0);

            gl.glScalef(fontScale*cw, fontScale*ch, 1);
            glut.glutStrokeCharacter(GLUT.STROKE_MONO_ROMAN, c);
            gl.glPopMatrix();


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

        public void render(GL2 gl) {
            TerminalSize ts = term.getTerminalSize();

            float cw = scale /2f ;
            float ch = scale;


            int rows = ts.getRows();
            for (int j = 0; j < rows; j++) {
                for (int i = 0; i < ts.getColumns(); i++) {



                    float x = i * cw;
                    int r = rows - 1 - j;
                    float y = j * ch;

                    TextCharacter c = term.getCharacter(i, r);
                    char cc = c.getCharacter();

                    Color bg = c.getBackgroundColor().toColor();
                    Color fg = c.getForegroundColor().toColor();

                    //draw.drawSolidRect(x, y, cw, ch, -0.1f, bg.getRed()/256f, bg.getGreen()/256f, bg.getBlue()/256f);
                    gl.glColor3f(bg.getRed()/256f, bg.getGreen()/256f, bg.getBlue()/256f);
                    float sf = 1;
                    gl.glRectf(x*sf, -y*sf, x*sf + sf, -y*sf - sf);

                    //un-ANSIfy
                    switch (cc) {
                        case 9474: cc = '|'; break;
                        case 9472: cc = '-'; break;
                        case 9492:
                        //..
                        case 9496: cc = '*'; break;
                    }

                    if ((cc != 0) && (cc != ' ')) {
                        gl.glColor3f(fg.getRed()/256f, fg.getGreen()/256f, fg.getBlue()/256f);
                        renderText(gl, x, y, cw, ch, cc);
                    }
                }
            }

        }
    }

    static class TerminalVirtual extends DefaultVirtualTerminal {

        public TerminalVirtual(int w, int h) {
            super(w, h);
        }
//        public TerminalANSI() {
//            this(DefaultTerminalFactory.DEFAULT_INPUT_STREAM, DefaultTerminalFactory.DEFAULT_OUTPUT_STREAM, DefaultTerminalFactory.DEFAULT_CHARSET);
//        }
//
//        public TerminalANSI(InputStream terminalInput, OutputStream terminalOutput, Charset terminalCharset) {
//            super(terminalInput, terminalOutput, terminalCharset);
//            onResized(80,24);
//        }
    }

}
