/*
 * This file is part of lanterna (http://code.google.com/p/lanterna/).
 *
 * lanterna is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Copyright (C) 2010-2016 Martin
 */
package com.googlecode.lanterna.terminal.virtual;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.screen.TabBehaviour;
import com.googlecode.lanterna.terminal.AbstractTerminal;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class DefaultVirtualTerminal extends AbstractTerminal implements VirtualTerminal {
    private final TextBuffer regularTextBuffer;
    private final TextBuffer privateModeTextBuffer;
    private final TreeSet<TerminalPosition> dirtyTerminalCells;
    private final List<VirtualTerminalListener> listeners;

    private TextBuffer currentTextBuffer;
    private boolean wholeBufferDirty;

    private TerminalPosition termSize;
    private boolean cursorVisible;
    private int backlogSize;

    private final BlockingQueue<KeyStroke> inputQueue;
    private final EnumSet<SGR> activeModifiers;
    private TextColor activeForegroundColor;
    private TextColor activeBackgroundColor;

    // Global coordinates, i.e. relative to the top-left corner of the full buffer
    private TerminalPosition cursorPosition;

    // Used when switching back from private mode, to restore the earlier cursor position
    private TerminalPosition savedCursorPosition;


    /**
     * Creates a new virtual terminal with an initial size set
     */
    public DefaultVirtualTerminal() {
        this(80, 24);
    }
    public DefaultVirtualTerminal(int w, int  h) {
        this(new TerminalPosition(w, h));
    }

    /**
     * Creates a new virtual terminal with an initial size set
     * @param initialSize Starting size of the virtual terminal
     */
    public DefaultVirtualTerminal(TerminalPosition initialSize) {
        this.regularTextBuffer = new TextBuffer();
        this.privateModeTextBuffer = new TextBuffer();
        this.dirtyTerminalCells = new TreeSet<>();
        this.listeners = new ArrayList<>();

        // Terminal state
        this.inputQueue = new LinkedBlockingQueue<>();
        this.activeModifiers = EnumSet.noneOf(SGR.class);
        this.activeForegroundColor = TextColor.ANSI.DEFAULT;
        this.activeBackgroundColor = TextColor.ANSI.DEFAULT;

        // Start with regular mode
        this.currentTextBuffer = regularTextBuffer;
        this.wholeBufferDirty = false;
        this.termSize = initialSize;
        this.cursorVisible = true;
        this.cursorPosition = TerminalPosition.TOP_LEFT_CORNER;
        this.savedCursorPosition = TerminalPosition.TOP_LEFT_CORNER;
        this.backlogSize = 1000;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Terminal interface methods (and related)
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    @Override
    public TerminalPosition terminalSize() {
        return termSize;
    }

    @Override
    public synchronized void resize(TerminalPosition newSize) {
        this.termSize = newSize;
        trimBufferBacklog();
        correctCursor();
        for(VirtualTerminalListener listener: listeners) {
            listener.onResized(this, termSize);
        }
        super.onResized(newSize.column, newSize.row);
    }

    @Override
    public synchronized void enterPrivateMode() {
        currentTextBuffer = privateModeTextBuffer;
        savedCursorPosition = bufferPos();
        moveCursorTo(TerminalPosition.TOP_LEFT_CORNER);
        setWholeBufferDirty();
    }

    @Override
    public synchronized void exitPrivateMode() {
        currentTextBuffer = regularTextBuffer;
        cursorPosition = savedCursorPosition;
        setWholeBufferDirty();
    }

    @Override
    public synchronized void clearScreen() {
        currentTextBuffer.clear();
        setWholeBufferDirty();
        moveCursorTo(TerminalPosition.TOP_LEFT_CORNER);
    }

    @Override
    public synchronized void moveCursorTo(int x, int y) {
        moveCursorTo(cursorPosition.withColumn(x).withRow(y));
    }

    @Override
    public synchronized void moveCursorTo(TerminalPosition cursorPosition) {
        if(termSize.row < getBufferLineCount()) {
            cursorPosition = cursorPosition.withRelativeRow(getBufferLineCount() - termSize.row);
        }
        this.cursorPosition = cursorPosition;
        correctCursor();
    }

    @Override
    public synchronized TerminalPosition cursor() {
        if(getBufferLineCount() <= termSize.row) {
            return bufferPos();
        }
        else {
            return cursorPosition.withRelativeRow(-(getBufferLineCount() - termSize.row));
        }
    }

    @Override
    public synchronized TerminalPosition bufferPos() {
        return cursorPosition;
    }

    @Override
    public synchronized void setCursorVisible(boolean visible) {
        this.cursorVisible = visible;
    }

    @Override
    public synchronized void put(char c)  {
        if(c == '\n') {
            moveCursorToNextLine();
        }
        else if(TerminalTextUtils.isPrintableCharacter(c)) {
            putCharacter(new TextCharacter(c, activeForegroundColor, activeBackgroundColor, activeModifiers));
        }
    }

    @Override
    public synchronized void enableSGR(SGR sgr) {
        activeModifiers.add(sgr);
    }

    @Override
    public synchronized void disableSGR(SGR sgr) {
        activeModifiers.remove(sgr);
    }

    @Override
    public synchronized void resetColorAndSGR() {
        this.activeModifiers.clear();
        this.activeForegroundColor = TextColor.ANSI.DEFAULT;
        this.activeBackgroundColor = TextColor.ANSI.DEFAULT;
    }

    @Override
    public synchronized void fore(TextColor color) {
        this.activeForegroundColor = color;
    }

    @Override
    public synchronized void back(TextColor color) {
        this.activeBackgroundColor = color;
    }

    @Override
    public synchronized byte[] enquireTerminal(int timeout, TimeUnit timeoutUnit) {
        return getClass().getName().getBytes();
    }

    @Override
    public synchronized void bell() {
        for(VirtualTerminalListener listener: listeners) {
            listener.onBell();
        }
    }

    @Override
    public synchronized void flush() {
        for(VirtualTerminalListener listener: listeners) {
            listener.onFlush();
        }
    }

    @Override
    public synchronized KeyStroke pollInput() {
        return inputQueue.poll();
    }

    @Override
    public synchronized KeyStroke readInput() {
        try {
            return inputQueue.take();
        }
        catch(InterruptedException e) {
            throw new RuntimeException("Unexpected interrupt", e);
        }
    }

    @Override
    public TextGraphics newTextGraphics() throws IOException {
        return new VirtualTerminalTextGraphics(this);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // VirtualTerminal specific methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public synchronized void addVirtualTerminalListener(VirtualTerminalListener listener) {
        if(listener != null) {
            listeners.add(listener);
        }
    }

    @Override
    public synchronized void removeVirtualTerminalListener(VirtualTerminalListener listener) {
        listeners.remove(listener);
    }

    @Override
    public synchronized void setBacklogSize(int backlogSize) {
        this.backlogSize = backlogSize;
    }

    @Override
    public synchronized boolean isCursorVisible() {
        return cursorVisible;
    }

    @Override
    public void input(KeyStroke keyStroke) {
        inputQueue.add(keyStroke);
    }

    public synchronized DefaultVirtualTerminal input(String str) {
        for (int i = 0; i < str.length(); i++) {
            input(KeyStroke.fromString(String.valueOf(str.charAt(i))));
        }
        flush();
        return this;
    }

    public synchronized TreeSet<TerminalPosition> getDirtyCells() {
        return new TreeSet<>(dirtyTerminalCells);
    }

    public synchronized TreeSet<TerminalPosition> getAndResetDirtyCells() {
        TreeSet<TerminalPosition> copy = new TreeSet<>(dirtyTerminalCells);
        dirtyTerminalCells.clear();
        return copy;
    }

    public synchronized boolean isWholeBufferDirtyThenReset() {
        boolean copy = wholeBufferDirty;
        wholeBufferDirty = false;
        return copy;
    }

    @Override
    public synchronized TextCharacter getView(TerminalPosition position) {
        return getView(position.column, position.row);
    }

    @Override
    public synchronized TextCharacter getView(int column, int row) {
        if(termSize.row < currentTextBuffer.getLineCount()) {
            row += currentTextBuffer.getLineCount() - termSize.row;
        }
        return getBuffer(column, row);
    }

    @Override
    public TextCharacter getBuffer(int column, int row) {
        return currentTextBuffer.get(row, column);
    }

    @Override
    public TextCharacter getBuffer(TerminalPosition position) {
        return getBuffer(position.column, position.row);
    }

    @Override
    public synchronized int getBufferLineCount() {
        return currentTextBuffer.getLineCount();
    }

    @Override
    public synchronized void forEachLine(int startRow, int endRow, BufferWalker bufferWalker) {
        final BufferLine emptyLine = new BufferLine() {
            @Override
            public TextCharacter getCharacterAt(int column) {
                return TextCharacter.DEFAULT_CHARACTER;
            }
        };
        ListIterator<List<TextCharacter>> iterator = currentTextBuffer.getLinesFrom(startRow);
        for(int row = startRow; row <= endRow; row++) {
            BufferLine bufferLine = emptyLine;
            if(iterator.hasNext()) {
                final List<TextCharacter> list = iterator.next();
                bufferLine = new BufferLine() {
                    @Override
                    public TextCharacter getCharacterAt(int column) {
                        if(column >= list.size()) {
                            return TextCharacter.DEFAULT_CHARACTER;
                        }
                        return list.get(column);
                    }
                };
            }
            bufferWalker.onLine(row, bufferLine);
        }
    }

    synchronized void putCharacter(TextCharacter terminalCharacter) {
        if(terminalCharacter.c == '\t') {
            int nrOfSpaces = TabBehaviour.ALIGN_TO_COLUMN_4.getTabReplacement(cursorPosition.column).length();
            for(int i = 0; i < nrOfSpaces && cursorPosition.column < termSize.column - 1; i++) {
                putCharacter(terminalCharacter.withCharacter(' '));
            }
        }
        else {
            boolean doubleWidth = TerminalTextUtils.isCharDoubleWidth(terminalCharacter.c);
            // If we're at the last column and the user tries to print a double-width character, reset the cell and move
            // to the next line
            if(cursorPosition.column == termSize.column - 1 && doubleWidth) {
                currentTextBuffer.set(cursorPosition.row, cursorPosition.column, TextCharacter.DEFAULT_CHARACTER);
                moveCursorToNextLine();
            }
            if(cursorPosition.column == termSize.column) {
                moveCursorToNextLine();
            }

            // Update the buffer
            int i = currentTextBuffer.set(cursorPosition.row, cursorPosition.column, terminalCharacter);
            if(!wholeBufferDirty) {
                dirtyTerminalCells.add(new TerminalPosition(cursorPosition.column, cursorPosition.row));
                if(i == 1) {
                    dirtyTerminalCells.add(new TerminalPosition(cursorPosition.column + 1, cursorPosition.row));
                }
                else if(i == 2) {
                    dirtyTerminalCells.add(new TerminalPosition(cursorPosition.column - 1, cursorPosition.row));
                }
                if(dirtyTerminalCells.size() > (termSize.column * termSize.row * 0.9)) {
                    setWholeBufferDirty();
                }
            }

            //Advance cursor
            cursorPosition = cursorPosition.withRelativeColumn(doubleWidth ? 2 : 1);
            if(cursorPosition.column > termSize.column) {
                moveCursorToNextLine();
            }
        }
    }

    /**
     * Moves the text cursor to the first column of the next line and trims the backlog of necessary
     */
    private void moveCursorToNextLine() {
        cursorPosition = cursorPosition.withColumn(0).withRelativeRow(1);
        if(cursorPosition.row >= currentTextBuffer.getLineCount()) {
            currentTextBuffer.newLine();
        }
        trimBufferBacklog();
        correctCursor();
    }

    /**
     * Marks the whole buffer as dirty so every cell is considered in need to repainting. This is used by methods such
     * as clear and bell that will affect all content at once.
     */
    private void setWholeBufferDirty() {
        wholeBufferDirty = true;
        dirtyTerminalCells.clear();
    }

    private void trimBufferBacklog() {
        // Now see if we need to discard lines from the backlog
        int bufferBacklogSize = backlogSize;
        if(currentTextBuffer == privateModeTextBuffer) {
            bufferBacklogSize = 0;
        }
        int trimBacklogRows = currentTextBuffer.getLineCount() - (bufferBacklogSize + termSize.row);
        if(trimBacklogRows > 0) {
            currentTextBuffer.removeTopLines(trimBacklogRows);
            // Adjust cursor position
            cursorPosition = cursorPosition.withRelativeRow(-trimBacklogRows);
            correctCursor();
            if(!wholeBufferDirty) {
                // Adjust all "dirty" positions
                TreeSet<TerminalPosition> newDirtySet = new TreeSet<>();
                for(TerminalPosition dirtyPosition: dirtyTerminalCells) {
                    TerminalPosition adjustedPosition = dirtyPosition.withRelativeRow(-trimBacklogRows);
                    if(adjustedPosition.row >= 0) {
                        newDirtySet.add(adjustedPosition);
                    }
                }
                dirtyTerminalCells.clear();
                dirtyTerminalCells.addAll(newDirtySet);
            }
        }
    }

    private void correctCursor() {
        this.cursorPosition =
                new TerminalPosition(
                        Math.max(cursorPosition.column, 0),
                        Math.max(cursorPosition.row, 0));
        this.cursorPosition = cursorPosition.withColumn(Math.min(cursorPosition.column, termSize.column - 1));
        this.cursorPosition = cursorPosition.withRow(Math.min(cursorPosition.row, Math.max(termSize.row, getBufferLineCount()) - 1));
    }

}
