package nars.rover.run;

import com.artemis.Component;
import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.io.WriteInput;
import com.googlecode.lanterna.screen.Screen;
import com.googlecode.lanterna.screen.TerminalScreen;
import com.googlecode.lanterna.terminal.swing.GraphicalTerminalImplementation;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorColorConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalEmulatorDeviceConfiguration;
import com.googlecode.lanterna.terminal.swing.TerminalScrollController;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.gl2.GLUT;
import nars.rover.Sim;
import nars.rover.obj.DrawAbove;
import nars.rover.physics.gl.JoglAbstractDraw;
import nars.rover.physics.j2d.LayerDraw;
import org.codehaus.plexus.util.StringOutputStream;
import org.jbox2d.dynamics.World2D;

import java.awt.*;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static nars.rover.run.DemoTerminal.TerminalSwing.font;
import static nars.rover.run.NEHEBitmapFont.glPrint;

/**
 * Created by me on 4/1/16.
 */
public class DemoTerminal {

    public static void main(String[] args) {

        final Sim sim = new Sim(new World2D());
        //new FoodSpawnWorld1(sim, 128, 48, 48, 0.5f);



        Terminal terminal = new Terminal(50, 20, 1f);
        sim.game.createEntity().edit()
                .add(terminal)
                .add(new DrawAbove(terminal)); //HACK


        new Thread(() -> {

            Screen screen = null;
            try {
                screen = new TerminalScreen(terminal.term);
                TextGraphics tGraphics = screen.newTextGraphics();

                screen.startScreen();
                screen.clear();

                tGraphics.drawRectangle(
                        new TerminalPosition(3,3), new TerminalSize(10,10), '*');
                screen.refresh();

                //PrintStream ps = new PrintStream(new ByteArrayOutputStream());
                //new WriteInput(ps, terminal.term).start();


                terminal.term.putString("abcd\nsjdfhkjsd\n\ndsfjsdflksdjf");
                terminal.term.putCharacter('\n');
                terminal.term.putCharacter('x');
                terminal.term.putCharacter('y');

                //screen.readInput();
                //screen.stopScreen();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }).start();

        sim.run(25);
    }

    /** http://www.java-tips.org/other-api-tips-100035/112-jogl/1689-outline-fonts-nehe-tutorial-jogl-port.html */
    public static class Terminal extends Component implements LayerDraw {
        final TerminalVirtual term;
        private final float scale;
        final GLUT glut = new GLUT();
        final static int font = GLUT.STROKE_MONO_ROMAN;
        private final float fontWidth;
        final float fontScale;

        public Terminal(int w, int h, float scale) {
            this.scale = scale;
            term = new TerminalVirtual();
            fontWidth = glut.glutStrokeLength(font, "x");
            fontScale = 1 / fontWidth;
            //term = new TerminalANSI(w, h, 1) {
                /*@Override
                public void repaint() {
                    System.out.println("repaint " + this);
                    super.repaint();
                }*/
            //};

        }

        @Override
        public void drawGround(JoglAbstractDraw draw, World2D w) {

        }

        void renderText(GL2 gl, float x, float y, float scale, char c) {
            // Center Our Text On The Screen
            //float width = glut.glutStrokeLength(font, string);
            //gl.glTranslatef(-width / 2f, 0, 0);
            // Render The Text

            gl.glPushMatrix();


            float hw = 0.5f;

            gl.glTranslatef(x-hw, y-hw, 0);

            gl.glScalef(fontScale, fontScale, fontScale);
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

        @Override
        public void drawSky(JoglAbstractDraw draw, World2D w) {
            TerminalSize ts = null;
            ts = term.getTerminalSize();

            float cw = scale;
            float ch = scale;

            GL2 gl = draw.gl();

            for (int i = 0; i < ts.getColumns(); i++) {
                for (int j = 0; j < ts.getRows(); j++) {

                    TextCharacter c = term.getCharacter(i, j);
                    char cc = c.getCharacter();


                    float x = i * cw;
                    float y = j * ch;

                    float r = c.getCharacter() / 256.0f;

                    draw.drawSolidRect(x, y, cw, ch, -0.1f, r, r, r);

                    if ((cc != 0) && (cc!=' ')) {
                        gl.glColor3f(1.0f, 1.0f, 1.0f);
                        renderText(gl, i, j, scale, cc);
                    }
                }
            }

        }
    }

    static class TerminalVirtual extends DefaultVirtualTerminal {

        public TerminalVirtual() {
            super();
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

    static class TerminalSwing extends GraphicalTerminalImplementation {
        private final int width;
        private final int height;
        static final Font font = Font.getFont("Monospace");


        public TerminalSwing(int w, int h, float scale) {
            super(new TerminalSize(w, h), TerminalEmulatorDeviceConfiguration.getDefault(),
                    TerminalEmulatorColorConfiguration.getDefault(),
                    new TerminalScrollController.Null()
            );
            this.width = (int) (w * getFontWidth() * scale);
            this.height = (int) (h * getFontHeight() * scale);
        }

        @Override
        public int getFontHeight() {
            return 16;
        }

        @Override
        public int getFontWidth() {
            return 8;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public Font getFontForCharacter(TextCharacter character) {
            return font;
        }

        @Override
        public boolean isTextAntiAliased() {
            return false;
        }

        @Override
        public void repaint() {

        }
    }
}
