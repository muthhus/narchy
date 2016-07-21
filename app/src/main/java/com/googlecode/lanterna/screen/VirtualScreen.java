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

import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.graphics.TextGraphics;
import com.googlecode.lanterna.input.KeyStroke;
import com.googlecode.lanterna.input.KeyType;

import java.io.IOException;

/**
 * VirtualScreen wraps a normal screen and presents it as a screen that has a configurable minimum size; if the real
 * screen is smaller than this size, the presented screen will add scrolling to get around it. To anyone using this
 * class, it will appear and behave just as a normal screen. Scrolling is done by using CTRL + arrow keys.
 * <p>
 * The use case for this class is to allow you to set a minimum size that you can count on be honored, no matter how
 * small the user makes the terminal. This should make programming GUIs easier.
 * @author Martin
 */
public class VirtualScreen extends AbstractScreen {
    private final Screen realScreen;
    private final FrameRenderer frameRenderer;
    private TerminalPosition minimumSize;
    private TerminalPosition viewportTopLeft;
    private TerminalPosition viewportSize;

    /**
     * Creates a new VirtualScreen that wraps a supplied Screen. The screen passed in here should be the real screen
     * that is created on top of the real {@code Terminal}, it will have the correct size and content for what's
     * actually displayed to the user, but this class will present everything as one view with a fixed minimum size,
     * no matter what size the real terminal has.
     * <p>
     * The initial minimum size will be the current size of the screen.
     * @param screen Real screen that will be used when drawing the whole or partial virtual screen
     */
    public VirtualScreen(Screen screen) {
        super(screen.terminalSize());
        this.frameRenderer = new DefaultFrameRenderer();
        this.realScreen = screen;
        this.minimumSize = screen.terminalSize();
        this.viewportTopLeft = TerminalPosition.TOP_LEFT_CORNER;
        this.viewportSize = minimumSize;
    }

    /**
     * Sets the minimum size we want the virtual screen to have. If the user resizes the real terminal to something
     * smaller than this, the virtual screen will refuse to make it smaller and add scrollbars to the view.
     * @param minimumSize Minimum size we want the screen to have
     */
    public void setMinimumSize(TerminalPosition minimumSize) {
        this.minimumSize = minimumSize;
        TerminalPosition virtualSize = minimumSize.max(realScreen.terminalSize());
        if(!minimumSize.equals(virtualSize)) {
            addResizeRequest(virtualSize);
            super.doResizeIfNecessary();
        }
        calculateViewport(realScreen.terminalSize());
    }

    /**
     * Returns the minimum size this virtual screen can have. If the real terminal is made smaller than this, the
     * virtual screen will draw scrollbars and implement scrolling
     * @return Minimum size configured for this virtual screen
     */
    public TerminalPosition getMinimumSize() {
        return minimumSize;
    }

    @Override
    public void startScreen() throws IOException {
        realScreen.startScreen();
    }

    @Override
    public void stopScreen() throws IOException {
        realScreen.stopScreen();
    }

    @Override
    public TextCharacter front(TerminalPosition position) {
        return null;
    }

    @Override
    public void moveCursorTo(TerminalPosition position) {
        super.moveCursorTo(position);
        if(position == null) {
            realScreen.moveCursorTo(null);
            return;
        }
        position = position.withRelativeColumn(-viewportTopLeft.col).withRelativeRow(-viewportTopLeft.row);
        if(position.col >= 0 && position.col < viewportSize.col &&
                position.row >= 0 && position.row < viewportSize.row) {
            realScreen.moveCursorTo(position);
        }
        else {
            realScreen.moveCursorTo(null);
        }
    }

    @Override
    public synchronized TerminalPosition doResizeIfNecessary() {
        TerminalPosition underlyingSize = realScreen.doResizeIfNecessary();
        if(underlyingSize == null) {
            return null;
        }

        TerminalPosition newVirtualSize = calculateViewport(underlyingSize);
        if(!terminalSize().equals(newVirtualSize)) {
            addResizeRequest(newVirtualSize);
            return super.doResizeIfNecessary();
        }
        return newVirtualSize;
    }

