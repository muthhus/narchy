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
package com.googlecode.lanterna.screen;

import com.googlecode.lanterna.*;
import com.googlecode.lanterna.graphics.Scrollable;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;
import com.googlecode.lanterna.terminal.Terminal;
import com.googlecode.lanterna.terminal.TerminalResizeListener;

import java.io.IOException;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Map;
import java.util.TreeMap;

/**
 * This is the default concrete implementation of the Screen interface, a buffered layer sitting on top of a Terminal.
 * If you want to get started with the Screen layer, this is probably the class you want to use. Remember to start the
 * screen before you can use it and stop it when you are done with it. This will place the terminal in private mode
 * during the screen operations and leave private mode afterwards.
 * @author martin
 */
public class TerminalScreen extends AbstractScreen {
    public final Terminal terminal;
    private boolean isStarted;
    private boolean fullRedrawHint;
    private ScrollHint scrollHint;

    /**
     * Creates a new Screen on top of a supplied terminal, will query the terminal for its size. The screen is initially
     * blank. The default character used for unused space (the newly initialized state of the screen and new areas after
     * expanding the terminal size) will be a blank space in 'default' ANSI front- and background color.
     * <p>
     * Before you can display the content of this buffered screen to the real underlying terminal, you must call the
     * {@code startScreen()} method. This will ask the terminal to enter private mode (which is required for Screens to
     * work properly). Similarly, when you are done, you should call {@code stopScreen()} which will exit private mode.
     *
     * @param terminal Terminal object to create the DefaultScreen on top of
     * @throws IOException If there was an underlying I/O error when querying the size of the terminal
     */
    public TerminalScreen(Terminal terminal) throws IOException {
        this(terminal, DEFAULT_CHARACTER);
    }

    /**
     * Creates a new Screen on top of a supplied terminal, will query the terminal for its size. The screen is initially
     * blank. The default character used for unused space (the newly initialized state of the screen and new areas after
     * expanding the terminal size) will be a blank space in 'default' ANSI front- and background color.
     * <p>
     * Before you can display the content of this buffered screen to the real underlying terminal, you must call the
     * {@code startScreen()} method. This will ask the terminal to enter private mode (which is required for Screens to
     * work properly). Similarly, when you are done, you should call {@code stopScreen()} which will exit private mode.
     *
     * @param terminal Terminal object to create the DefaultScreen on top of.
     * @param defaultCharacter What character to use for the initial state of the screen and expanded areas
     * @throws IOException If there was an underlying I/O error when querying the size of the terminal
     */
    public TerminalScreen(Terminal terminal, TextCharacter defaultCharacter) throws IOException {
        super(terminal.terminalSize(), defaultCharacter);
        this.terminal = terminal;
        this.terminal.addResizeListener(new TerminalScreenResizeListener());
        this.isStarted = false;
        this.fullRedrawHint = true;
    }

    @Override
    public synchronized void startScreen() throws IOException {
        if(isStarted) {
            return;
        }

        isStarted = true;
        terminal.enterPrivateMode();
        terminal.terminalSize();
        terminal.clearScreen();
        this.fullRedrawHint = true;
        TerminalPosition cursorPosition = cursorPosition();
        if(cursorPosition != null) {
            terminal.setCursorVisible(true);
            terminal.moveCursorTo(cursorPosition.column, cursorPosition.row);
        } else {
            terminal.setCursorVisible(false);
        }
    }

    @Override
    public void stopScreen() throws IOException {
        stopScreen(true);
    }
    
    public synchronized void stopScreen(boolean flushInput) throws IOException {
        if(!isStarted) {
            return;
        }

        if (flushInput) {
            //Drain the input queue
            KeyStroke keyStroke;
            do {
                keyStroke = pollInput();
            }
            while(keyStroke != null && keyStroke.getKeyType() != KeyType.EOF);
        }

        terminal.exitPrivateMode();
        isStarted = false;
    }

