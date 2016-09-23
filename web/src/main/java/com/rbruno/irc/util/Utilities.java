package com.rbruno.irc.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;

/**
 * Utilities for any use.
 */
public class Utilities {

	/**
	 * Reads the file and returns a List of all the lines in the file.
	 * 
	 * @param fileName
	 *            The path to the file.
	 * @return A List of all the lines in the file.
	 * @throws IOException
	 */
	public static List<String> read(String fileName) throws IOException {
		return Files.readAllLines(new File(fileName).toPath(), Charset.defaultCharset());
	}

	/**
	 * Copies file inside the current jar to outside.
	 * 
	 * @param fileName
	 *            The name of the file inside the jar and destination file.
	 * @throws IOException
	 */
//	public static void makeFile(String fileName) throws IOException {
//		InputStream inputStream = Utilities.class.getResourceAsStream("/" + fileName);
//		Files.copy(inputStream, new File(fileName).toPath(), StandardCopyOption.REPLACE_EXISTING);
//	}

}