    private TerminalPosition calculateViewport(TerminalPosition realTerminalPosition) {
        TerminalPosition newVirtualSize = minimumSize.max(realTerminalPosition);
        if(newVirtualSize.equals(realTerminalPosition)) {
            viewportSize = realTerminalPosition;
            viewportTopLeft = TerminalPosition.TOP_LEFT_CORNER;
        }
        else {
            TerminalPosition newViewportSize = frameRenderer.getViewportSize(realTerminalPosition, newVirtualSize);
            if(newViewportSize.row > viewportSize.row) {
                viewportTopLeft = viewportTopLeft.withRow(Math.max(0, viewportTopLeft.row - (newViewportSize.row - viewportSize.row)));
            }
            if(newViewportSize.col > viewportSize.col) {
                viewportTopLeft = viewportTopLeft.withColumn(Math.max(0, viewportTopLeft.col - (newViewportSize.col - viewportSize.col)));
            }
            viewportSize = newViewportSize;
        }
        return newVirtualSize;
    }

    @Override
    public void refresh(RefreshType refreshType) throws IOException {
        moveCursorTo(cursorPosition()); //Make sure the cursor is at the correct position
        if(!viewportSize.equals(realScreen.terminalSize())) {
            frameRenderer.drawFrame(
                    realScreen.newTextGraphics(),
                    realScreen.terminalSize(),
                    terminalSize(),
                    viewportTopLeft);
        }

        //Copy the rows
        TerminalPosition viewportOffset = frameRenderer.getViewportOffset();
        if(realScreen instanceof AbstractScreen) {
            AbstractScreen asAbstractScreen = (AbstractScreen)realScreen;
            getBackBuffer().copyTo(
                    asAbstractScreen.getBackBuffer(),
                    viewportTopLeft.row,
                    viewportSize.row,
                    viewportTopLeft.col,
                    viewportSize.col,
                    viewportOffset.row,
                    viewportOffset.col);
        }
        else {
            for(int y = 0; y < viewportSize.row; y++) {
                for(int x = 0; x < viewportSize.col; x++) {
                    realScreen.set(
                            x + viewportOffset.col,
                            y + viewportOffset.row,
                            getBackBuffer().get(
                                    x + viewportTopLeft.col,
                                    y + viewportTopLeft.row));
                }
            }
        }
        realScreen.refresh(refreshType);
    }

    @Override
    public KeyStroke pollInput() throws IOException {
        return filter(realScreen.pollInput());
    }

    @Override
    public KeyStroke readInput() throws IOException {
        return filter(realScreen.readInput());
    }

    private KeyStroke filter(KeyStroke keyStroke) throws IOException {
        if(keyStroke == null) {
            return null;
        }
        else if(keyStroke.isAltDown() && keyStroke.getKeyType() == KeyType.ArrowLeft) {
            if(viewportTopLeft.col > 0) {
                viewportTopLeft = viewportTopLeft.withRelativeColumn(-1);
                refresh();
                return null;
            }
        }
        else if(keyStroke.isAltDown() && keyStroke.getKeyType() == KeyType.ArrowRight) {
            if(viewportTopLeft.col + viewportSize.col < terminalSize().col) {
                viewportTopLeft = viewportTopLeft.withRelativeColumn(1);
                refresh();
                return null;
            }
        }
        else if(keyStroke.isAltDown() && keyStroke.getKeyType() == KeyType.ArrowUp) {
            if(viewportTopLeft.row > 0) {
                viewportTopLeft = viewportTopLeft.withRelativeRow(-1);
                realScreen.scrollLines(0, viewportSize.row -1,-1);
                refresh();
                return null;
            }
        }
        else if(keyStroke.isAltDown() && keyStroke.getKeyType() == KeyType.ArrowDown) {
            if(viewportTopLeft.row + viewportSize.row < terminalSize().row) {
                viewportTopLeft = viewportTopLeft.withRelativeRow(1);
                realScreen.scrollLines(0, viewportSize.row -1,1);
                refresh();
                return null;
            }
        }
        return keyStroke;
    }

