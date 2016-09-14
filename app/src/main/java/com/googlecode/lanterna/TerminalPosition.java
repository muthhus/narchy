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
package com.googlecode.lanterna;

import nars.util.Util;

/**
 * A 2-d position in 'terminal space'. Please note that the coordinates are 0-indexed, meaning 0x0 is the top left
 * corner of the terminal. This object is immutable so you cannot change it after it has been created. Instead, you
 * can easily create modified 'clones' by using the 'with' methods.
 *
 * @author Martin
 */
public final class TerminalPosition implements Comparable<TerminalPosition> {

    /**
     * Constant for the top-left corner (0x0)
     */
    public static final TerminalPosition TOP_LEFT_CORNER = new TerminalPosition(0, 0);
    /**
     * Constant for the 1x1 position (one offset in both directions from top-left)
     */
    public static final TerminalPosition OFFSET_1x1 = new TerminalPosition(1, 1);
    public static final TerminalPosition ZERO = new TerminalPosition(0, 0);
    public static final TerminalPosition ONE = new TerminalPosition(1, 1);


    /**
     * Returns the index of the column this position is representing, zero indexed (the first column has index 0)
     * @return Index of the row this position has
     */
    public final int row;

    /**
     * Returns the index of the row this position is representing, zero indexed (the first row has index 0)
     * @return Index of the row this position has
     */
    public final int col;

    /**
     * Creates a new TerminalPosition object, which represents a location on the screen. There is no check to verify
     * that the position you specified is within the size of the current terminal and you can specify negative positions
     * as well.
     *
     * @param col Column of the location, or the "x" coordinate, zero indexed (the first column is 0)
     * @param row Row of the location, or the "y" coordinate, zero indexed (the first row is 0)
     */
    public TerminalPosition(int col, int row) {
        this.row = row;
        this.col = col;
    }

    /**
     * Creates a new TerminalPosition object representing a position with the same column index as this but with a
     * supplied row index.
     * @param row Index of the row for the new position
     * @return A TerminalPosition object with the same column as this but with a specified row index
     */
    public TerminalPosition withRow(int row) {
        if(row == 0 && this.col == 0) {
            return TOP_LEFT_CORNER;
        }
        return new TerminalPosition(this.col, row);
    }

    /**
     * Creates a new TerminalPosition object representing a position with the same row index as this but with a
     * supplied column index.
     * @param column Index of the column for the new position
     * @return A TerminalPosition object with the same row as this but with a specified column index
     */
    public TerminalPosition withColumn(int column) {
        if(column == 0 && this.row == 0) {
            return TOP_LEFT_CORNER;
        }
        return new TerminalPosition(column, this.row);
    }

    /**
     * Creates a new TerminalPosition object representing a position on the same row, but with a column offset by a
     * supplied value. Calling this method with delta 0 will return this, calling it with a positive delta will return
     * a terminal position <i>delta</i> number of columns to the right and for negative numbers the same to the left.
     * @param delta Column offset
     * @return New terminal position based off this one but with an applied offset
     */
    public TerminalPosition withRelativeColumn(int delta) {
        if(delta == 0) {
            return this;
        }
        return withColumn(col + delta);
    }

    /**
     * Creates a new TerminalPosition object representing a position on the same column, but with a row offset by a
     * supplied value. Calling this method with delta 0 will return this, calling it with a positive delta will return
     * a terminal position <i>delta</i> number of rows to the down and for negative numbers the same up.
     * @param delta Row offset
     * @return New terminal position based off this one but with an applied offset
     */
    public final TerminalPosition withRelativeRow(int delta) {
        return delta == 0 ? this : withRow(row + delta);
    }

    /**
     * Creates a new TerminalPosition object that is 'translated' by an amount of rows and columns specified by another
     * TerminalPosition. Same as calling
     * <code>withRelativeRow(translate.getRow()).withRelativeColumn(translate.getColumn())</code>
     * @param translate How many columns and rows to translate
     * @return New TerminalPosition that is the result of the original with added translation
     */
    public TerminalPosition withRelative(TerminalPosition translate) {
        return withRelative(translate.col, translate.row);
    }

    /**
     * Creates a new TerminalPosition object that is 'translated' by an amount of rows and columns specified by the two
     * parameters. Same as calling
     * <code>withRelativeRow(deltaRow).withRelativeColumn(deltaColumn)</code>
     * @param deltaColumn How many columns to move from the current position in the new TerminalPosition
     * @param deltaRow How many rows to move from the current position in the new TerminalPosition
     * @return New TerminalPosition that is the result of the original position with added translation
     */
    public TerminalPosition withRelative(int deltaColumn, int deltaRow) {
        return withRelativeRow(deltaRow).withRelativeColumn(deltaColumn);
    }

    @Override
    public int compareTo(TerminalPosition o) {
        int r = this.row;
        int or = o.row;
        if(r < or) {
            return -1;
        } else if(r > or) {
            return 1;
        }
        return Integer.compare(this.col, o.col);
    }

    @Override
    public String toString() {
        return "[" + col + ':' + row + "]";
    }

    @Override
    public int hashCode() {
        return Util.hashCombine(row, col);
    }

    public boolean equals(int columnIndex, int rowIndex) {
        return this.col == columnIndex && this.row == rowIndex;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null) return false;

        if (obj instanceof TerminalPosition) {
            final TerminalPosition other = (TerminalPosition) obj;
            return equals(other.col, other.row);
        }
        return false;
    }

    /**
     * Takes a different TerminalPosition and returns a new TerminalPosition that has the largest dimensions of the two,
     * measured separately. So calling 3x5 on a 5x3 will return 5x5.
     * @param other Other TerminalSize to compare with
     * @return TerminalSize that combines the maximum width between the two and the maximum height
     */
    public TerminalPosition max(TerminalPosition other) {
        return withColumn(Math.max(col, other.col))
                .withRow(Math.max(row, other.row));
    }

    /**
     * Takes a different TerminalPosition and returns a new TerminalPosition that has the smallest dimensions of the two,
     * measured separately. So calling 3x5 on a 5x3 will return 3x3.
     * @param other Other TerminalSize to compare with
     * @return TerminalSize that combines the minimum width between the two and the minimum height
     */
    public TerminalPosition min(TerminalPosition other) {
        return withColumn(Math.min(col, other.col))
                .withRow(Math.min(row, other.row));
    }
    public TerminalPosition with(TerminalPosition size) {
        if(equals(size)) {
            return this;
        }
        return size;
    }

    public final boolean isZero() {
        return row==0 && col ==0;
    }
}

