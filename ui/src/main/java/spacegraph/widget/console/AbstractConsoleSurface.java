package spacegraph.widget.console;

import com.googlecode.lanterna.TextCharacter;
import com.jogamp.opengl.GL2;
import spacegraph.Surface;

import java.awt.*;

public abstract class AbstractConsoleSurface extends Surface implements Appendable {
    protected Color bg;
    protected int rows, cols;

    public static char visible(char cc) {
        //HACK: un-ANSIfy

        //see: https://github.com/Hexworks/zircon/blob/master/src/main/kotlin/org/codetome/zircon/Symbols.kt

        switch (cc) {
            case ' ':
                return 0;
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

    public void resize(int cols, int rows) {
        this.cols = cols;
        this.rows = rows;
        //align(Align.Center, cols/1.5f, rows);
    }

    @Override
    public abstract void paint(GL2 gl);

    /** x,y aka col,row */
    public abstract int[] getCursorPos();

    abstract public TextCharacter charAt(int col, int row);
}