    @Override
    public void scrollLines(int firstLine, int lastLine, int distance) {
        // do base class stuff (scroll own back buffer)
        super.scrollLines(firstLine, lastLine, distance);
        // vertical range visible in realScreen:
        int vpFirst = viewportTopLeft.row,
            vpRows = viewportSize.row;
        // adapt to realScreen range:
        firstLine = Math.max(0, firstLine - vpFirst);
        lastLine = Math.min(vpRows - 1, lastLine - vpFirst);
        // if resulting range non-empty: scroll that range in realScreen:
        if (firstLine <= lastLine) {
            realScreen.scrollLines(firstLine, lastLine, distance);
        }
    }

    /**
     * Interface for rendering the virtual screen's frame when the real terminal is too small for the virtual screen
     */
    public interface FrameRenderer {
        /**
         * Given the size of the real terminal and the current size of the virtual screen, how large should the viewport
         * where the screen content is drawn be?
         * @param realSize Size of the real terminal
         * @param virtualSize Size of the virtual screen
         * @return Size of the viewport, according to this FrameRenderer
         */
        TerminalPosition getViewportSize(TerminalPosition realSize, TerminalPosition virtualSize);

        /**
         * Where in the virtual screen should the top-left position of the viewport be? To draw the viewport from the
         * top-left position of the screen, return 0x0 (or TerminalPosition.TOP_LEFT_CORNER) here.
         * @return Position of the top-left corner of the viewport inside the screen
         */
        TerminalPosition getViewportOffset();

        /**
         * Drawn the 'frame', meaning anything that is outside the viewport (title, scrollbar, etc)
         * @param graphics Graphics to use to text drawing operations
         * @param realSize Size of the real terminal
         * @param virtualSize Size of the virtual screen
         * @param virtualScrollPosition If the virtual screen is larger than the real terminal, this is the current
         *                              scroll offset the VirtualScreen is using
         */
        void drawFrame(
                TextGraphics graphics,
                TerminalPosition realSize,
                TerminalPosition virtualSize,
                TerminalPosition virtualScrollPosition);
    }

    private static class DefaultFrameRenderer implements FrameRenderer {
        @Override
        public TerminalPosition getViewportSize(TerminalPosition realSize, TerminalPosition virtualSize) {
            if(realSize.col > 1 && realSize.row > 2) {
                return realSize.withRelativeColumn(-1).withRelativeRow(-2);
            }
            else {
                return realSize;
            }
        }

        @Override
        public TerminalPosition getViewportOffset() {
            return TerminalPosition.TOP_LEFT_CORNER;
        }

        @Override
        public void drawFrame(
                TextGraphics graphics,
                TerminalPosition realSize,
                TerminalPosition virtualSize,
                TerminalPosition virtualScrollPosition) {

            if(realSize.col == 1 || realSize.row <= 2) {
                return;
            }
            TerminalPosition viewportSize = getViewportSize(realSize, virtualSize);

            graphics.setForegroundColor(TextColor.ANSI.WHITE);
            graphics.setBackgroundColor(TextColor.ANSI.BLACK);
            graphics.fill(' ');
            graphics.putString(0, graphics.getSize().row - 1, "Terminal too small, use ALT+arrows to scroll");

            int horizontalSize = (int)(((double)(viewportSize.col) / (double) virtualSize.col) * (viewportSize.col));
            int scrollable = viewportSize.col - horizontalSize - 1;
            int horizontalPosition = (int)((double)scrollable * ((double) virtualScrollPosition.col / (double)(virtualSize.col - viewportSize.col)));
            graphics.drawLine(
                    new TerminalPosition(horizontalPosition, graphics.getSize().row - 2),
                    new TerminalPosition(horizontalPosition + horizontalSize, graphics.getSize().row - 2),
                    Symbols.BLOCK_MIDDLE);

            int verticalSize = (int)(((double)(viewportSize.row) / (double) virtualSize.row) * (viewportSize.row));
            scrollable = viewportSize.row - verticalSize - 1;
            int verticalPosition = (int)((double)scrollable * ((double) virtualScrollPosition.row / (double)(virtualSize.row - viewportSize.row)));
            graphics.drawLine(
                    new TerminalPosition(graphics.getSize().col - 1, verticalPosition),
                    new TerminalPosition(graphics.getSize().col - 1, verticalPosition + verticalSize),
                    Symbols.BLOCK_MIDDLE);
        }
    }
}
