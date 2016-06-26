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
import com.googlecode.lanterna.TerminalTextUtils;
import com.googlecode.lanterna.input.KeyStroke;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * This component keeps a text content that is editable by the user. A TextBox can be single line or multiline and lets
 * the user navigate the cursor in the text area by using the arrow keys, page up, page down, home and end. For
 * multi-line {@code TextBox}:es, scrollbars will be automatically displayed if needed.
 * <p>
 * Size-wise, a {@code TextBox} should be hard-coded to a particular size, it's not good at guessing how large it should
 * be. You can do this through the constructor.
 */
public class TextBox extends AbstractInteractableComponent<TextBox> {

    /**
     * Enum value to force a {@code TextBox} to be either single line or multi line. This is usually auto-detected if
     * the text box has some initial content by scanning that content for \n characters.
     */
    public enum Style {
        /**
         * The {@code TextBox} contains a single line of text and is typically drawn on one row
         */
        SINGLE_LINE,
        /**
         * The {@code TextBox} contains a none, one or many lines of text and is normally drawn over multiple lines
         */
        MULTI_LINE,
        ;
    }

    private final List<String> lines;
    private final Style style;

    private TerminalPosition caretPosition;
    private boolean caretWarp;
    private boolean readOnly;
    private boolean horizontalFocusSwitching;
    private boolean verticalFocusSwitching;
    private final int maxLineLength;
    private int longestRow;
    private final char unusedSpaceCharacter;
    private Character mask;
    private Pattern validationPattern;

    /**
     * Default constructor, this creates a single-line {@code TextBox} of size 10 which is initially empty
     */
    public TextBox() {
        this(new TerminalPosition(10, 1), "", Style.SINGLE_LINE);
    }

    /**
     * Constructor that creates a {@code TextBox} with an initial content and attempting to be big enough to display
     * the whole text at once without scrollbars
     * @param initialContent Initial content of the {@code TextBox}
     */
    public TextBox(String initialContent) {
        this(null, initialContent, initialContent.contains("\n") ? Style.MULTI_LINE : Style.SINGLE_LINE);
    }

    /**
     * Creates a {@code TextBox} that has an initial content and attempting to be big enough to display the whole text
     * at once without scrollbars.
     *
     * @param initialContent Initial content of the {@code TextBox}
     * @param style Forced style instead of auto-detecting
     */
    public TextBox(String initialContent, Style style) {
        this(null, initialContent, style);
    }

    /**
     * Creates a new empty {@code TextBox} with a specific size
     * @param preferredSize Size of the {@code TextBox}
     */
    public TextBox(TerminalPosition preferredSize) {
        this(preferredSize, (preferredSize != null && preferredSize.row > 1) ? Style.MULTI_LINE : Style.SINGLE_LINE);
    }

    /**
     * Creates a new empty {@code TextBox} with a specific size and style
     * @param preferredSize Size of the {@code TextBox}
     * @param style Style to use
     */
    public TextBox(TerminalPosition preferredSize, Style style) {
        this(preferredSize, "", style);
    }

    /**
     * Creates a new empty {@code TextBox} with a specific size and initial content
     * @param preferredSize Size of the {@code TextBox}
     * @param initialContent Initial content of the {@code TextBox}
     */
    public TextBox(TerminalPosition preferredSize, String initialContent) {
        this(preferredSize, initialContent, (preferredSize != null && preferredSize.row > 1) || initialContent.contains("\n") ? Style.MULTI_LINE : Style.SINGLE_LINE);
    }

    /**
     * Main constructor of the {@code TextBox} which decides size, initial content and style
     * @param preferredSize Size of the {@code TextBox}
     * @param initialContent Initial content of the {@code TextBox}
     * @param style Style to use for this {@code TextBox}, instead of auto-detecting
     */
    public TextBox(TerminalPosition preferredSize, String initialContent, Style style) {
        this.lines = new ArrayList<>();
        this.style = style;
        this.readOnly = false;
        this.caretWarp = false;
        this.verticalFocusSwitching = true;
        this.horizontalFocusSwitching = (style == Style.SINGLE_LINE);
        this.caretPosition = TerminalPosition.TOP_LEFT_CORNER;
        this.maxLineLength = -1;
        this.longestRow = 1;    //To fit the cursor
        this.unusedSpaceCharacter = ' ';
        this.mask = null;
        this.validationPattern = null;
        setText(initialContent);
        if (preferredSize == null) {
            preferredSize = new TerminalPosition(Math.max(10, longestRow), lines.size());
        }
        setPreferredSize(preferredSize);
    }

