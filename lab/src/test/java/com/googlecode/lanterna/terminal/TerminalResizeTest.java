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
package com.googlecode.lanterna.terminal;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TestTerminalFactory;
import com.googlecode.lanterna.input.KeyStroke;

import java.io.IOException;

/**
 *
 * @author Martin
 */
public class TerminalResizeTest implements TerminalResizeListener {

    public static void main(String[] args) throws InterruptedException, IOException {
        Terminal terminal = new TestTerminalFactory(args).createTerminal();
        terminal.enterPrivateMode();
        terminal.clearScreen();
        terminal.moveCursorTo(10, 5);
        terminal.put('H');
        terminal.put('e');
        terminal.put('l');
        terminal.put('l');
        terminal.put('o');
        terminal.put('!');
        terminal.moveCursorTo(0, 0);
        terminal.flush();
        terminal.addResizeListener(new TerminalResizeTest());

        while(true) {
            KeyStroke key = terminal.pollInput();
            if(key == null || key.getCharacter() != 'q') {
                Thread.sleep(1);
            }
            else {
                break;
            }
        }
        terminal.exitPrivateMode();
    }

    @Override
    public void onResized(Terminal terminal, TerminalPosition newSize) {
        try {
            terminal.moveCursorTo(0, 0);
            String string = newSize.column + "x" + newSize.row + "                     ";
            char[] chars = string.toCharArray();
            for(char c : chars) {
                terminal.put(c);
            }
            terminal.flush();
        }
        catch(IOException e) {
            throw new RuntimeException(e);
        }
    }
}
