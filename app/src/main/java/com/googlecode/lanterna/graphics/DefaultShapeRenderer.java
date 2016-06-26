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
package com.googlecode.lanterna.graphics;

import com.googlecode.lanterna.TerminalPosition;
import com.googlecode.lanterna.TextCharacter;

import java.util.Arrays;
import java.util.Comparator;

/**
 * Default implementation of ShapeRenderer. This class (and the interface) is mostly here to make the code cleaner in
 * {@code AbstractTextGraphics}.
 * @author Martin
 */
class DefaultShapeRenderer implements ShapeRenderer {
    interface Callback {
        void onPoint(int column, int row, TextCharacter character);
    }

    private final Callback callback;

    DefaultShapeRenderer(Callback callback) {
        this.callback = callback;
    }

    @Override
    public void drawLine(TerminalPosition p1, TerminalPosition p2, TextCharacter character) {
        //http://en.wikipedia.org/wiki/Bresenham%27s_line_algorithm
        //Implementation from Graphics Programming Black Book by Michael Abrash
        //Available at http://www.gamedev.net/page/resources/_/technical/graphics-programming-and-theory/graphics-programming-black-book-r1698
        if(p1.row > p2.row) {
            TerminalPosition temp = p1;
            p1 = p2;
            p2 = temp;
        }
        int deltaX = p2.column - p1.column;
        int deltaY = p2.row - p1.row;
        if(deltaX > 0) {
            if(deltaX > deltaY) {
                drawLine0(p1, deltaX, deltaY, true, character);
            }
            else {
                drawLine1(p1, deltaX, deltaY, true, character);
            }
        }
        else {
            deltaX = Math.abs(deltaX);
            if(deltaX > deltaY) {
                drawLine0(p1, deltaX, deltaY, false, character);
            }
            else {
                drawLine1(p1, deltaX, deltaY, false, character);
            }
        }
    }

    private void drawLine0(TerminalPosition start, int deltaX, int deltaY, boolean leftToRight, TextCharacter character) {
        int x = start.column;
        int y = start.row;
        int deltaYx2 = deltaY * 2;
        int deltaYx2MinusDeltaXx2 = deltaYx2 - (deltaX * 2);
        int errorTerm = deltaYx2 - deltaX;
        callback.onPoint(x, y, character);
        while(deltaX-- > 0) {
            if(errorTerm >= 0) {
                y++;
                errorTerm += deltaYx2MinusDeltaXx2;
            }
            else {
                errorTerm += deltaYx2;
            }
            x += leftToRight ? 1 : -1;
            callback.onPoint(x, y, character);
        }
    }

    private void drawLine1(TerminalPosition start, int deltaX, int deltaY, boolean leftToRight, TextCharacter character) {
        int x = start.column;
        int y = start.row;
        int deltaXx2 = deltaX * 2;
        int deltaXx2MinusDeltaYx2 = deltaXx2 - (deltaY * 2);
        int errorTerm = deltaXx2 - deltaY;
        callback.onPoint(x, y, character);
        while(deltaY-- > 0) {
            if(errorTerm >= 0) {
                x += leftToRight ? 1 : -1;
                errorTerm += deltaXx2MinusDeltaYx2;
            }
            else {
                errorTerm += deltaXx2;
            }
            y++;
            callback.onPoint(x, y, character);
        }
    }

    @Override
    public void drawTriangle(TerminalPosition p1, TerminalPosition p2, TerminalPosition p3, TextCharacter character) {
        drawLine(p1, p2, character);
        drawLine(p2, p3, character);
        drawLine(p3, p1, character);
    }

    @Override
    public void drawRectangle(TerminalPosition topLeft, TerminalPosition size, TextCharacter character) {
        TerminalPosition topRight = topLeft.withRelativeColumn(size.column - 1);
        TerminalPosition bottomRight = topRight.withRelativeRow(size.row - 1);
        TerminalPosition bottomLeft = topLeft.withRelativeRow(size.row - 1);
        drawLine(topLeft, topRight, character);
        drawLine(topRight, bottomRight, character);
        drawLine(bottomRight, bottomLeft, character);
        drawLine(bottomLeft, topLeft, character);
    }

    @Override
    public void fillTriangle(TerminalPosition p1, TerminalPosition p2, TerminalPosition p3, TextCharacter character) {
        //I've used the algorithm described here:
        //http://www-users.mat.uni.torun.pl/~wrona/3d_tutor/tri_fillers.html
        TerminalPosition[] points = new TerminalPosition[]{p1, p2, p3};
        Arrays.sort(points, new Comparator<TerminalPosition>() {
            @Override
            public int compare(TerminalPosition o1, TerminalPosition o2) {
                return (o1.row < o2.row) ? -1 : ((o1.row == o2.row) ? 0 : 1);
            }
        });

        float dx1, dx2, dx3;
        if (points[1].row - points[0].row > 0) {
            dx1 = (float)(points[1].column - points[0].column) / (float)(points[1].row - points[0].row);
        }
        else {
            dx1 = 0;
        }
        if (points[2].row - points[0].row > 0) {
            dx2 = (float)(points[2].column - points[0].column) / (float)(points[2].row - points[0].row);
        }
        else {
            dx2 = 0;
        }
        if (points[2].row - points[1].row > 0) {
            dx3 = (float)(points[2].column - points[1].column) / (float)(points[2].row - points[1].row);
        }
        else {
            dx3 = 0;
        }

        float startX, startY, endX;
        startX = endX = points[0].column;
        startY = points[0].row;
        if (dx1 > dx2) {
            for (; startY <= points[1].row; startY++, startX += dx2, endX += dx1) {
                drawLine(new TerminalPosition((int)startX, (int)startY), new TerminalPosition((int)endX, (int)startY), character);
            }
            endX = points[1].column;
            for (; startY <= points[2].row; startY++, startX += dx2, endX += dx3) {
                drawLine(new TerminalPosition((int)startX, (int)startY), new TerminalPosition((int)endX, (int)startY), character);
            }
        } else {
            for (; startY <= points[1].row; startY++, startX += dx1, endX += dx2) {
                drawLine(new TerminalPosition((int)startX, (int)startY), new TerminalPosition((int)endX, (int)startY), character);
            }
            startX = points[1].column;
            startY = points[1].row;
            for (; startY <= points[2].row; startY++, startX += dx3, endX += dx2) {
                drawLine(new TerminalPosition((int)startX, (int)startY), new TerminalPosition((int)endX, (int)startY), character);
            }
        }
    }

    @Override
    public void fillRectangle(TerminalPosition topLeft, TerminalPosition size, TextCharacter character) {
        for(int y = 0; y < size.row; y++) {
            for(int x = 0; x < size.column; x++) {
                callback.onPoint(topLeft.column + x, topLeft.row + y, character);
            }
        }
    }
}