    /**
     * Sets a pattern on which the content of the text box is to be validated. For multi-line TextBox:s, the pattern is
     * checked against each line individually, not the content as a whole. Partial matchings will not be allowed, the
     * whole pattern must match, however, empty lines will always be allowed. When the user tried to modify the content
     * of the TextBox in a way that does not match the pattern, the operation will be silently ignored. If you set this
     * pattern to {@code null}, all validation is turned off.
     * @param validationPattern Pattern to validate the lines in this TextBox against, or {@code null} to disable
     * @return itself
     */
    public synchronized TextBox setValidationPattern(Pattern validationPattern) {
        if(validationPattern != null) {
            for(String line: lines) {
                if(!validated(line)) {
                    throw new IllegalStateException("TextBox validation pattern " + validationPattern + " does not match existing content");
                }
            }
        }
        this.validationPattern = validationPattern;
        return this;
    }

    /**
     * Updates the text content of the {@code TextBox} to the supplied string.
     * @param text New text to assign to the {@code TextBox}
     * @return Itself
     */
    public synchronized TextBox setText(String text) {
        String[] split = text.split("\n");
        lines.clear();
        longestRow = 1;
        for(String line : split) {
            addLine(line);
        }
        if(caretPosition.row > lines.size() - 1) {
            caretPosition = caretPosition.withRow(lines.size() - 1);
        }
        if(caretPosition.column > lines.get(caretPosition.row).length()) {
            caretPosition = caretPosition.withColumn(lines.get(caretPosition.row).length());
        }
        invalidate();
        return this;
    }

    @Override
    public TextBoxRenderer getRenderer() {
        return (TextBoxRenderer)super.getRenderer();
    }

    /**
     * Adds a single line to the {@code TextBox} at the end, this only works when in multi-line mode
     * @param line Line to add at the end of the content in this {@code TextBox}
     * @return Itself
     */
    public synchronized TextBox addLine(String line) {
        StringBuilder bob = new StringBuilder();
        for(int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if(c == '\n' && style == Style.MULTI_LINE) {
                String string = bob.toString();
                int lineWidth = TerminalTextUtils.getColumnWidth(string);
                lines.add(string);
                if(longestRow < lineWidth + 1) {
                    longestRow = lineWidth + 1;
                }
                addLine(line.substring(i + 1));
                return this;
            }
            else if(Character.isISOControl(c)) {
                continue;
            }

            bob.append(c);
        }
        String string = bob.toString();
        if(!validated(string)) {
            throw new IllegalStateException("TextBox validation pattern " + validationPattern + " does not match the supplied text");
        }
        int lineWidth = TerminalTextUtils.getColumnWidth(string);
        lines.add(string);
        if(longestRow < lineWidth + 1) {
            longestRow = lineWidth + 1;
        }
        invalidate();
        return this;
    }

    /**
     * Sets if the caret should jump to the beginning of the next line if right arrow is pressed while at the end of a
     * line. Similarly, pressing left arrow at the beginning of a line will make the caret jump to the end of the
     * previous line. This only makes sense for multi-line TextBox:es; for single-line ones it has no effect. By default
     * this is {@code false}.
     * @param caretWarp Whether the caret will warp at the beginning/end of lines
     * @return Itself
     */
    public TextBox setCaretWarp(boolean caretWarp) {
        this.caretWarp = caretWarp;
        return this;
    }

    /**
     * Checks whether caret warp mode is enabled or not. See {@code setCaretWarp} for more details.
     * @return {@code true} if caret warp mode is enabled
     */
    public boolean isCaretWarp() {
        return caretWarp;
    }

    /**
     * Returns the position of the caret, as a {@code TerminalPosition} where the row and columns equals the coordinates
     * in a multi-line {@code TextBox} and for single-line {@code TextBox} you can ignore the {@code row} component.
     * @return Position of the text input caret
     */
    public TerminalPosition getCaretPosition() {
        return caretPosition;
    }

    /**
     * Returns the text in this {@code TextBox}, for multi-line mode all lines will be concatenated together with \n as
     * separator.
     * @return The text inside this {@code TextBox}
     */
    public synchronized String getText() {
        StringBuilder bob = new StringBuilder(lines.get(0));
        for(int i = 1; i < lines.size(); i++) {
            bob.append('\n').append(lines.get(i));
        }
        return bob.toString();
    }

