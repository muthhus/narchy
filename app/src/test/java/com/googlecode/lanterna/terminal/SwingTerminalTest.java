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

import com.googlecode.lanterna.SGR;
import com.googlecode.lanterna.Symbols;
import com.googlecode.lanterna.TestTerminalFactory;
import com.googlecode.lanterna.terminal.swing.SwingTerminalFrame;

import javax.swing.*;

/**
 *
 * @author Martin
 */
public class SwingTerminalTest {

    public static void main(String[] args) throws InterruptedException {
        SwingTerminalFrame terminal = new TestTerminalFactory(args).createSwingTerminal();
        terminal.setVisible(true);
        terminal.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        terminal.enterPrivateMode();
        terminal.clearScreen();
        terminal.moveCursorTo(10, 5);
        terminal.put('H');
        terminal.put('e');
        terminal.put('l');
        terminal.put('l');
        terminal.put('o');
        terminal.put('!');
        terminal.put(' ');
        terminal.put(Symbols.HEART);
        terminal.put(Symbols.SPADES);
        terminal.put(Symbols.CLUB);
        terminal.put(Symbols.DIAMOND);
        terminal.put(Symbols.DOUBLE_LINE_CROSS);
        terminal.put(Symbols.SINGLE_LINE_CROSS);
        terminal.put(Symbols.DOUBLE_LINE_T_DOWN);
        terminal.put(Symbols.SINGLE_LINE_VERTICAL);
        terminal.put(Symbols.SINGLE_LINE_HORIZONTAL);
        terminal.moveCursorTo(10, 7);
        terminal.enableSGR(SGR.BOLD);
        terminal.put('H');
        terminal.put('e');
        terminal.put('l');
        terminal.put('l');
        terminal.put('o');
        terminal.put('!');
        terminal.put(' ');
        terminal.put(Symbols.HEART);
        terminal.put(Symbols.SPADES);
        terminal.put(Symbols.CLUB);
        terminal.put(Symbols.DIAMOND);
        terminal.put(Symbols.DOUBLE_LINE_CROSS);
        terminal.put(Symbols.SINGLE_LINE_CROSS);
        terminal.put(Symbols.DOUBLE_LINE_T_DOWN);
        terminal.put(Symbols.SINGLE_LINE_VERTICAL);
        terminal.put(Symbols.SINGLE_LINE_HORIZONTAL);
        terminal.moveCursorTo(10, 9);
        terminal.enableSGR(SGR.UNDERLINE);
        terminal.put('H');
        terminal.put('e');
        terminal.enableSGR(SGR.BOLD);
        terminal.put('l');
        terminal.enableSGR(SGR.UNDERLINE);
        terminal.put('l');
        terminal.put('o');
        terminal.enableSGR(SGR.UNDERLINE);
        terminal.put('!');
        terminal.put(' ');
        terminal.put(Symbols.HEART);
        terminal.put(Symbols.SPADES);
        terminal.put(Symbols.CLUB);
        terminal.put(Symbols.DIAMOND);
        terminal.put(Symbols.DOUBLE_LINE_CROSS);
        terminal.put(Symbols.SINGLE_LINE_CROSS);
        terminal.put(Symbols.DOUBLE_LINE_T_DOWN);
        terminal.put(Symbols.SINGLE_LINE_VERTICAL);
        terminal.put(Symbols.SINGLE_LINE_HORIZONTAL);
        terminal.moveCursorTo(10, 11);
        terminal.enableSGR(SGR.BORDERED);
        terminal.put('!');
        terminal.put(' ');
        terminal.put(Symbols.HEART);
        terminal.put(Symbols.SPADES);
        terminal.put(Symbols.CLUB);
        terminal.put(Symbols.DIAMOND);
        terminal.put(Symbols.DOUBLE_LINE_CROSS);
        terminal.put(Symbols.SINGLE_LINE_CROSS);
        terminal.put(Symbols.DOUBLE_LINE_T_DOWN);
        terminal.put(Symbols.SINGLE_LINE_VERTICAL);
        terminal.put(Symbols.SINGLE_LINE_HORIZONTAL);
        terminal.resetColorAndSGR();
        terminal.moveCursorTo(0, 0);
        terminal.flush();

        Thread.sleep(5000);
        terminal.exitPrivateMode();
    }
}
