package spacegraph.widget.console;

import com.googlecode.lanterna.TextCharacter;
import spacegraph.widget.Widget;

import java.awt.*;

public abstract class AbstractConsoleSurface extends Widget implements Appendable {
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


    /** x,y aka col,row */
    public abstract int[] getCursorPos();

    abstract public TextCharacter charAt(int col, int row);
}
