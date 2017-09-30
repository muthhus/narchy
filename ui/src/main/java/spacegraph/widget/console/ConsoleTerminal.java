package spacegraph.widget.console;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TerminalSize;
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminal;
import com.googlecode.lanterna.terminal.virtual.VirtualTerminalListener;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.opengl.GL2;
import org.jetbrains.annotations.Nullable;
import spacegraph.Surface;
import spacegraph.video.TextureSurface;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by me on 11/14/16.
 */
public class ConsoleTerminal extends AbstractConsoleSurface /*ConsoleSurface*/ {

    final TextureSurface texture = new TextureSurface();

    public final VirtualTerminal term;
    private final int[] cursorPos = new int[2];

    public ConsoleTerminal(int cols, int rows) {
        this(new DefaultVirtualTerminal(new TerminalSize(cols, rows)));
    }

    public ConsoleTerminal(VirtualTerminal t) {
        resize(t.getTerminalSize().getColumns(), t.getTerminalSize().getRows());
        this.term = t;
    }

    private void render() {
        needFullRedraw = true;

        if (needFullRedraw) {
            updateBackBuffer(0);
            texture.update(backbuffer);
            this.needFullRedraw = false;
        }
    }

    @Override
    public Appendable append(CharSequence c) {
        int l = c.length();
        for (int i = 0; i < l; i++) {
            append(c.charAt(i));
        }
        return this;
    }

    @Override
    public Appendable append(char c) {
        term.putCharacter(c);
        return this;
    }

    @Override
    public Appendable append(CharSequence charSequence, int i, int i1) {
        throw new UnsupportedOperationException("TODO");
    }


    public OutputStream output() {
        return new OutputStream() {

            @Override
            public void write(int i) {
                append((char) i);
            }

            @Override
            public void flush() {
                term.flush();
            }
        };
    }

    @Override
    public void start(@Nullable Surface parent) {

        term.addVirtualTerminalListener(new VirtualTerminalListener() {


            @Override
            public void onFlush() {
                render();
            }

            @Override
            public void onBell() {

            }

            @Override
            public void onClose() {
                onDestroyed();
            }

            @Override
            public void onResized(Terminal terminal, TerminalSize terminalSize) {
                render();
            }
        });

        super.start(parent);

        term.addInput(KeyStroke.fromString("<pageup>")); //HACK trigger redraw


    }

    @Override
    public void stop() {
        super.stop();
        term.close();
        onDestroyed();
    }

    @Override
    public void paint(GL2 gl) {

        texture.paint(gl);
    }

    @Override
    public int[] getCursorPos() {
        TerminalPosition p = term.getCursorPosition();
        cursorPos[0] = p.getColumn();
        cursorPos[1] = p.getRow();
        return cursorPos;
    }

    public int cursorX() {
        return term.getCursorPosition().getColumn();
    }

    public int cursorY() {
        return term.getCursorPosition().getRow();
    }

    @Override
    public TextCharacter charAt(int col, int row) {
        return term.getCharacter(col, row);
    }