    @Override
    public synchronized void refresh(RefreshType refreshType) throws IOException {
        if(!isStarted) {
            return;
        }
        if((refreshType == RefreshType.AUTOMATIC && fullRedrawHint) || refreshType == RefreshType.COMPLETE) {
            refreshFull();
            fullRedrawHint = false;
        }
        else if(refreshType == RefreshType.AUTOMATIC &&
                (scrollHint == null || scrollHint == ScrollHint.INVALID)) {
            double threshold = terminalSize().row * terminalSize().column * 0.75;
            if(getBackBuffer().isVeryDifferent(getFrontBuffer(), (int) threshold)) {
                refreshFull();
            }
            else {
                refreshByDelta();
            }
        }
        else {
            refreshByDelta();
        }
        getBackBuffer().copyTo(getFrontBuffer());
        TerminalPosition cursorPosition = cursorPosition();
        if(cursorPosition != null) {
            terminal.setCursorVisible(true);
            //If we are trying to move the cursor to the padding of a CJK character, put it on the actual character instead
            if(cursorPosition.column > 0 && TerminalTextUtils.isCharCJK(getFrontBuffer().get(cursorPosition.withRelativeColumn(-1)).c)) {
                terminal.moveCursorTo(cursorPosition.column - 1, cursorPosition.row);
            }
            else {
                terminal.moveCursorTo(cursorPosition.column, cursorPosition.row);
            }
        } else {
            terminal.setCursorVisible(false);
        }
        terminal.flush();
    }

    private void useScrollHint() throws IOException {
        if (scrollHint == null) { return; }

        try {
            if (scrollHint == ScrollHint.INVALID) { return; }
            Terminal term = terminal;
            if (term instanceof Scrollable) {
                // just try and see if it cares:
                scrollHint.applyTo( (Scrollable)term );
                // if that didn't throw, then update front buffer:
                scrollHint.applyTo( getFrontBuffer() );
            }
        }
        catch (UnsupportedOperationException uoe) { /* ignore */ }
        finally { scrollHint = null; }
    }

    private void refreshByDelta() throws IOException {
        Map<TerminalPosition, TextCharacter> updateMap = new TreeMap<>(new ScreenPointComparator());
        TerminalPosition terminalPosition = terminalSize();

        useScrollHint();

        for(int y = 0; y < terminalPosition.row; y++) {
            for(int x = 0; x < terminalPosition.column; x++) {
                TextCharacter backBufferCharacter = getBackBuffer().get(x, y);
                if(!backBufferCharacter.equals(getFrontBuffer().get(x, y))) {
                    updateMap.put(new TerminalPosition(x, y), backBufferCharacter);
                }
                if(TerminalTextUtils.isCharCJK(backBufferCharacter.c)) {
                    x++;    //Skip the trailing padding
                }
            }
        }

        if(updateMap.isEmpty()) {
            return;
        }
        TerminalPosition currentPosition = updateMap.keySet().iterator().next();
        terminal.moveCursorTo(currentPosition.column, currentPosition.row);

        TextCharacter firstScreenCharacterToUpdate = updateMap.values().iterator().next();
        EnumSet<SGR> currentSGR = firstScreenCharacterToUpdate.getModifiers();
        terminal.resetColorAndSGR();
        for(SGR sgr: currentSGR) {
            terminal.enableSGR(sgr);
        }
        TextColor currentForegroundColor = firstScreenCharacterToUpdate.fore;
        TextColor currentBackgroundColor = firstScreenCharacterToUpdate.back;
        terminal.fore(currentForegroundColor);
        terminal.back(currentBackgroundColor);
        for(TerminalPosition position: updateMap.keySet()) {
            if(!position.equals(currentPosition)) {
                terminal.moveCursorTo(position.column, position.row);
                currentPosition = position;
            }
            TextCharacter newCharacter = updateMap.get(position);
            if(!currentForegroundColor.equals(newCharacter.fore)) {
                terminal.fore(newCharacter.fore);
                currentForegroundColor = newCharacter.fore;
            }
            if(!currentBackgroundColor.equals(newCharacter.back)) {
                terminal.back(newCharacter.back);
                currentBackgroundColor = newCharacter.back;
            }
            for(SGR sgr: SGR.values()) {
                if(currentSGR.contains(sgr) && !newCharacter.getModifiers().contains(sgr)) {
                    terminal.disableSGR(sgr);
                    currentSGR.remove(sgr);
                }
                else if(!currentSGR.contains(sgr) && newCharacter.getModifiers().contains(sgr)) {
                    terminal.enableSGR(sgr);
                    currentSGR.add(sgr);
                }
            }
            terminal.put(newCharacter.c);
            if(TerminalTextUtils.isCharCJK(newCharacter.c)) {
                //CJK characters advances two columns
                currentPosition = currentPosition.withRelativeColumn(2);
            }
            else {
                //Normal characters advances one column
                currentPosition = currentPosition.withRelativeColumn(1);
            }
        }
    }

