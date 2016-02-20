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

import java.util.Arrays;
import junit.framework.*;

/** JUnit TestCase class for the different kinds of representations of red.REDText. 
  *
  * @author gerald.czech@scch.at
  * @tier test
  */
public class RTestREDTextRepresentations extends RTestREDText {
	/**
	 * Static method to construct the TestSuite of RTestREDTextFileIO.
	 */
	public static Test suite() {
		return new TestSuite(RTestREDTextRepresentations.class);
	}
	
	/**
	 * Constructor.
	 * Constructs a new RTestREDTextModification object.
	 */
	public RTestREDTextRepresentations(String name) {
		super(name);
	}
	
	/**
	 * Tests the length() method. This method is called by several testXXX()
	 * methods, which rely on the proper function of length().
	 */
	private void ensureLength() {
		REDText text = getTestText();
		assertEquals("Length of text and supposed string are not equal",
				TEXT_CONTENT.length(), text.length());
	}
	
	/**
	 * Tests the charAt() method of REDText.
	 */
	public void testCharAt() {
		REDText text = getTestText();
		byte[] result = TEXT_CONTENT.getBytes();
		int pos = 0;
		
		// ensure that text.length() works, cause the calculated position
		// depends on it
		ensureLength();
		// pos == 0: first character
		assertEquals("Wrong character on first position " + pos, result[pos],
				text.charAt(pos));
		// pos == length() - 1: last character
		pos = text.length() - 1;
		assertEquals("Wrong character on last position " + pos, result[pos],
				text.charAt(pos));
		// pos == length() / 2: character in the middle
		pos = text.length() / 2;
		assertEquals("Wrong character on middle position " + pos, result[pos],
				text.charAt(pos));
		// pos == 1: second character
		pos = 1;
		assertEquals("Wrong character on second position " + pos, result[pos],
				text.charAt(pos));
		// pos == length() - 2: forelast character
		pos = text.length() - 2;
		assertEquals("Wrong character on forelast position " + pos, result[pos],
				text.charAt(pos));
		// pos == -1: negative position (should return first character)
		pos = -1;
		assertEquals("Wrong character on negative position " + pos, result[0],
				text.charAt(pos));
		// pos == length(): after last position (should return '\0')
		pos = text.length();
		assertEquals("Wrong character on position length() " + pos, '\0',
				text.charAt(pos));
		// pos == 0; text = emptyText(): after last position (should return '\0')
		text = getEmptyTestText();
		pos = 0;
		assertEquals("Wrong character in empty text on position " + pos, '\0',
				text.charAt(pos));
	}

	/**
	 * Tests the asString() method of REDText.
	 */
	public void testAsString() {
		REDText text = getTestText();
		int from = 0;
		int to = text.length();

		// ensure that text.length() works, cause the calculated position
		// depends on it		
		ensureLength();		
		// test parameterless method: whole String
		assertEquals("String representation doesn't equal supposed String ",
				TEXT_CONTENT, text.asString());
		// 0 ... length(): whole String
		assertEquals("String representation doesn't equal supposed String ",
				TEXT_CONTENT, text.asString(from, to));
		// 0 ... length() / 2: first half of String
		to = text.length() / 2;
		assertEquals("First half of String representation doesn't equal supposed String ",
				TEXT_CONTENT.substring(from, to), text.asString(from, to));
		// length() / 2 ... length(): second half of String
		from = text.length() / 2;
		to = text.length();
		assertEquals("Second half of String representation doesn't equal supposed String ",
				TEXT_CONTENT.substring(from, to), text.asString(from, to));
		// length() / 3 ... length() / 3 * 2: middle part of String
		from = text.length() / 3;
		to = text.length() / 3 * 2;
		assertEquals("Second half of String representation doesn't equal supposed String ",
				TEXT_CONTENT.substring(from, to), text.asString(from, to));
		// 1 ... length(): cut first character
		from = 1;
		to = text.length();
		assertEquals("Cutting first character doesn't return supposed String ",
				TEXT_CONTENT.substring(from, to), text.asString(from, to));
		// 0 ... length() - 1: cut last character
		from = 0;
		to = text.length() - 1;
		assertEquals("Cutting last character doesn't return supposed String ",
				TEXT_CONTENT.substring(from, to), text.asString(from, to));
		// -1 ... length(): whole String; index should be normalized to 0
		from = - 1;
		to = text.length();
		assertEquals("Negative index doesn't return supposed String ",
				TEXT_CONTENT, text.asString(from, to));
		// 0 ... length() + 1: whole String; index should be normalized to length()
		from = 0;
		to = text.length() + 1;
		assertEquals("To big length value doesn't return supposed String ",
				TEXT_CONTENT, text.asString(from, to));
		// test parameterless method on emptyText(): should return ""
		text = getEmptyTestText();
		assertEquals("Empty text did not return empty String", "",
				text.asString());
	}