    @Override
    public boolean onKey(KeyEvent e, boolean pressed) {

        //return super.onKey(e, pressed);
        VirtualTerminal eterm = this.term;

        int cc = e.getKeyCode();
        if (pressed && cc == 13) {
            term.addInput(new KeyStroke(KeyType.Enter, e.isControlDown(), e.isAltDown()));
        } else if (pressed && cc == 8) {
            term.addInput(new KeyStroke(KeyType.Backspace, e.isControlDown(), e.isAltDown()));
        } else if (pressed && cc == 27) {
            term.addInput(new KeyStroke(KeyType.Escape, e.isControlDown(), e.isAltDown()));
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
                case KeyEvent.VK_BACK_SPACE:
                    c = KeyType.Backspace;
                    break;
                case KeyEvent.VK_ENTER:
                    c = KeyType.Enter;
                    break;
                case KeyEvent.VK_DELETE:
                    c = KeyType.Delete;
                    break;
                case KeyEvent.VK_LEFT:
                    c = KeyType.ArrowLeft;
                    break;
                case KeyEvent.VK_RIGHT:
                    c = KeyType.ArrowRight;
                    break;
                case KeyEvent.VK_UP:
                    c = KeyType.ArrowUp;
                    break;
                case KeyEvent.VK_DOWN:
                    c = KeyType.ArrowDown;
                    break;

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
        } else {
            //...
        }

        //AtomicBoolean busy = new AtomicBoolean(false);
        //if (busy.compareAndSet(false,true)) {

        //this.term.flush();

//        if (eterm instanceof TerminalUI) {
//            TerminalUI ee = (TerminalUI) eterm;
////            ee.gui.getGUIThread().invokeLater(() -> {
//                try {
//                    ee.gui.processInput();
//                    //ee.gui.updateScreen();
//                } catch (IOException e1) {
//                    e1.printStackTrace();
//                }
////            });
//        }
        return true;
    }

//    static class DirtyCellsLookupTable {
//        private final java.util.List<BitSet> table = new ArrayList();
//        private int firstRowIndex = -1;
//        private boolean allDirty = false;
//
//        DirtyCellsLookupTable() {
//        }
//
//        void resetAndInitialize(int firstRowIndex, int lastRowIndex, int columns) {
//            this.firstRowIndex = firstRowIndex;
//            this.allDirty = false;
//            int rows = lastRowIndex - firstRowIndex + 1;
//
//            while (this.table.size() < rows) {
//                this.table.add(new BitSet(columns));
//            }
//
//            while (this.table.size() > rows) {
//                this.table.remove(this.table.size() - 1);
//            }
//
//            for (int index = 0; index < this.table.size(); ++index) {
//                if (((BitSet) this.table.get(index)).size() != columns) {
//                    this.table.set(index, new BitSet(columns));
//                } else {
//                    ((BitSet) this.table.get(index)).clear();
//                }
//            }
//
//        }
//
//        void setAllDirty() {
//            this.allDirty = true;
//        }
//
//        boolean isAllDirty() {
//            return this.allDirty;
//        }
//
//        void setDirty(TerminalPosition position) {
//            if (position.getRow() >= this.firstRowIndex && position.getRow() < this.firstRowIndex + this.table.size()) {
//                BitSet tableRow = (BitSet) this.table.get(position.getRow() - this.firstRowIndex);
//                if (position.getColumn() < tableRow.size()) {
//                    tableRow.set(position.getColumn());
//                }
//
//            }
//        }
//
//        void setRowDirty(int rowNumber) {
//            BitSet row = (BitSet) this.table.get(rowNumber - this.firstRowIndex);
//            row.set(0, row.size());
//        }
//
//        void setColumnDirty(int column) {
//            Iterator var2 = this.table.iterator();
//
//            while (var2.hasNext()) {
//                BitSet row = (BitSet) var2.next();
//                if (column < row.size()) {
//                    row.set(column);
//                }
//            }
//
//        }
//
//        boolean isDirty(int row, int column) {
//            if (row >= this.firstRowIndex && row < this.firstRowIndex + this.table.size()) {
//                BitSet tableRow = (BitSet) this.table.get(row - this.firstRowIndex);
//                return column < tableRow.size() ? tableRow.get(column) : false;
//            } else {
//                return false;
//            }
//        }
//    }


    private static final Set<Character> TYPED_KEYS_TO_IGNORE = new HashSet(Arrays.asList('\n', '\t', '\r', '\b', '\u001b', '\u007f'));

    //abstract class GraphicalTerminalImplementation implements IOSafeTerminal {
//        private final TerminalEmulatorDeviceConfiguration deviceConfiguration;
//        private final TerminalEmulatorColorConfiguration colorConfiguration;
//    private final DirtyCellsLookupTable dirtyCellsLookupTable;
    private boolean cursorIsVisible;
    private boolean enableInput;


    private final boolean blinkOn;

