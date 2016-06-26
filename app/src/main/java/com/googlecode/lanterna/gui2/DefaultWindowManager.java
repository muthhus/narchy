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
package com.googlecode.lanterna.gui2;

import com.googlecode.lanterna.TerminalPosition;

import java.util.List;

/**
 * The default window manager implementation used by Lanterna. New windows will be generally added in a tiled manner,
 * starting in the top-left corner and moving down-right as new windows are added. By using the various window hints
 * that are available you have some control over how the window manager will place and size the windows.
 *
 * @author Martin
 */
public class DefaultWindowManager implements WindowManager {

    private final WindowDecorationRenderer windowDecorationRenderer;
    private TerminalPosition lastKnownScreenSize;

    /**
     * Default constructor, will create a window manager that uses {@code DefaultWindowDecorationRenderer} for drawing
     * window decorations. Any size calculations done before the text GUI has actually been started and displayed on
     * the terminal will assume the terminal size is 80x24.
     */
    public DefaultWindowManager() {
        this(new DefaultWindowDecorationRenderer());
    }

    /**
     * Creates a new {@code DefaultWindowManager} with a specific window decoration renderer. Any size calculations done
     * before the text GUI has actually been started and displayed on the terminal will assume the terminal size is
     * 80x24.
     *
     * @param windowDecorationRenderer Window decoration renderer to use when drawing windows
     */
    public DefaultWindowManager(WindowDecorationRenderer windowDecorationRenderer) {
        this(windowDecorationRenderer, null);
    }

    /**
     * Creates a new {@code DefaultWindowManager} using a {@code DefaultWindowDecorationRenderer} for drawing window
     * decorations. Any size calculations done before the text GUI has actually been started and displayed on the
     * terminal will use the size passed in with the {@code initialScreenSize} parameter
     *
     * @param initialScreenSize Size to assume the terminal has until the text GUI is started and can be notified of the
     *                          correct size
     */
    public DefaultWindowManager(TerminalPosition initialScreenSize) {
        this(new DefaultWindowDecorationRenderer(), initialScreenSize);
    }

    /**
     * Creates a new {@code DefaultWindowManager} using a specified {@code windowDecorationRenderer} for drawing window
     * decorations. Any size calculations done before the text GUI has actually been started and displayed on the
     * terminal will use the size passed in with the {@code initialScreenSize} parameter
     *
     * @param windowDecorationRenderer Window decoration renderer to use when drawing windows
     * @param initialScreenSize Size to assume the terminal has until the text GUI is started and can be notified of the
     *                          correct size
     */
    public DefaultWindowManager(WindowDecorationRenderer windowDecorationRenderer, TerminalPosition initialScreenSize) {
        this.windowDecorationRenderer = windowDecorationRenderer;
        if(initialScreenSize != null) {
            this.lastKnownScreenSize = initialScreenSize;
        }
        else {
            this.lastKnownScreenSize = new TerminalPosition(80, 24);
        }
    }

    @Override
    public boolean isInvalid() {
        return false;
    }

    @Override
    public WindowDecorationRenderer getWindowDecorationRenderer(Window window) {
        if(window.getHints().contains(Window.Hint.NO_DECORATIONS)) {
            return new EmptyWindowDecorationRenderer();
        }
        return windowDecorationRenderer;
    }

