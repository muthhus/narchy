//    RED - A Java Editor Library
//    Copyright (C) 2003  Robert Lichtenberger
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
package red;

import junit.framework.*;
import java.awt.event.*;

/** Test case for REDViewReadonlyController.
  * @author rli@chello.at
  * @tier test
  */
public class RTestREDViewReadonlyController extends RTestControllerBase {
	
	public RTestREDViewReadonlyController(String name) {
		super(name);
	}

    protected void send(int keyCode, char keyChar, int modifiers) {
		super.send(keyCode, keyChar, modifiers);
        assertTrue(!fEditor.isModified());
	}

    public void setUp() throws Exception {   
        super.setUp();
        fEditor.setController(new REDViewReadonlyController(fEditor.getController()));
    }
	
	/** test immutability of editor against defined key events */
	public void testInmutability() {
		String oriContent = fEditor.asString();
		// lowercase letters
		for (char i = 'a'; i <= 'z'; i++) {
			send(0, i, 0);
		}
		// uppercase letters
		for (char i = 'A'; i <= 'Z'; i++) {
			send(0, i, InputEvent.SHIFT_MASK);
		}
		// some other letters
		send(0, '!'); send(0, '"'); send(0, '§'); send(0, '$'); send(0, '%'); send(0, '&'); send(0, '/'); send(0, '(');
		send(0, ')'); send(0, '='); send(0, '?'); send(0, '´'); send(0, '`'); send(0, '{'); send(0, '}'); send(0, '['); 
		send(0, ']'); send(0, '\\'); send(0, '@'); send(0, '^'); send(0, '°'); send(0, '<'); send(0, '>'); send(0, '|');
		send(0, '~'); send(0, '+'); send(0, '-'); send(0, '*'); send(0, '#'); send(0, '\''); send(0, '.'); send(0, ':');
		send(0, ','); send(0, ';'); send(0, '\n'); send(0, '\b'); send(0, '\r');

		// try all vk - combinations; this is kind of a brute force attack		
		tryWithAllModifiers(KeyEvent.VK_ESCAPE); 
		tryWithAllModifiers(KeyEvent.VK_ENTER); 
		tryWithAllModifiers(KeyEvent.VK_BACK_SPACE); 
		tryWithAllModifiers(KeyEvent.VK_TAB); 
		tryWithAllModifiers(KeyEvent.VK_CANCEL); 
		tryWithAllModifiers(KeyEvent.VK_CLEAR); 
		tryWithAllModifiers(KeyEvent.VK_SHIFT); 
		tryWithAllModifiers(KeyEvent.VK_CONTROL); 
		tryWithAllModifiers(KeyEvent.VK_ALT); 
		tryWithAllModifiers(KeyEvent.VK_PAUSE); 
		tryWithAllModifiers(KeyEvent.VK_CAPS_LOCK); 
		tryWithAllModifiers(KeyEvent.VK_ESCAPE); 
		tryWithAllModifiers(KeyEvent.VK_SPACE); 
		tryWithAllModifiers(KeyEvent.VK_PAGE_UP); 
		tryWithAllModifiers(KeyEvent.VK_PAGE_DOWN); 
		tryWithAllModifiers(KeyEvent.VK_END); 
		tryWithAllModifiers(KeyEvent.VK_HOME); 
		tryWithAllModifiers(KeyEvent.VK_LEFT); 
		tryWithAllModifiers(KeyEvent.VK_UP); 
		tryWithAllModifiers(KeyEvent.VK_RIGHT); 
		tryWithAllModifiers(KeyEvent.VK_DOWN); 
		tryWithAllModifiers(KeyEvent.VK_COMMA); 
		tryWithAllModifiers(KeyEvent.VK_MINUS); 
		tryWithAllModifiers(KeyEvent.VK_PERIOD); 
		tryWithAllModifiers(KeyEvent.VK_SLASH); 
		tryWithAllModifiers(KeyEvent.VK_0); 
		tryWithAllModifiers(KeyEvent.VK_1); 
		tryWithAllModifiers(KeyEvent.VK_2); 
		tryWithAllModifiers(KeyEvent.VK_3); 
		tryWithAllModifiers(KeyEvent.VK_4); 
		tryWithAllModifiers(KeyEvent.VK_5); 
		tryWithAllModifiers(KeyEvent.VK_6); 
		tryWithAllModifiers(KeyEvent.VK_7); 
		tryWithAllModifiers(KeyEvent.VK_8); 
		tryWithAllModifiers(KeyEvent.VK_9); 
		tryWithAllModifiers(KeyEvent.VK_SEMICOLON); 
		tryWithAllModifiers(KeyEvent.VK_EQUALS); 
		tryWithAllModifiers(KeyEvent.VK_A); 
		tryWithAllModifiers(KeyEvent.VK_B); 
		tryWithAllModifiers(KeyEvent.VK_C); 
		tryWithAllModifiers(KeyEvent.VK_D); 
		tryWithAllModifiers(KeyEvent.VK_E); 
		tryWithAllModifiers(KeyEvent.VK_F); 
		tryWithAllModifiers(KeyEvent.VK_G); 
		tryWithAllModifiers(KeyEvent.VK_H); 
		tryWithAllModifiers(KeyEvent.VK_I); 
		tryWithAllModifiers(KeyEvent.VK_J); 
		tryWithAllModifiers(KeyEvent.VK_K); 
		tryWithAllModifiers(KeyEvent.VK_L); 
		tryWithAllModifiers(KeyEvent.VK_M); 
		tryWithAllModifiers(KeyEvent.VK_N); 
		tryWithAllModifiers(KeyEvent.VK_O); 
		tryWithAllModifiers(KeyEvent.VK_P); 
		tryWithAllModifiers(KeyEvent.VK_Q); 
		tryWithAllModifiers(KeyEvent.VK_R); 
		tryWithAllModifiers(KeyEvent.VK_S); 
		tryWithAllModifiers(KeyEvent.VK_T); 
		tryWithAllModifiers(KeyEvent.VK_U); 
		tryWithAllModifiers(KeyEvent.VK_V); 
		tryWithAllModifiers(KeyEvent.VK_W); 
		tryWithAllModifiers(KeyEvent.VK_X); 
		tryWithAllModifiers(KeyEvent.VK_Y); 
		tryWithAllModifiers(KeyEvent.VK_Z); 
		tryWithAllModifiers(KeyEvent.VK_OPEN_BRACKET); 
		tryWithAllModifiers(KeyEvent.VK_BACK_SLASH); 
		tryWithAllModifiers(KeyEvent.VK_CLOSE_BRACKET); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD0); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD1); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD2); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD3); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD4); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD5); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD6); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD7); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD8); 
		tryWithAllModifiers(KeyEvent.VK_NUMPAD9); 
		tryWithAllModifiers(KeyEvent.VK_MULTIPLY); 
		tryWithAllModifiers(KeyEvent.VK_ADD); 
		tryWithAllModifiers(KeyEvent.VK_SEPARATER); 
		tryWithAllModifiers(KeyEvent.VK_SUBTRACT); 
		tryWithAllModifiers(KeyEvent.VK_DECIMAL); 
		tryWithAllModifiers(KeyEvent.VK_DIVIDE); 
		tryWithAllModifiers(KeyEvent.VK_DELETE);  /* ASCII DEL */
		tryWithAllModifiers(KeyEvent.VK_NUM_LOCK); 
		tryWithAllModifiers(KeyEvent.VK_SCROLL_LOCK); 
		tryWithAllModifiers(KeyEvent.VK_F1); 
		tryWithAllModifiers(KeyEvent.VK_F2); 
		tryWithAllModifiers(KeyEvent.VK_F3); 
		tryWithAllModifiers(KeyEvent.VK_F4); 
		tryWithAllModifiers(KeyEvent.VK_F5); 
		tryWithAllModifiers(KeyEvent.VK_F6); 
		tryWithAllModifiers(KeyEvent.VK_F7); 
		tryWithAllModifiers(KeyEvent.VK_F8); 
		tryWithAllModifiers(KeyEvent.VK_F9); 
		tryWithAllModifiers(KeyEvent.VK_F10); 
		tryWithAllModifiers(KeyEvent.VK_F11); 
		tryWithAllModifiers(KeyEvent.VK_F12); 
		tryWithAllModifiers(KeyEvent.VK_F13); 
		tryWithAllModifiers(KeyEvent.VK_F14); 
		tryWithAllModifiers(KeyEvent.VK_F15); 
		tryWithAllModifiers(KeyEvent.VK_F16); 
		tryWithAllModifiers(KeyEvent.VK_F17); 
		tryWithAllModifiers(KeyEvent.VK_F18); 
		tryWithAllModifiers(KeyEvent.VK_F19); 
		tryWithAllModifiers(KeyEvent.VK_F20); 
		tryWithAllModifiers(KeyEvent.VK_F21); 
		tryWithAllModifiers(KeyEvent.VK_F22); 
		tryWithAllModifiers(KeyEvent.VK_F23); 
		tryWithAllModifiers(KeyEvent.VK_F24); 
		tryWithAllModifiers(KeyEvent.VK_PRINTSCREEN); 
		tryWithAllModifiers(KeyEvent.VK_INSERT); 
		tryWithAllModifiers(KeyEvent.VK_HELP); 
		tryWithAllModifiers(KeyEvent.VK_META); 
		tryWithAllModifiers(KeyEvent.VK_BACK_QUOTE); 
		tryWithAllModifiers(KeyEvent.VK_QUOTE); 
		tryWithAllModifiers(KeyEvent.VK_KP_UP); 
		tryWithAllModifiers(KeyEvent.VK_KP_DOWN); 
		tryWithAllModifiers(KeyEvent.VK_KP_LEFT); 
		tryWithAllModifiers(KeyEvent.VK_KP_RIGHT); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_GRAVE); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_ACUTE); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_CIRCUMFLEX); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_TILDE); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_MACRON); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_BREVE); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_ABOVEDOT); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_DIAERESIS); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_ABOVERING); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_DOUBLEACUTE); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_CARON); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_CEDILLA); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_OGONEK); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_IOTA); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_VOICED_SOUND); 
		tryWithAllModifiers(KeyEvent.VK_DEAD_SEMIVOICED_SOUND); 
		tryWithAllModifiers(KeyEvent.VK_AMPERSAND); 
		tryWithAllModifiers(KeyEvent.VK_ASTERISK); 
		tryWithAllModifiers(KeyEvent.VK_QUOTEDBL); 
		tryWithAllModifiers(KeyEvent.VK_LESS); 
		tryWithAllModifiers(KeyEvent.VK_GREATER); 
		tryWithAllModifiers(KeyEvent.VK_BRACELEFT); 
		tryWithAllModifiers(KeyEvent.VK_BRACERIGHT); 
		tryWithAllModifiers(KeyEvent.VK_AT); 
		tryWithAllModifiers(KeyEvent.VK_COLON); 
		tryWithAllModifiers(KeyEvent.VK_CIRCUMFLEX); 
		tryWithAllModifiers(KeyEvent.VK_DOLLAR); 
		tryWithAllModifiers(KeyEvent.VK_EURO_SIGN); 
		tryWithAllModifiers(KeyEvent.VK_EXCLAMATION_MARK); 
		tryWithAllModifiers(KeyEvent.VK_INVERTED_EXCLAMATION_MARK); 
		tryWithAllModifiers(KeyEvent.VK_LEFT_PARENTHESIS); 
		tryWithAllModifiers(KeyEvent.VK_NUMBER_SIGN); 
		tryWithAllModifiers(KeyEvent.VK_PLUS); 
		tryWithAllModifiers(KeyEvent.VK_RIGHT_PARENTHESIS); 
		tryWithAllModifiers(KeyEvent.VK_UNDERSCORE); 
		tryWithAllModifiers(KeyEvent.VK_FINAL); 
		tryWithAllModifiers(KeyEvent.VK_CONVERT); 
		tryWithAllModifiers(KeyEvent.VK_NONCONVERT); 
		tryWithAllModifiers(KeyEvent.VK_ACCEPT); 
		tryWithAllModifiers(KeyEvent.VK_MODECHANGE); 
		tryWithAllModifiers(KeyEvent.VK_KANA); 
		tryWithAllModifiers(KeyEvent.VK_KANJI); 
		tryWithAllModifiers(KeyEvent.VK_ALPHANUMERIC); 
		tryWithAllModifiers(KeyEvent.VK_KATAKANA); 
		tryWithAllModifiers(KeyEvent.VK_HIRAGANA); 
		tryWithAllModifiers(KeyEvent.VK_FULL_WIDTH); 
		tryWithAllModifiers(KeyEvent.VK_HALF_WIDTH); 
		tryWithAllModifiers(KeyEvent.VK_ROMAN_CHARACTERS); 
		tryWithAllModifiers(KeyEvent.VK_ALL_CANDIDATES); 
		tryWithAllModifiers(KeyEvent.VK_PREVIOUS_CANDIDATE); 
		tryWithAllModifiers(KeyEvent.VK_CODE_INPUT); 
		tryWithAllModifiers(KeyEvent.VK_JAPANESE_KATAKANA); 
		tryWithAllModifiers(KeyEvent.VK_JAPANESE_HIRAGANA); 
		tryWithAllModifiers(KeyEvent.VK_JAPANESE_ROMAN); 
		tryWithAllModifiers(KeyEvent.VK_KANA_LOCK); 
		tryWithAllModifiers(KeyEvent.VK_INPUT_METHOD_ON_OFF); 
		tryWithAllModifiers(KeyEvent.VK_CUT); 
		tryWithAllModifiers(KeyEvent.VK_COPY); 
		tryWithAllModifiers(KeyEvent.VK_PASTE); 
		tryWithAllModifiers(KeyEvent.VK_UNDO); 
		tryWithAllModifiers(KeyEvent.VK_AGAIN); 
		tryWithAllModifiers(KeyEvent.VK_FIND); 
		tryWithAllModifiers(KeyEvent.VK_PROPS); 
		tryWithAllModifiers(KeyEvent.VK_STOP); 
		tryWithAllModifiers(KeyEvent.VK_COMPOSE); 
		tryWithAllModifiers(KeyEvent.VK_ALT_GRAPH); 
		
		assertEquals(oriContent, fEditor.asString());
	}

	public static Test suite() {
		return new TestSuite(RTestREDViewReadonlyController.class);
	}
}