    private boolean needFullRedraw;
    private TerminalPosition lastDrawnCursorPosition;
    private int lastBufferUpdateScrollPosition;
    private final int lastComponentWidth;
    private final int lastComponentHeight;
    private BufferedImage backbuffer;
    //private BufferedImage copybuffer;
    Color cursorColor = Color.ORANGE;


    private final int fontWidth;

    private final int fontHeight;

    private final Font font;
    boolean useAntiAliasing = false;

    {


//            term = new DefaultVirtualTerminal(initialTerminalSize);
//            this.deviceConfiguration = deviceConfiguration;
//            this.colorConfiguration = colorConfiguration;


        this.cursorIsVisible = true;
        this.enableInput = false;
        this.lastDrawnCursorPosition = null;
        this.lastBufferUpdateScrollPosition = 0;
        this.lastComponentHeight = 0;
        this.lastComponentWidth = 0;
        this.backbuffer = null;
        //this.copybuffer = null;
        this.blinkOn = true;
        this.needFullRedraw = false;

        font = new Font("Monospaced", 0, 24);
        this.fontWidth = getFontWidth(font);
        this.fontHeight = getFontHeight(font);

    }

    private FontRenderContext getFontRenderContext() {
        return new FontRenderContext((AffineTransform) null, useAntiAliasing ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF, RenderingHints.VALUE_FRACTIONALMETRICS_DEFAULT);
    }

    private int getFontWidth(Font font) {
        return (int) font.getStringBounds("W", this.getFontRenderContext()).getWidth();
    }

    private int getFontHeight(Font font) {
        return (int) font.getStringBounds("W", this.getFontRenderContext()).getHeight();
    }

    protected int getFontHeight() {
        return fontHeight;
    }

    protected int getFontWidth() {
        return fontWidth;
    }


    synchronized void onCreated() {
        this.enableInput = true;
    }

    synchronized void onDestroyed() {
        this.enableInput = false;
    }


    private int getWidth() {
        return fontWidth * cols;
    }

    private int getHeight() {
        return fontHeight * rows;
    }

//    synchronized void paintComponent(Graphics componentGraphics) {
//        int width = this.getWidth();
//        int height = this.getHeight();
////        this.scrollController.updateModel(term.getBufferLineCount() * this.getFontHeight(), height);
//        boolean needToUpdateBackBuffer = this.needFullRedraw;
//        int leftoverWidth;
//        if (width != this.lastComponentWidth || height != this.lastComponentHeight) {
//            int columns = width / this.getFontWidth();
//            leftoverWidth = height / this.getFontHeight();
//            TerminalSize terminalSize = term.getTerminalSize().withColumns(columns).withRows(leftoverWidth);
//            term.setTerminalSize(terminalSize);
//            needToUpdateBackBuffer = true;
//        }
//
////        if (needToUpdateBackBuffer) {
////            this.updateBackBuffer(this.scrollController.getScrollingOffset());
////        }
//
//        this.ensureGraphicBufferHasRightSize();
//        Rectangle clipBounds = componentGraphics.getClipBounds();
//        if (clipBounds == null) {
//            clipBounds = new Rectangle(0, 0, this.getWidth(), this.getHeight());
//        }
//
//        componentGraphics.drawImage(this.backbuffer, clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height, clipBounds.x, clipBounds.y, clipBounds.width, clipBounds.height, (ImageObserver) null);
//        leftoverWidth = this.getWidth() % this.getFontWidth();
//        componentGraphics.setColor(Color.BLACK);
//        if (leftoverWidth > 0) {
//            componentGraphics.fillRect(this.getWidth() - leftoverWidth, 0, leftoverWidth, this.getHeight());
//        }
//
//        this.lastComponentWidth = width;
//        this.lastComponentHeight = height;
//        componentGraphics.dispose();
//        this.notifyAll();
//    }

