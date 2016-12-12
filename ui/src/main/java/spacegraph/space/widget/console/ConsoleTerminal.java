package spacegraph.space.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.jogamp.newt.event.KeyEvent;

import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by me on 11/14/16.
 */
public class ConsoleTerminal extends ConsoleSurface implements Appendable {

    final DefaultVirtualTerminal term;
    private final int[] cursorPos = new int[2];

    public ConsoleTerminal(int cols, int rows) {
        this(new DefaultVirtualTerminal(new TerminalSize(cols, rows)));
    }

    public ConsoleTerminal(DefaultVirtualTerminal t) {
        super(t.getTerminalSize().getColumns(), t.getTerminalSize().getRows());
        this.term = t;
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
            public void flush() {
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



    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {

        //return super.onKey(e, pressed);
        DefaultVirtualTerminal eterm = (DefaultVirtualTerminal) this.term;

        int cc = e.getKeyCode();
        if (pressed && cc== 13) {
            term.addInput( new KeyStroke(KeyType.Enter, e.isControlDown(), e.isAltDown()) );
        } else if (pressed && cc == 8) {
            term.addInput( new KeyStroke(KeyType.Backspace, e.isControlDown(), e.isAltDown()) );
        } else if (pressed && cc == 27) {
            term.addInput( new KeyStroke(KeyType.Escape, e.isControlDown(), e.isAltDown()) );
        } else if (e.isPrintableKey() && !e.isActionKey() && !e.isModifierKey()) {
            char c = e.getKeyChar();
            if (!TerminalTextUtils.isControlCharacter(c) && !pressed /* release */) {
                //eterm.gui.getActiveWindow().handleInput(
                term.addInput(
                        //eterm.gui.handleInput(
                        new KeyStroke(c, e.isControlDown(), e.isAltDown())
                );
            } else {
                return false;
            }
        } else if (pressed) {
            KeyType c = null;
            //System.out.println(" keycode: " + e.getKeyCode());
            switch (e.getKeyCode()) {
                case KeyEvent.VK_BACK_SPACE:  c = KeyType.Backspace; break;
                case KeyEvent.VK_ENTER: c = KeyType.Enter; break;
                case KeyEvent.VK_DELETE: c = KeyType.Delete; break;
                case KeyEvent.VK_LEFT: c = KeyType.ArrowLeft; break;
                case KeyEvent.VK_RIGHT: c = KeyType.ArrowRight; break;
                case KeyEvent.VK_UP: c = KeyType.ArrowUp; break;
                case KeyEvent.VK_DOWN: c = KeyType.ArrowDown; break;

                default:
                    System.err.println("character not handled: " + e);
                    return false;
            }



            //eterm.gui.handleInput(

            //eterm.gui.getActiveWindow().handleInput(
            term.addInput(
                    new KeyStroke(c, e.isControlDown(), e.isAltDown(), e.isShiftDown())
            );
            //                    KeyEvent.isModifierKey(KeyEvent.VK_CONTROL),
//                    KeyEvent.isModifierKey(KeyEvent.VK_ALT),
//                    KeyEvent.isModifierKey(KeyEvent.VK_SHIFT)
//            ));
        }

        AtomicBoolean busy = new AtomicBoolean(false);
        if (busy.compareAndSet(false,true)) {

            //this.term.flush();

            if (eterm instanceof ConsoleTerminal.EditTerminal) {
                try {
                    EditTerminal ee = (EditTerminal) eterm;
                    ee.gui.processInput();
                    busy.set(false);
                    ee.gui.updateScreen();
                } catch (IOException e1) {
                    e1.printStackTrace();
                    busy.set(false);
                }
            }



        }

        return true;
    }
}