	/**
	 * This method implements an assert statement with the given assert
	 * message testing the equality of the text and the supposed result as
	 * bytes in the given range.
	 *
	 * @param assertMsg the message for the assert statement.
	 * @param from the lower index of the range to test.
	 * @param to the upper index of the range to test.
	 */
	private void assertTextBytes(String assertMsg, int from, int to) {
		REDText text = getTestText();
		byte[] result;
		byte[] textBytes = text.asBytes(from, to, null);
		int resFrom;
		int resTo;
		
		// ensure from is in the index bounds of the result string
		if (from >= 0 && from <= TEXT_CONTENT.length()) {
			resFrom = from;
		}
		else {
			resFrom = 0;
		}

		// ensure to is in the index bounds of the result string
		if (to >= 0 && to <= TEXT_CONTENT.length()) {
			resTo = to;
		}
		else {
			resTo = TEXT_CONTENT.length();
		}
		result = TEXT_CONTENT.substring(resFrom, resTo).getBytes();
		assertTrue(assertMsg, Arrays.equals(result, textBytes));
		assertEquals("Actual number of bytes differs from supposed number of bytes",
				result.length, textBytes.length);
	}

	/**
	 * Tests the asBytes() method of REDText.
	 */
	public void testAsBytes() {
		REDText text = getTestText();
		int from = 0;
		int to = text.length();
		byte[] textBytes;	// array to store the return value from asBytes()
		byte[] result;		// array to store the supposed result
		
		// length is needed for position calculations
		ensureLength();
		// 0 ... length(): whole text
		assertTextBytes("Text bytes and proposed result bytes don't equal ",
				from, to);
		// 0 ... length() / 2: first half of text bytes
		to = text.length() / 2;
		assertTextBytes("First half of text bytes representation doesn't equal supposed text bytes ",
				from, to);
		// length() / 2 ... length(): second half of text bytes
		from = text.length() / 2;
		to = text.length();
		assertTextBytes("Second half of text bytes representation doesn't equal supposed text bytes ",
				from, to);
		// length() / 3 ... length() / 3 * 2: middle part of text bytes
		from = text.length() / 3;
		to = text.length() / 3 * 2;
		assertTextBytes("Second half of text bytes representation doesn't equal supposed text bytes ",
				from, to);
		// 1 ... length(): cut first character
		from = 1;
		to = text.length();
		assertTextBytes("Cutting first character doesn't return supposed text bytes ",
				from, to);
		// 0 ... length() - 1: cut last character
		from = 0;
		to = text.length() - 1;
		assertTextBytes("Cutting last character doesn't return supposed text bytes ",
				from, to);
		// -1 ... length(): whole text bytes; index should be normalized to 0
		from = - 1;
		to = text.length();
		assertTextBytes("Negative index doesn't return supposed text bytes ",
				from, to);
		// 0 ... length() + 1: whole text bytes; index should be normalized to length()
		from = 0;
		to = text.length() + 1;
		assertTextBytes("To big length value doesn't return supposed text bytes ",
				from, to);
		// Insert in a longer byte array.
		textBytes = new byte[text.length()];
		result = new byte[text.length()];
		Arrays.fill(textBytes, (byte)32);	// initialize arrays with blanks
		Arrays.fill(result, (byte)32);
		from = 0;
		to = text.length() / 2;
		textBytes = text.asBytes(from, to, textBytes);
		result = copyInto(TEXT_CONTENT.substring(from, to).getBytes(), result);
		assertTrue("Insert in a longer byte array failed ",
				Arrays.equals(result, textBytes));
		assertEquals("Predefined byte array has wrong length after asBytes() ",
				result.length, textBytes.length);
		// Insert in a byte array with equal length.
		Arrays.fill(textBytes, (byte)32);	// initialize arrays with blanks
		Arrays.fill(result, (byte)32);
		from = 0;
		to = text.length();
		textBytes = text.asBytes(from, to, textBytes);
		assertTrue("Insert in a byte array with equal length failed ",
				Arrays.equals(TEXT_CONTENT.getBytes(), textBytes));
		assertEquals("Predefined byte array has wrong length after asBytes() ",
				result.length, textBytes.length);
		// Insert in a byte array with shorter length.
		textBytes = new byte[text.length() / 2];
		result = new byte[text.length()];
		Arrays.fill(textBytes, (byte)32);	// initialize arrays with blanks
		Arrays.fill(result, (byte)32);
		from = 0;
		to = text.length();
		textBytes = text.asBytes(from, to, textBytes);
		assertTrue("Insert in a byte array with shorter length failed ",
				Arrays.equals(TEXT_CONTENT.getBytes(), textBytes));
		assertEquals("Predefined byte array has wrong length after asBytes() ",
				result.length, textBytes.length);
		// 0 ... length() of empty text: whole text
		text = getEmptyTestText();
		from = 0;
		to = text.length();
		textBytes = text.asBytes(from, to, null);
		assertEquals("Empty Text returned no empty byte array", null,
				textBytes);
	}
	