    private synchronized void updateBackBuffer(final int scrollOffsetFromTopInPixels) {
        final int fontWidth = this.getFontWidth();
        final int fontHeight = this.getFontHeight();
        final TerminalPosition cursorPosition = term.getCursorBufferPosition();
        final TerminalSize viewportSize = term.getTerminalSize();
        int firstVisibleRowIndex = scrollOffsetFromTopInPixels / fontHeight;
        int lastVisibleRowIndex = (scrollOffsetFromTopInPixels + this.getHeight()) / fontHeight;
        this.ensureGraphicBufferHasRightSize();
        final Graphics2D backbufferGraphics = this.backbuffer.createGraphics();

        backbufferGraphics.setFont(font);
        if (useAntiAliasing) { //if (this.isTextAntiAliased()) {
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            backbufferGraphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        }

//            final AtomicBoolean foundBlinkingCharacters = new AtomicBoolean(this.deviceConfiguration.isCursorBlinking());
//        this.buildDirtyCellsLookupTable(firstVisibleRowIndex, lastVisibleRowIndex);
        int previousLastVisibleRowIndex;
        Graphics2D graphics;
        int previousFirstVisibleRowIndex;
//        if (this.lastBufferUpdateScrollPosition < scrollOffsetFromTopInPixels) {
//            previousLastVisibleRowIndex = scrollOffsetFromTopInPixels - this.lastBufferUpdateScrollPosition;
////            if (previousLastVisibleRowIndex / fontHeight < viewportSize.getRows()) {
////                graphics = this.copybuffer.createGraphics();
////                graphics.setClip(0, 0, this.getWidth(), this.getHeight() - previousLastVisibleRowIndex);
////                graphics.drawImage(this.backbuffer, 0, -previousLastVisibleRowIndex, (ImageObserver) null);
////                graphics.dispose();
////                backbufferGraphics.drawImage(this.copybuffer, 0, 0, this.getWidth(), this.getHeight(), 0, 0, this.getWidth(), this.getHeight(), (ImageObserver) null);
////                if (!this.dirtyCellsLookupTable.isAllDirty()) {
////                    previousFirstVisibleRowIndex = (this.lastBufferUpdateScrollPosition + this.getHeight()) / fontHeight;
////
////                    for (int row = previousFirstVisibleRowIndex; row <= lastVisibleRowIndex; ++row) {
////                        this.dirtyCellsLookupTable.setRowDirty(row);
////                    }
////                }
////            } else {
//            this.dirtyCellsLookupTable.setAllDirty();
////            }
//        } else if (this.lastBufferUpdateScrollPosition > scrollOffsetFromTopInPixels) {
//            previousLastVisibleRowIndex = this.lastBufferUpdateScrollPosition - scrollOffsetFromTopInPixels;
////            if (previousLastVisibleRowIndex / fontHeight < viewportSize.getRows()) {
////                graphics = this.copybuffer.createGraphics();
////                graphics.setClip(0, 0, this.getWidth(), this.getHeight() - previousLastVisibleRowIndex);
////                graphics.drawImage(this.backbuffer, 0, 0, (ImageObserver) null);
////                graphics.dispose();
////                backbufferGraphics.drawImage(this.copybuffer, 0, previousLastVisibleRowIndex, this.getWidth(), this.getHeight(), 0, 0, this.getWidth(), this.getHeight() - previousLastVisibleRowIndex, (ImageObserver) null);
////                if (!this.dirtyCellsLookupTable.isAllDirty()) {
////                    previousFirstVisibleRowIndex = this.lastBufferUpdateScrollPosition / fontHeight;
////
////                    for (int row = firstVisibleRowIndex; row <= previousFirstVisibleRowIndex; ++row) {
////                        this.dirtyCellsLookupTable.setRowDirty(row);
////                    }
////                }
////            } else {
//            this.dirtyCellsLookupTable.setAllDirty();
////            }
//        }
//
//        if (this.lastComponentWidth < this.getWidth() && !this.dirtyCellsLookupTable.isAllDirty()) {
//            previousLastVisibleRowIndex = this.getWidth() / fontWidth;
//            int row = this.lastComponentWidth / fontWidth;
//
//            for (previousFirstVisibleRowIndex = row; previousFirstVisibleRowIndex <= previousLastVisibleRowIndex; ++previousFirstVisibleRowIndex) {
//                this.dirtyCellsLookupTable.setColumnDirty(previousFirstVisibleRowIndex);
//            }
//        }
//
//        if (this.lastComponentHeight < this.getHeight() && !this.dirtyCellsLookupTable.isAllDirty()) {
//            previousLastVisibleRowIndex = (scrollOffsetFromTopInPixels + this.lastComponentHeight) / fontHeight;
//
//            for (int row = previousLastVisibleRowIndex; row <= lastVisibleRowIndex; ++row) {
//                this.dirtyCellsLookupTable.setRowDirty(row);
//            }
//        }

        int cols = viewportSize.getColumns();
        int cursorCol = cursorPosition.getColumn();
        int cursorRow = cursorPosition.getRow();

        term.forEachLine(firstVisibleRowIndex, lastVisibleRowIndex, (rowNumber, bufferLine) -> {
            for (int column = 0; column < cols; ++column) {
                TextCharacter textCharacter = bufferLine.getCharacterAt(column);
                boolean atCursorLocation = cursorPosition.equals(column, rowNumber);
                if (!atCursorLocation && cursorCol == column + 1 && cursorRow == rowNumber && TerminalTextUtils.isCharCJK(textCharacter.getCharacter())) {
                    atCursorLocation = true;
                }


                //if (dirtyCellsLookupTable.isAllDirty() || dirtyCellsLookupTable.isDirty(rowNumber, column)) {
                int characterWidth = fontWidth * (TerminalTextUtils.isCharCJK(textCharacter.getCharacter()) ? 2 : 1);
                Color foregroundColor = textCharacter.getForegroundColor().toColor();
                Color backgroundColor = textCharacter.getBackgroundColor().toColor();

                drawCharacter(backbufferGraphics, textCharacter, column, rowNumber, foregroundColor, backgroundColor, fontWidth, fontHeight, characterWidth, scrollOffsetFromTopInPixels, atCursorLocation);
                //}

                if (TerminalTextUtils.isCharCJK(textCharacter.getCharacter())) {
                    ++column;
                }
            }

        });
        backbufferGraphics.dispose();

        this.lastDrawnCursorPosition = cursorPosition;
        this.lastBufferUpdateScrollPosition = scrollOffsetFromTopInPixels;
        this.needFullRedraw = false;
    }

//    private void buildDirtyCellsLookupTable(int firstRowOffset, int lastRowOffset) {
//        if (!(term instanceof DefaultVirtualTerminal && ((DefaultVirtualTerminal) term).isWholeBufferDirtyThenReset() && !this.needFullRedraw)) {
//            TerminalSize viewportSize = term.getTerminalSize();
//            TerminalPosition cursorPosition = term.getCursorBufferPosition();
//            this.dirtyCellsLookupTable.resetAndInitialize(firstRowOffset, lastRowOffset, viewportSize.getColumns());
//            this.dirtyCellsLookupTable.setDirty(cursorPosition);
//            if (this.lastDrawnCursorPosition != null && !this.lastDrawnCursorPosition.equals(cursorPosition)) {
//                if (term.getCharacter(this.lastDrawnCursorPosition).isDoubleWidth()) {
//                    this.dirtyCellsLookupTable.setDirty(this.lastDrawnCursorPosition.withRelativeColumn(1));
//                }
//
//                if (this.lastDrawnCursorPosition.getColumn() > 0 && term.getCharacter(this.lastDrawnCursorPosition.withRelativeColumn(-1)).isDoubleWidth()) {
//                    this.dirtyCellsLookupTable.setDirty(this.lastDrawnCursorPosition.withRelativeColumn(-1));
//                }
//
//                this.dirtyCellsLookupTable.setDirty(this.lastDrawnCursorPosition);
//            }
//
//            TreeSet<TerminalPosition> dirtyCells = ((DefaultVirtualTerminal) term).getAndResetDirtyCells();
//            Iterator var6 = dirtyCells.iterator();
//
//            while (var6.hasNext()) {
//                TerminalPosition position = (TerminalPosition) var6.next();
//                this.dirtyCellsLookupTable.setDirty(position);
//            }
//
//        } else {
//            this.dirtyCellsLookupTable.setAllDirty();
//        }
//    }