    /**
     * Helper method, it will return the content of the {@code TextBox} unless it's empty in which case it will return
     * the supplied default value
     * @param defaultValueIfEmpty Value to return if the {@code TextBox} is empty
     * @return Text in the {@code TextBox} or {@code defaultValueIfEmpty} is the {@code TextBox} is empty
     */
    public String getTextOrDefault(String defaultValueIfEmpty) {
        String text = getText();
        if(text.isEmpty()) {
            return defaultValueIfEmpty;
        }
        return text;
    }

    /**
     * Returns the current text mask, meaning the substitute to draw instead of the text inside the {@code TextBox}.
     * This is normally used for password input fields so the password isn't shown
     * @return Current text mask or {@code null} if there is no mask
     */
    public Character getMask() {
        return mask;
    }

    /**
     * Sets the current text mask, meaning the substitute to draw instead of the text inside the {@code TextBox}.
     * This is normally used for password input fields so the password isn't shown
     * @param mask New text mask or {@code null} if there is no mask
     * @return Itself
     */
    public TextBox setMask(Character mask) {
        if(mask != null && TerminalTextUtils.isCharCJK(mask)) {
            throw new IllegalArgumentException("Cannot use a CJK character as a mask");
        }
        this.mask = mask;
        invalidate();
        return this;
    }

    /**
     * Returns {@code true} if this {@code TextBox} is in read-only mode, meaning text input from the user through the
     * keyboard is prevented
     * @return {@code true} if this {@code TextBox} is in read-only mode
     */
    public boolean isReadOnly() {
        return readOnly;
    }