    @Override
    public void onAdded(WindowBasedTextGUI textGUI, Window window, List<Window> allWindows) {
        WindowDecorationRenderer decorationRenderer = getWindowDecorationRenderer(window);
        TerminalPosition expectedDecoratedSize = decorationRenderer.getDecoratedSize(window, window.getPreferredSize());
        window.setDecoratedSize(expectedDecoratedSize);

        //noinspection StatementWithEmptyBody
        if(window.getHints().contains(Window.Hint.FIXED_POSITION)) {
            //Don't place the window, assume the position is already set
        }
        else if(allWindows.isEmpty()) {
            window.setPosition(TerminalPosition.OFFSET_1x1);
        }
        else if(window.getHints().contains(Window.Hint.CENTERED)) {
            int left = (lastKnownScreenSize.column - expectedDecoratedSize.column) / 2;
            int top = (lastKnownScreenSize.row - expectedDecoratedSize.row) / 2;
            window.setPosition(new TerminalPosition(left, top));
        }
        else {
            TerminalPosition nextPosition = allWindows.get(allWindows.size() - 1).getPosition().withRelative(2, 1);
            if(nextPosition.column + expectedDecoratedSize.column > lastKnownScreenSize.column ||
                    nextPosition.row + expectedDecoratedSize.row > lastKnownScreenSize.row) {
                nextPosition = TerminalPosition.OFFSET_1x1;
            }
            window.setPosition(nextPosition);
        }

        // Finally, run through the usual calculations so the window manager's usual prepare method can have it's say
        prepareWindow(lastKnownScreenSize, window);
    }

    @Override
    public void onRemoved(WindowBasedTextGUI textGUI, Window window, List<Window> allWindows) {
        //NOP
    }

    @Override
    public void prepareWindows(WindowBasedTextGUI textGUI, List<Window> allWindows, TerminalPosition screenSize) {
        this.lastKnownScreenSize = screenSize;
        for(Window window: allWindows) {
            prepareWindow(screenSize, window);
        }
    }

    /**
     * Called by {@link DefaultWindowManager} when iterating through all windows to decide their size and position. If
     * you override {@link DefaultWindowManager} to add your own logic to how windows are placed on the screen, you can
     * override this method and selectively choose which window to interfere with. Note that the two key properties that
     * are read by the GUI system after preparing all windows are the position and decorated size. Your custom
     * implementation should set these two fields directly on the window. You can infer the decorated size from the
     * content size by using the window decoration renderer that is attached to the window manager.
     *
     * @param screenSize Size of the terminal that is available to draw on
     * @param window Window to prepare decorated size and position for
     */
    protected void prepareWindow(TerminalPosition screenSize, Window window) {
        WindowDecorationRenderer decorationRenderer = getWindowDecorationRenderer(window);
        TerminalPosition contentAreaSize;
        if(window.getHints().contains(Window.Hint.FIXED_SIZE)) {
            contentAreaSize = window.getSize();
        }
        else {
            contentAreaSize = window.getPreferredSize();
        }
        TerminalPosition size = decorationRenderer.getDecoratedSize(window, contentAreaSize);
        TerminalPosition position = window.getPosition();

        if(window.getHints().contains(Window.Hint.FULL_SCREEN)) {
            position = TerminalPosition.TOP_LEFT_CORNER;
            size = screenSize;
        }
        else if(window.getHints().contains(Window.Hint.EXPANDED)) {
            position = TerminalPosition.OFFSET_1x1;
            size = screenSize.withRelative(
                    -Math.min(4, screenSize.column),
                    -Math.min(3, screenSize.row));
            if(!size.equals(window.getDecoratedSize())) {
                window.invalidate();
            }
        }
        else if(window.getHints().contains(Window.Hint.FIT_TERMINAL_WINDOW) ||
                window.getHints().contains(Window.Hint.CENTERED)) {
            //If the window is too big for the terminal, move it up towards 0x0 and if that's not enough then shrink
            //it instead
            while(position.row > 0 && position.row + size.row > screenSize.row) {
                position = position.withRelativeRow(-1);
            }
            while(position.column > 0 && position.column + size.column > screenSize.column) {
                position = position.withRelativeColumn(-1);
            }
            if(position.row + size.row > screenSize.row) {
                size = size.withRow(screenSize.row - position.row);
            }
            if(position.column + size.column > screenSize.column) {
                size = size.withColumn(screenSize.column - position.column);
            }
            if(window.getHints().contains(Window.Hint.CENTERED)) {
                int left = (lastKnownScreenSize.column - size.column) / 2;
                int top = (lastKnownScreenSize.row - size.row) / 2;
                position = new TerminalPosition(left, top);
            }
        }

        window.setPosition(position);
        window.setDecoratedSize(size);
    }
}