    private void ensureGraphicBufferHasRightSize() {

        if (this.backbuffer == null || this.backbuffer.getWidth() != this.getWidth() || this.backbuffer.getHeight() != this.getHeight()) {
            BufferedImage newBackbuffer = new BufferedImage(getWidth(), getHeight(), 1);
            Graphics2D graphics = newBackbuffer.createGraphics();
            graphics.fillRect(0, 0, newBackbuffer.getWidth(), newBackbuffer.getHeight());
            graphics.drawImage(this.backbuffer, 0, 0, (ImageObserver) null);
            graphics.dispose();
            this.backbuffer = newBackbuffer;
            //this.copybuffer = new BufferedImage(backbuffer.getWidth(), backbuffer.getHeight(), 1);
        }

    }

    private void drawCharacter(Graphics g, TextCharacter character, int columnIndex, int rowIndex, Color foregroundColor, Color backgroundColor, int fontWidth, int fontHeight, int characterWidth, int scrollingOffsetInPixels, boolean drawCursor) {
        int x = columnIndex * fontWidth;
        int y = rowIndex * fontHeight - scrollingOffsetInPixels;
        g.setColor(backgroundColor);
        //g.setClip(x, y, characterWidth, fontHeight);
        g.fillRect(x, y, characterWidth, fontHeight);
        g.setColor(foregroundColor);

        //FontMetrics fontMetrics = g.getFontMetrics();
        //g.drawString(Character.toString(character.getCharacter()), x, y + fontHeight - fontMetrics.getDescent() + 1);
        final int descent = 4;
        g.drawChars(new char[] { character.getCharacter() }, 0, 1, x, y + fontHeight + 1 - descent);
        int lineStartY;
        int lineEndX;
        if (character.isCrossedOut()) {
            lineStartY = y + fontHeight / 2;
            lineEndX = x + characterWidth;
            g.drawLine(x, lineStartY, lineEndX, lineStartY);
        }

        if (character.isUnderlined()) {
            lineStartY = y + fontHeight - descent + 1;
            lineEndX = x + characterWidth;
            g.drawLine(x, lineStartY, lineEndX, lineStartY);
        }

        if (drawCursor) {
            if (cursorColor == null) {
                g.setColor(foregroundColor);
            } else {
                g.setColor(cursorColor);
            }

//        if (this.deviceConfiguration.getCursorStyle() == CursorStyle.UNDER_BAR) {
//            g.fillRect(x, y + fontHeight - 3, characterWidth, 2);
//        } else if (this.deviceConfiguration.getCursorStyle() == CursorStyle.VERTICAL_BAR) {
            g.fillRect(x, y + 1, 2, fontHeight - 2);
//        }
        }

    }



//        public synchronized void enterPrivateMode() {
//            term.enterPrivateMode();
//            this.clearBackBuffer();
//            this.flush();
//        }
//
//        public synchronized void exitPrivateMode() {
//            term.exitPrivateMode();
//            this.clearBackBuffer();
//            this.flush();
//        }
//
//        public synchronized void clearScreen() {
//            term.clearScreen();
//            this.clearBackBuffer();
//        }

