package spacegraph.obj.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Created by me on 11/14/16.
 */
public class VirtualConsole extends ConsoleSurface implements Appendable {

    final DefaultVirtualTerminal term;
    private final int[] cursorPos = new int[2];

    public VirtualConsole(int cols, int rows) {

        super(cols, rows);
        term = new DefaultVirtualTerminal(new TerminalSize(cols, rows));
    }

    @Override public Appendable append(CharSequence c) {
        int l = c.length();
        for (int i = 0; i < l; i++) {
            append(c.charAt(i));
        }
        return this;
    }

    @Override
    public Appendable append(char c)  {
        term.putCharacter(c);
        return this;
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i1)  {
        throw new UnsupportedOperationException("TODO");
    }


    public OutputStream output() {
        return new OutputStream() {

            @Override public void write(int i) {
                append((char)i);
            }

            @Override
            public void flush() throws IOException {
                term.flush();
            }
        };
    }

    @Override
    public int[] getCursorPos() {
        TerminalPosition p = term.getCursorPosition();
        cursorPos[0] = p.getColumn();
        cursorPos[1] = p.getRow();
        return cursorPos;
    }

    @Override
    public TextCharacter charAt(int col, int row) {
        return term.getCharacter(col, row);
    }


}