	/**
	 * Method to copy a given byte array into another, predefined byte array.
	 *
	 * @param src the source byte array to copy.
	 * @param dest the destination byte array to store the contents of src. If
	 * the size of the dest is smaller than the size of src, dest is initialized
	 * with the same size as src.
	 * @return the destination byte array holding the contents of src.
	 */
	private static byte[] copyInto(byte[] src, byte[] dest) {
		// Ensure that dest has at least the size of src.
		if (dest.length < src.length) {
			dest = new byte[src.length];
		}
		// copy bytewise
		System.arraycopy(src, 0, dest, 0, src.length);
		return dest;
	}
	
	public void testGetLine() {
		char [] arr = null;
		char [] arr2 = null;
		REDText text = getTestText();
		arr = text.getLine(0, arr);
		assertEquals("Don't meddle in the affairs of wizards,\n", new String(arr, 0, text.getLineLength(0)));
		arr2 = text.getLine(1, arr);
		assertSame("getLine does not reuse array", arr2, arr);
		assertEquals("for they are subtle and quick to anger.\n", new String(arr, 0, text.getLineLength(1)));
		arr = text.getLine(2, arr);
		assertEquals("\n", new String(arr, 0, text.getLineLength(2)));
		arr = text.getLine(3, arr);
		assertEquals("The Lord of the Rings.\n", new String(arr, 0, text.getLineLength(3)));
		arr = text.getLine(4, arr);
		assertEquals("", new String(arr, 0, text.getLineLength(4)));
		arr = text.getLine(5, arr);
		assertSame(arr, null);		
		
		arr = text.getLine(1, null);
		assertEquals("for they are subtle and quick to anger.\n", new String(arr, 0, text.getLineLength(1)));
	}
}