    private void clearBackBuffer() {
        if (this.backbuffer != null) {
            Graphics2D graphics = this.backbuffer.createGraphics();
            Color backgroundColor = Color.BLACK; //this.colorConfiguration.toAWTColor(ANSI.DEFAULT, false, false);
            graphics.setColor(backgroundColor);
            graphics.fillRect(0, 0, this.getWidth(), this.getHeight());
            graphics.dispose();
        }

    }

//    public synchronized void setCursorPosition(int x, int y) {
//        this.setCursorPosition(new TerminalPosition(x, y));
//    }

//        public synchronized void setCursorPosition(TerminalPosition position) {
//            if (position.getColumn() < 0) {
//                position = position.withColumn(0);
//            }
//
//            if (position.getRow() < 0) {
//                position = position.withRow(0);
//            }
//
//            term.setCursorPosition(position);
//        }
//
//        public TerminalPosition getCursorPosition() {
//            return term.getCursorPosition();
//        }

    public void setCursorVisible(boolean visible) {
        this.cursorIsVisible = visible;
    }

//        public synchronized void putCharacter(char c) {
//            term.putCharacter(c);
//        }
//
//        public TextGraphics newTextGraphics() {
//            return term.newTextGraphics();
//        }

//        public void enableSGR(SGR sgr) {
//            term.enableSGR(sgr);
//        }
//
//        public void disableSGR(SGR sgr) {
//            term.disableSGR(sgr);
//        }
//
//        public void resetColorAndSGR() {
//            term.resetColorAndSGR();
//        }
//
//        public void setForegroundColor(TextColor color) {
//            term.setForegroundColor(color);
//        }
//
//        public void setBackgroundColor(TextColor color) {
//            term.setBackgroundColor(color);
//        }
//
//        public synchronized TerminalSize getTerminalSize() {
//            return term.getTerminalSize();
//        }

//    public byte[] enquireTerminal(int timeout, TimeUnit timeoutUnit) {
//        return this.enquiryString.getBytes();
//    }

