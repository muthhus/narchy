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

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Objects;

/**
 * Represents a single character with additional metadata such as colors and modifiers. This class is immutable and
 * cannot be modified after creation.
 * @author Martin
 */
public class TextCharacter {
    private static EnumSet<SGR> toEnumSet(SGR... modifiers) {
        if(modifiers.length == 0) {
            return EnumSet.noneOf(SGR.class);
        }
        else {
            return EnumSet.copyOf(Arrays.asList(modifiers));
        }
    }

    public static final TextCharacter DEFAULT_CHARACTER = new TextCharacter(' ', TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT);

    public final char c;
    public final TextColor fore;
    public final TextColor back;
    private final EnumSet<SGR> modifiers;  //This isn't immutable, but we should treat it as such and not expose it!

    /**
     * Creates a {@code ScreenCharacter} based on a supplied character, with default colors and no extra modifiers.
     * @param c Physical character to use
     */
    public TextCharacter(char c) {
        this(c, TextColor.ANSI.DEFAULT, TextColor.ANSI.DEFAULT);
    }
    
    /**
     * Copies another {@code ScreenCharacter}
     * @param c screenCharacter to copy from
     */
    public TextCharacter(TextCharacter c) {
        this(c.c,
                c.fore,
                c.back,
                c.getModifiers().toArray(new SGR[c.getModifiers().size()]));
    }

    /**
     * Creates a new {@code ScreenCharacter} based on a physical character, color information and optional modifiers.
     * @param c Physical character to refer to
     * @param foregroundColor Foreground color the character has
     * @param backgroundColor Background color the character has
     * @param styles Optional list of modifiers to apply when drawing the character
     */
    @SuppressWarnings("WeakerAccess")
    public TextCharacter(
            char c,
            TextColor foregroundColor,
            TextColor backgroundColor,
            SGR... styles) {
        
        this(c,
                foregroundColor, 
                backgroundColor, 
                toEnumSet(styles));
    }

    /**
     * Creates a new {@code ScreenCharacter} based on a physical character, color information and a set of modifiers.
     * @param c Physical character to refer to
     * @param foregroundColor Foreground color the character has
     * @param backgroundColor Background color the character has
     * @param modifiers Set of modifiers to apply when drawing the character
     */
    public TextCharacter(
            char c,
            TextColor foregroundColor,
            TextColor backgroundColor,
            EnumSet<SGR> modifiers) {

        // Don't allow creating a TextCharacter containing a control character
        // For backward-compatibility, do allow tab for now
        // TODO: In lanterna 3.1, don't allow tab
//        if(TerminalTextUtils.isControlCharacter(c) && c != '\t') {
//            throw new IllegalArgumentException("Cannot create a TextCharacter from a control character (0x" + Integer.toHexString(c) + ')');
//        }

        if(foregroundColor == null) {
            foregroundColor = TextColor.ANSI.DEFAULT;
        }
        if(backgroundColor == null) {
            backgroundColor = TextColor.ANSI.DEFAULT;
        }

        this.c = c;
        this.fore = foregroundColor;
        this.back = backgroundColor;
        this.modifiers = EnumSet.copyOf(modifiers);
    }

    /**
     * Returns a set of all active modifiers on this TextCharacter
     * @return Set of active SGR codes
     */
    public EnumSet<SGR> getModifiers() {
        return EnumSet.copyOf(modifiers);
    }

    /**
     * Returns true if this TextCharacter has the bold modifier active
     * @return {@code true} if this TextCharacter has the bold modifier active
     */
    public boolean isBold() {
        return modifiers.contains(SGR.BOLD);
    }

    /**
     * Returns true if this TextCharacter has the reverse modifier active
     * @return {@code true} if this TextCharacter has the reverse modifier active
     */
    public boolean isReversed() {
        return modifiers.contains(SGR.REVERSE);
    }

    /**
     * Returns true if this TextCharacter has the underline modifier active
     * @return {@code true} if this TextCharacter has the underline modifier active
     */
    public boolean isUnderlined() {
        return modifiers.contains(SGR.UNDERLINE);
    }

    /**
     * Returns true if this TextCharacter has the blink modifier active
     * @return {@code true} if this TextCharacter has the blink modifier active
     */
    public boolean isBlinking() {
        return modifiers.contains(SGR.BLINK);
    }

