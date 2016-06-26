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

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;
import com.googlecode.lanterna.graphics.AbstractTextGraphics;
import com.googlecode.lanterna.graphics.TextGraphics;

/**
 * Implementation of {@link TextGraphics} for {@link VirtualTerminal}
 * @author Martin
 */
class VirtualTerminalTextGraphics extends AbstractTextGraphics {
    private final DefaultVirtualTerminal virtualTerminal;

    VirtualTerminalTextGraphics(DefaultVirtualTerminal virtualTerminal) {
        this.virtualTerminal = virtualTerminal;
    }

    @Override
    public TextGraphics set(int columnIndex, int rowIndex, TextCharacter textCharacter) {
        TerminalPosition size = getSize();
        if(columnIndex < 0 || columnIndex >= size.column ||
                rowIndex < 0 || rowIndex >= size.row) {
            return this;
        }
        synchronized(virtualTerminal) {
            virtualTerminal.moveCursorTo(new TerminalPosition(columnIndex, rowIndex));
            virtualTerminal.putCharacter(textCharacter);
        }
        return this;
    }

    @Override
    public TextCharacter get(TerminalPosition position) {
        return virtualTerminal.getView(position);
    }

    @Override
    public TextCharacter get(int column, int row) {
        return get(new TerminalPosition(column, row));
    }

    @Override
    public TerminalPosition getSize() {
        return virtualTerminal.terminalSize();
    }
}