    //        public void bell() {
//            if (!this.bellOn) {
//                this.bellOn = true;
//                this.needFullRedraw = true;
//                this.updateBackBuffer(this.scrollController.getScrollingOffset());
//                this.repaint();
//                (new Thread("BellSilencer") {
//                    public void run() {
//                        try {
//                            Thread.sleep(100L);
//                        } catch (InterruptedException var2) {
//                            ;
//                        }
//
//                        bellOn = false;
//                        needFullRedraw = true;
//                        updateBackBuffer(scrollController.getScrollingOffset());
//                        repaint();
//                    }
//                }).start();
//                Toolkit.getDefaultToolkit().beep();
//            }
//        }
//
//    public synchronized void flush() {
//        this.updateBackBuffer(this.scrollController.getScrollingOffset());
//        this.repaint();
//    }

//    public void close() {
//    }

//        public void addResizeListener(TerminalResizeListener listener) {
//            term.addResizeListener(listener);
//        }
//
//        public void removeResizeListener(TerminalResizeListener listener) {
//            term.removeResizeListener(listener);
//        }
//
//        private void pasteClipboardContent() {
//            try {
//                Clipboard systemClipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//                if (systemClipboard != null) {
//                    this.injectStringAsKeyStrokes((String) systemClipboard.getData(DataFlavor.stringFlavor));
//                }
//            } catch (Exception var2) {
//                ;
//            }
//
//        }
//
//        private void pasteSelectionContent() {
//            try {
//                Clipboard systemSelection = Toolkit.getDefaultToolkit().getSystemSelection();
//                if (systemSelection != null) {
//                    this.injectStringAsKeyStrokes((String) systemSelection.getData(DataFlavor.stringFlavor));
//                }
//            } catch (Exception var2) {
//                ;
//            }
//
//        }

//        private void injectStringAsKeyStrokes(String string) {
//            StringReader stringReader = new StringReader(string);
//            InputDecoder inputDecoder = new InputDecoder(stringReader);
//            inputDecoder.addProfile(new DefaultKeyDecodingProfile());
//
//            try {
//                for (KeyStroke keyStroke = inputDecoder.getNextCharacter(false); keyStroke != null && keyStroke.getKeyType() != KeyType.EOF; keyStroke = inputDecoder.getNextCharacter(false)) {
//                    this.keyQueue.add(keyStroke);
//                }
//            } catch (IOException var5) {
//                ;
//            }
//
//        }


}