    /**
     * Sets the read-only mode of the {@code TextBox}, meaning text input from the user through the keyboard is
     * prevented. The user can still focus and scroll through the text in this mode.
     * @param readOnly If {@code true} then the {@code TextBox} will switch to read-only mode
     * @return Itself
     */
    public TextBox setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        invalidate();
        return this;
    }

    /**
     * If {@code true}, the component will switch to the next available component above if the cursor is at the top of
     * the TextBox and the user presses the 'up' array key, or switch to the next available component below if the
     * cursor is at the bottom of the TextBox and the user presses the 'down' array key. The means that for single-line
     * TextBox:es, pressing up and down will always switch focus.
     * @return {@code true} if vertical focus switching is enabled
     */
    public boolean isVerticalFocusSwitching() {
        return verticalFocusSwitching;
    }

    /**
     * If set to {@code true}, the component will switch to the next available component above if the cursor is at the
     * top of the TextBox and the user presses the 'up' array key, or switch to the next available component below if
     * the cursor is at the bottom of the TextBox and the user presses the 'down' array key. The means that for
     * single-line TextBox:es, pressing up and down will always switch focus with this mode enabled.
     * @param verticalFocusSwitching If called with true, vertical focus switching will be enabled
     * @return Itself
     */
    public TextBox setVerticalFocusSwitching(boolean verticalFocusSwitching) {
        this.verticalFocusSwitching = verticalFocusSwitching;
        return this;
    }

    /**
     * If {@code true}, the TextBox will switch focus to the next available component to the left if the cursor in the
     * TextBox is at the left-most position (index 0) on the row and the user pressed the 'left' arrow key, or vice
     * versa for pressing the 'right' arrow key when the cursor in at the right-most position of the current row.
     * @return {@code true} if horizontal focus switching is enabled
     */
    public boolean isHorizontalFocusSwitching() {
        return horizontalFocusSwitching;
    }

    /**
     * If set to {@code true}, the TextBox will switch focus to the next available component to the left if the cursor
     * in the TextBox is at the left-most position (index 0) on the row and the user pressed the 'left' arrow key, or
     * vice versa for pressing the 'right' arrow key when the cursor in at the right-most position of the current row.
     * @param horizontalFocusSwitching If called with true, horizontal focus switching will be enabled
     * @return Itself
     */
    public TextBox setHorizontalFocusSwitching(boolean horizontalFocusSwitching) {
        this.horizontalFocusSwitching = horizontalFocusSwitching;
        return this;
    }

    /**
     * Returns the line on the specific row. For non-multiline TextBox:es, calling this with index set to 0 will return
     * the same as calling {@code getText()}. If the row index is invalid (less than zero or equals or larger than the
     * number of rows), this method will throw IndexOutOfBoundsException.
     * @param index Index of the row to return the contents from
     * @return The line at the specified index, as a String
     * @throws IndexOutOfBoundsException if the row index is less than zero or too large
     */
    public synchronized String getLine(int index) {
        return lines.get(index);
    }

    /**
     * Returns the number of lines currently in this TextBox. For single-line TextBox:es, this will always return 1.
     * @return Number of lines of text currently in this TextBox
     */
    public synchronized int getLineCount() {
        return lines.size();
    }

    @Override
    protected TextBoxRenderer createDefaultRenderer() {
        return new DefaultTextBoxRenderer();
    }

    @Override
    public synchronized Result handleKeyStroke(KeyStroke keyStroke) {
        if(readOnly) {
            return handleKeyStrokeReadOnly(keyStroke);
        }
        String line = lines.get(caretPosition.row);
        switch(keyStroke.getKeyType()) {
            case Character:
                if(maxLineLength == -1 || maxLineLength > line.length() + 1) {
                    line = line.substring(0, caretPosition.column) + keyStroke.getCharacter() + line.substring(caretPosition.column);
                    if(validated(line)) {
                        lines.set(caretPosition.row, line);
                        caretPosition = caretPosition.withRelativeColumn(1);
                    }
                }
                return Result.HANDLED;
            case Backspace:
                if(caretPosition.column > 0) {
                    line = line.substring(0, caretPosition.column - 1) + line.substring(caretPosition.column);
                    if(validated(line)) {
                        lines.set(caretPosition.row, line);
                        caretPosition = caretPosition.withRelativeColumn(-1);
                    }
                }
                else if(style == Style.MULTI_LINE && caretPosition.row > 0) {
                    String concatenatedLines = lines.get(caretPosition.row - 1) + line;
                    if(validated(concatenatedLines)) {
                        lines.remove(caretPosition.row);
                        caretPosition = caretPosition.withRelativeRow(-1);
                        caretPosition = caretPosition.withColumn(lines.get(caretPosition.row).length());
                        lines.set(caretPosition.row, concatenatedLines);
                    }
                }
                return Result.HANDLED;
            case Delete:
                if(caretPosition.column < line.length()) {
                    line = line.substring(0, caretPosition.column) + line.substring(caretPosition.column + 1);
                    if(validated(line)) {
                        lines.set(caretPosition.row, line);
                    }
                }
                else if(style == Style.MULTI_LINE && caretPosition.row < lines.size() - 1) {
                    String concatenatedLines = line + lines.get(caretPosition.row + 1);
                    if(validated(concatenatedLines)) {
                        lines.set(caretPosition.row, concatenatedLines);
                        lines.remove(caretPosition.row + 1);
                    }
                }
                return Result.HANDLED;
            case ArrowLeft:
                if(caretPosition.column > 0) {
                    caretPosition = caretPosition.withRelativeColumn(-1);
                }
                else if(style == Style.MULTI_LINE && caretWarp && caretPosition.row > 0) {
                    caretPosition = caretPosition.withRelativeRow(-1);
                    caretPosition = caretPosition.withColumn(lines.get(caretPosition.row).length());
                }
                else if(horizontalFocusSwitching) {
                    return Result.MOVE_FOCUS_LEFT;
                }
                return Result.HANDLED;
            case ArrowRight:
                if(caretPosition.column < lines.get(caretPosition.row).length()) {
                    caretPosition = caretPosition.withRelativeColumn(1);
                }
                else if(style == Style.MULTI_LINE && caretWarp && caretPosition.row < lines.size() - 1) {
                    caretPosition = caretPosition.withRelativeRow(1);
                    caretPosition = caretPosition.withColumn(0);
                }
                else if(horizontalFocusSwitching) {
                    return Result.MOVE_FOCUS_RIGHT;
                }
                return Result.HANDLED;
            case ArrowUp:
                if(caretPosition.row > 0) {
                    int trueColumnPosition = TerminalTextUtils.getColumnIndex(lines.get(caretPosition.row), caretPosition.column);
                    caretPosition = caretPosition.withRelativeRow(-1);
                    line = lines.get(caretPosition.row);
                    if(trueColumnPosition > TerminalTextUtils.getColumnWidth(line)) {
                        caretPosition = caretPosition.withColumn(line.length());
                    }
                    else {
                        caretPosition = caretPosition.withColumn(TerminalTextUtils.getStringCharacterIndex(line, trueColumnPosition));
                    }
                }
                else if(verticalFocusSwitching) {
                    return Result.MOVE_FOCUS_UP;
                }
                return Result.HANDLED;
            case ArrowDown:
                if(caretPosition.row < lines.size() - 1) {
                    int trueColumnPosition = TerminalTextUtils.getColumnIndex(lines.get(caretPosition.row), caretPosition.column);
                    caretPosition = caretPosition.withRelativeRow(1);
                    line = lines.get(caretPosition.row);
                    if(trueColumnPosition > TerminalTextUtils.getColumnWidth(line)) {
                        caretPosition = caretPosition.withColumn(line.length());
                    }
                    else {
                        caretPosition = caretPosition.withColumn(TerminalTextUtils.getStringCharacterIndex(line, trueColumnPosition));
                    }
                }
                else if(verticalFocusSwitching) {
                    return Result.MOVE_FOCUS_DOWN;
                }
                return Result.HANDLED;
            case End:
                caretPosition = caretPosition.withColumn(line.length());
                return Result.HANDLED;
            case Enter:
                if(style == Style.SINGLE_LINE) {
                    return Result.MOVE_FOCUS_NEXT;
                }
                String newLine = line.substring(caretPosition.column);
                String oldLine = line.substring(0, caretPosition.column);
                if(validated(newLine) && validated(oldLine)) {
                    lines.set(caretPosition.row, oldLine);
                    lines.add(caretPosition.row + 1, newLine);
                    caretPosition = caretPosition.withColumn(0).withRelativeRow(1);
                }
                return Result.HANDLED;
            case Home:
                caretPosition = caretPosition.withColumn(0);
                return Result.HANDLED;
            case PageDown:
                caretPosition = caretPosition.withRelativeRow(getSize().row);
                if(caretPosition.row > lines.size() - 1) {
                    caretPosition = caretPosition.withRow(lines.size() - 1);
                }
                if(lines.get(caretPosition.row).length() < caretPosition.column) {
                    caretPosition = caretPosition.withColumn(lines.get(caretPosition.row).length());
                }
                return Result.HANDLED;
            case PageUp:
                caretPosition = caretPosition.withRelativeRow(-getSize().row);
                if(caretPosition.row < 0) {
                    caretPosition = caretPosition.withRow(0);
                }
                if(lines.get(caretPosition.row).length() < caretPosition.column) {
                    caretPosition = caretPosition.withColumn(lines.get(caretPosition.row).length());
                }
                return Result.HANDLED;
            default:
        }
        return super.handleKeyStroke(keyStroke);
    }

    private boolean validated(String line) {
        return validationPattern == null || line.isEmpty() || validationPattern.matcher(line).matches();
    }

    private Result handleKeyStrokeReadOnly(KeyStroke keyStroke) {
        switch (keyStroke.getKeyType()) {
            case ArrowLeft:
                if(getRenderer().getViewTopLeft().column == 0 && horizontalFocusSwitching) {
                    return Result.MOVE_FOCUS_LEFT;
                }
                getRenderer().setViewTopLeft(getRenderer().getViewTopLeft().withRelativeColumn(-1));
                return Result.HANDLED;
            case ArrowRight:
                if(getRenderer().getViewTopLeft().column + getSize().column == longestRow && horizontalFocusSwitching) {
                    return Result.MOVE_FOCUS_RIGHT;
                }
                getRenderer().setViewTopLeft(getRenderer().getViewTopLeft().withRelativeColumn(1));
                return Result.HANDLED;
            case ArrowUp:
                if(getRenderer().getViewTopLeft().row == 0 && verticalFocusSwitching) {
                    return Result.MOVE_FOCUS_UP;
                }
                getRenderer().setViewTopLeft(getRenderer().getViewTopLeft().withRelativeRow(-1));
                return Result.HANDLED;
            case ArrowDown:
                if(getRenderer().getViewTopLeft().row + getSize().row == lines.size() && verticalFocusSwitching) {
                    return Result.MOVE_FOCUS_DOWN;
                }
                getRenderer().setViewTopLeft(getRenderer().getViewTopLeft().withRelativeRow(1));
                return Result.HANDLED;
            case Home:
                getRenderer().setViewTopLeft(TerminalPosition.TOP_LEFT_CORNER);
                return Result.HANDLED;
            case End:
                getRenderer().setViewTopLeft(TerminalPosition.TOP_LEFT_CORNER.withRow(getLineCount() - getSize().row));
                return Result.HANDLED;
            case PageDown:
                getRenderer().setViewTopLeft(getRenderer().getViewTopLeft().withRelativeRow(getSize().row));
                return Result.HANDLED;
            case PageUp:
                getRenderer().setViewTopLeft(getRenderer().getViewTopLeft().withRelativeRow(-getSize().row));
                return Result.HANDLED;
            default:
        }
        return super.handleKeyStroke(keyStroke);
    }

    /**
     * Helper interface that doesn't add any new methods but makes coding new text box renderers a little bit more clear
     */
    public interface TextBoxRenderer extends InteractableRenderer<TextBox> {
        TerminalPosition getViewTopLeft();
        void setViewTopLeft(TerminalPosition position);
    }

    /**
     * This is the default text box renderer that is used if you don't override anything. With this renderer, the text
     * box is filled with a solid background color and the text is drawn on top of it. Scrollbars are added for
     * multi-line text whenever the text inside the {@code TextBox} does not fit in the available area.
     */
    public static class DefaultTextBoxRenderer implements TextBoxRenderer {
        private TerminalPosition viewTopLeft;
        private final ScrollBar verticalScrollBar;
        private final ScrollBar horizontalScrollBar;
        private boolean hideScrollBars;

        /**
         * Default constructor
         */
        public DefaultTextBoxRenderer() {
            viewTopLeft = TerminalPosition.TOP_LEFT_CORNER;
            verticalScrollBar = new ScrollBar(Direction.VERTICAL);
            horizontalScrollBar = new ScrollBar(Direction.HORIZONTAL);
            hideScrollBars = false;
        }

        @Override
        public TerminalPosition getViewTopLeft() {
            return viewTopLeft;
        }

        @Override
        public void setViewTopLeft(TerminalPosition position) {
            if(position.column < 0) {
                position = position.withColumn(0);
            }
            if(position.row < 0) {
                position = position.withRow(0);
            }
            viewTopLeft = position;
        }

        @Override
        public TerminalPosition getCursorLocation(TextBox component) {
            if(component.isReadOnly()) {
                return null;
            }

            //Adjust caret position if necessary
            TerminalPosition caretPosition = component.getCaretPosition();
            String line = component.getLine(caretPosition.row);
            caretPosition = caretPosition.withColumn(Math.min(caretPosition.column, line.length()));

            return caretPosition
                    .withColumn(TerminalTextUtils.getColumnIndex(line, caretPosition.column))
                    .withRelativeColumn(-viewTopLeft.column)
                    .withRelativeRow(-viewTopLeft.row);
        }

        @Override
        public TerminalPosition getPreferredSize(TextBox component) {
            return new TerminalPosition(component.longestRow, component.lines.size());
        }

        /**
         * Controls whether scrollbars should be visible or not when a multi-line {@code TextBox} has more content than
         * it can draw in the area it was assigned (default: false)
         * @param hideScrollBars If {@code true}, don't show scrollbars if the multi-line content is bigger than the
         *                       area
         */
        public void setHideScrollBars(boolean hideScrollBars) {
            this.hideScrollBars = hideScrollBars;
        }

        @Override
        public void drawComponent(TextGUIGraphics graphics, TextBox component) {
            TerminalPosition realTextArea = graphics.getSize();
            if(realTextArea.row == 0 || realTextArea.column == 0) {
                return;
            }
            boolean drawVerticalScrollBar = false;
            boolean drawHorizontalScrollBar = false;
            int textBoxLineCount = component.getLineCount();
            if(!hideScrollBars && textBoxLineCount > realTextArea.row && realTextArea.column > 1) {
                realTextArea = realTextArea.withRelativeColumn(-1);
                drawVerticalScrollBar = true;
            }
            if(!hideScrollBars && component.longestRow > realTextArea.column && realTextArea.row > 1) {
                realTextArea = realTextArea.withRelativeRow(-1);
                drawHorizontalScrollBar = true;
                if(textBoxLineCount > realTextArea.row && realTextArea.row == graphics.getSize().row) {
                    realTextArea = realTextArea.withRelativeColumn(-1);
                    drawVerticalScrollBar = true;
                }
            }

            drawTextArea(graphics.newTextGraphics(TerminalPosition.TOP_LEFT_CORNER, realTextArea), component);

            //Draw scrollbars, if any
            if(drawVerticalScrollBar) {
                verticalScrollBar.setViewSize(realTextArea.row);
                verticalScrollBar.setScrollMaximum(textBoxLineCount);
                verticalScrollBar.setScrollPosition(viewTopLeft.row);
                verticalScrollBar.draw(graphics.newTextGraphics(
                        new TerminalPosition(graphics.getSize().column - 1, 0),
                        new TerminalPosition(1, graphics.getSize().row - 1)));
            }
            if(drawHorizontalScrollBar) {
                horizontalScrollBar.setViewSize(realTextArea.column);
                horizontalScrollBar.setScrollMaximum(component.longestRow - 1);
                horizontalScrollBar.setScrollPosition(viewTopLeft.column);
                horizontalScrollBar.draw(graphics.newTextGraphics(
                        new TerminalPosition(0, graphics.getSize().row - 1),
                        new TerminalPosition(graphics.getSize().column - 1, 1)));
            }
        }

        private void drawTextArea(TextGUIGraphics graphics, TextBox component) {
            TerminalPosition textAreaSize = graphics.getSize();
            if(viewTopLeft.column + textAreaSize.column > component.longestRow) {
                viewTopLeft = viewTopLeft.withColumn(component.longestRow - textAreaSize.column);
                if(viewTopLeft.column < 0) {
                    viewTopLeft = viewTopLeft.withColumn(0);
                }
            }
            if(viewTopLeft.row + textAreaSize.row > component.getLineCount()) {
                viewTopLeft = viewTopLeft.withRow(component.getLineCount() - textAreaSize.row);
                if(viewTopLeft.row < 0) {
                    viewTopLeft = viewTopLeft.withRow(0);
                }
            }
            if (component.isFocused()) {
                graphics.applyThemeStyle(graphics.getThemeDefinition(TextBox.class).getActive());
            }
            else {
                graphics.applyThemeStyle(graphics.getThemeDefinition(TextBox.class).getNormal());
            }
            graphics.fill(component.unusedSpaceCharacter);

            if(!component.isReadOnly()) {
                //Adjust caret position if necessary
                TerminalPosition caretPosition = component.getCaretPosition();
                String caretLine = component.getLine(caretPosition.row);
                caretPosition = caretPosition.withColumn(Math.min(caretPosition.column, caretLine.length()));

                //Adjust the view if necessary
                int trueColumnPosition = TerminalTextUtils.getColumnIndex(caretLine, caretPosition.column);
                if (trueColumnPosition < viewTopLeft.column) {
                    viewTopLeft = viewTopLeft.withColumn(trueColumnPosition);
                }
                else if (trueColumnPosition >= textAreaSize.column + viewTopLeft.column) {
                    viewTopLeft = viewTopLeft.withColumn(trueColumnPosition - textAreaSize.column + 1);
                }
                if (caretPosition.row < viewTopLeft.row) {
                    viewTopLeft = viewTopLeft.withRow(caretPosition.row);
                }
                else if (caretPosition.row >= textAreaSize.row + viewTopLeft.row) {
                    viewTopLeft = viewTopLeft.withRow(caretPosition.row - textAreaSize.row + 1);
                }

                //Additional corner-case for CJK characters
                if(trueColumnPosition - viewTopLeft.column == graphics.getSize().column - 1) {
                    if(caretLine.length() > caretPosition.column &&
                            TerminalTextUtils.isCharCJK(caretLine.charAt(caretPosition.column))) {
                        viewTopLeft = viewTopLeft.withRelativeColumn(1);
                    }
                }
            }

            for (int row = 0; row < textAreaSize.row; row++) {
                int rowIndex = row + viewTopLeft.row;
                if(rowIndex >= component.lines.size()) {
                    continue;
                }
                String line = component.lines.get(rowIndex);
                if(component.getMask() != null) {
                    StringBuilder builder = new StringBuilder();
                    for(int i = 0; i < line.length(); i++) {
                        builder.append(component.getMask());
                    }
                    line = builder.toString();
                }
                graphics.putString(0, row, TerminalTextUtils.fitString(line, viewTopLeft.column, textAreaSize.column));
            }
        }
    }
}