    /**
     * Returns true if this TextCharacter has the bordered modifier active
     * @return {@code true} if this TextCharacter has the bordered modifier active
     */
    public boolean isBordered() {
        return modifiers.contains(SGR.BORDERED);
    }

    /**
     * Returns true if this TextCharacter has the crossed-out modifier active
     * @return {@code true} if this TextCharacter has the crossed-out modifier active
     */
    public boolean isCrossedOut() {
        return modifiers.contains(SGR.CROSSED_OUT);
    }

    /**
     * Returns a new TextCharacter with the same colors and modifiers but a different underlying character
     * @param character Character the copy should have
     * @return Copy of this TextCharacter with different underlying character
     */
    @SuppressWarnings("SameParameterValue")
    public TextCharacter withCharacter(char character) {
        if(this.c == character) {
            return this;
        }
        return new TextCharacter(character, fore, back, modifiers);
    }

    /**
     * Returns a copy of this TextCharacter with a specified foreground color
     * @param foregroundColor Foreground color the copy should have
     * @return Copy of the TextCharacter with a different foreground color
     */
    public TextCharacter withForegroundColor(TextColor foregroundColor) {
        if(this.fore == foregroundColor || this.fore.equals(foregroundColor)) {
            return this;
        }
        return new TextCharacter(c, foregroundColor, back, modifiers);
    }

    /**
     * Returns a copy of this TextCharacter with a specified background color
     * @param backgroundColor Background color the copy should have
     * @return Copy of the TextCharacter with a different background color
     */
    public TextCharacter withBackgroundColor(TextColor backgroundColor) {
        if(this.back == backgroundColor || this.back.equals(backgroundColor)) {
            return this;
        }
        return new TextCharacter(c, fore, backgroundColor, modifiers);
    }

    /**
     * Returns a copy of this TextCharacter with specified list of SGR modifiers. None of the currently active SGR codes
     * will be carried over to the copy, only those in the passed in value.
     * @param modifiers SGR modifiers the copy should have
     * @return Copy of the TextCharacter with a different set of SGR modifiers
     */
    public TextCharacter withModifiers(Collection<SGR> modifiers) {
        EnumSet<SGR> newSet = EnumSet.copyOf(modifiers);
        if(modifiers.equals(newSet)) {
            return this;
        }
        return new TextCharacter(c, fore, back, newSet);
    }

    /**
     * Returns a copy of this TextCharacter with an additional SGR modifier. All of the currently active SGR codes
     * will be carried over to the copy, in addition to the one specified.
     * @param modifier SGR modifiers the copy should have in additional to all currently present
     * @return Copy of the TextCharacter with a new SGR modifier
     */
    public TextCharacter withModifier(SGR modifier) {
        if(modifiers.contains(modifier)) {
            return this;
        }
        EnumSet<SGR> newSet = EnumSet.copyOf(this.modifiers);
        newSet.add(modifier);
        return new TextCharacter(c, fore, back, newSet);
    }

    /**
     * Returns a copy of this TextCharacter with an SGR modifier removed. All of the currently active SGR codes
     * will be carried over to the copy, except for the one specified. If the current TextCharacter doesn't have the
     * SGR specified, it will return itself.
     * @param modifier SGR modifiers the copy should not have
     * @return Copy of the TextCharacter without the SGR modifier
     */
    public TextCharacter withoutModifier(SGR modifier) {
        if(!modifiers.contains(modifier)) {
            return this;
        }
        EnumSet<SGR> newSet = EnumSet.copyOf(this.modifiers);
        newSet.remove(modifier);
        return new TextCharacter(c, fore, back, newSet);
    }

    public boolean isDoubleWidth() {
        return TerminalTextUtils.isCharDoubleWidth(c);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final TextCharacter other = (TextCharacter) obj;
        if(this.c != other.c) {
            return false;
        }
        if(!Objects.equals(fore, fore)) {
            return false;
        }
        if(!Objects.equals(back, back)) {
            return false;
        }
        return !(!Objects.equals(modifiers, modifiers));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 37 * hash + this.c;
        hash = 37 * hash + (this.fore != null ? this.fore.hashCode() : 0);
        hash = 37 * hash + (this.back != null ? this.back.hashCode() : 0);
        hash = 37 * hash + (this.modifiers != null ? this.modifiers.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return "TextCharacter{" + "character=" + c + ", foregroundColor=" + fore + ", backgroundColor=" + back + ", modifiers=" + modifiers + '}';
    }
}