    private void refreshFull() throws IOException {
        terminal.fore(TextColor.ANSI.DEFAULT);
        terminal.back(TextColor.ANSI.DEFAULT);
        terminal.clearScreen();
        terminal.resetColorAndSGR();
        scrollHint = null; // discard any scroll hint for full refresh

        EnumSet<SGR> currentSGR = EnumSet.noneOf(SGR.class);
        TextColor currentForegroundColor = TextColor.ANSI.DEFAULT;
        TextColor currentBackgroundColor = TextColor.ANSI.DEFAULT;
        for(int y = 0; y < terminalSize().row; y++) {
            terminal.moveCursorTo(0, y);
            int currentColumn = 0;
            for(int x = 0; x < terminalSize().column; x++) {
                TextCharacter newCharacter = getBackBuffer().get(x, y);
                if(newCharacter.equals(DEFAULT_CHARACTER)) {
                    continue;
                }

                if(!currentForegroundColor.equals(newCharacter.fore)) {
                    terminal.fore(newCharacter.fore);
                    currentForegroundColor = newCharacter.fore;
                }
                if(!currentBackgroundColor.equals(newCharacter.back)) {
                    terminal.back(newCharacter.back);
                    currentBackgroundColor = newCharacter.back;
                }
                for(SGR sgr: SGR.values()) {
                    if(currentSGR.contains(sgr) && !newCharacter.getModifiers().contains(sgr)) {
                        terminal.disableSGR(sgr);
                        currentSGR.remove(sgr);
                    }
                    else if(!currentSGR.contains(sgr) && newCharacter.getModifiers().contains(sgr)) {
                        terminal.enableSGR(sgr);
                        currentSGR.add(sgr);
                    }
                }
                if(currentColumn != x) {
                    terminal.moveCursorTo(x, y);
                    currentColumn = x;
                }
                terminal.put(newCharacter.c);
                if(TerminalTextUtils.isCharCJK(newCharacter.c)) {
                    //CJK characters take up two columns
                    currentColumn += 2;
                    x++;
                }
                else {
                    //Normal characters take up one column
                    currentColumn += 1;
                }
            }
        }
    }

    @Override
    public KeyStroke readInput() throws IOException {
        return terminal.readInput();
    }

    @Override
    public KeyStroke pollInput() throws IOException {
        return terminal.pollInput();
    }

    @Override
    public synchronized void clear() {
        super.clear();
        fullRedrawHint = true;
        scrollHint = ScrollHint.INVALID;
    }

    @Override
    public synchronized TerminalPosition doResizeIfNecessary() {
        TerminalPosition newSize = super.doResizeIfNecessary();
        if(newSize != null) {
            fullRedrawHint = true;
        }
        return newSize;
    }
    
    /**
     * Perform the scrolling and save scroll-range and distance in order
     * to be able to optimize Terminal-update later.
     */
    @Override
    public void scrollLines(int firstLine, int lastLine, int distance) {
        // just ignore certain kinds of garbage:
        if (distance == 0 || firstLine > lastLine) { return; }

        super.scrollLines(firstLine, lastLine, distance);

        // Save scroll hint for next refresh:
        ScrollHint newHint = new ScrollHint(firstLine,lastLine,distance);
        if (scrollHint == null) {
            // no scroll hint yet: use the new one:
            scrollHint = newHint;
        } else //noinspection StatementWithEmptyBody
            if (scrollHint == ScrollHint.INVALID) {
            // scroll ranges already inconsistent since latest refresh!
            // leave at INVALID
        } else if (scrollHint.matches(newHint)) {
            // same range: just accumulate distance:
            scrollHint.distance += newHint.distance;
        } else {
            // different scroll range: no scroll-optimization for next refresh
            this.scrollHint = ScrollHint.INVALID;
        }
    }

    private class TerminalScreenResizeListener implements TerminalResizeListener {
        @Override
        public void onResized(Terminal terminal, TerminalPosition newSize) {
            addResizeRequest(newSize);
        }
    }

    private static class ScreenPointComparator implements Comparator<TerminalPosition> {
        @Override
        public int compare(TerminalPosition o1, TerminalPosition o2) {
            if(o1.row == o2.row) {
                if(o1.column == o2.column) {
                    return 0;
                } else {
                    return new Integer(o1.column).compareTo(o2.column);
                }
            } else {
                return Integer.compare(o1.row, o2.row);
            }
        }
    }

    private static class ScrollHint {
        public static final ScrollHint INVALID = new ScrollHint(-1,-1,0);
        public final int firstLine;
        public final int lastLine;
        public int distance;

        public ScrollHint(int firstLine, int lastLine, int distance) {
            this.firstLine = firstLine;
            this.lastLine = lastLine;
            this.distance = distance;
        }

        public boolean matches(ScrollHint other) {
            return this.firstLine == other.firstLine
                && this.lastLine == other.lastLine;
        }

        public void applyTo( Scrollable scr ) throws IOException {
            scr.scrollLines(firstLine, lastLine, distance);
        }
    }

}
