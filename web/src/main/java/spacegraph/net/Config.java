package spacegraph.net;

import java.io.IOException;
import java.util.Properties;

/**
 * Manages and gives access to the config and ops file.
 */
public class Config {

	private Properties config;
	private Properties ops;

	/**
	 * Creates a new Config object. Loads from ops.txt and config.txt to be
	 * read. If ops.txt and config.txt do not exist will it create them.
	 * 
	 * @throws Exception
	 */
	public Config() {
//		File config = new File("config.txt");
//		File ops = new File("ops.txt");
//		File info = new File("info.txt");
//		if (!config.exists()) {
//			Utilities.makeFile("config.txt");
//		}
//		if (!ops.exists()) {
//			Utilities.makeFile("ops.txt");
//		}
//		if (!info.exists()) {
//			Utilities.makeFile("info.txt");
//		}
//		this.config = new Properties();
//		this.config.load(new FileReader(config));
//		this.ops = new Properties();
//		this.ops.load(new FileReader(ops));
	}

	/**
	 * Returns the String value of the given key.
	 * 
	 * @param key
	 *            Key of the requested value
	 * @return Value of the given key or null if key does not exist.
	 */
	public static String getProperty(String key) {
//		if (config.getProperty(key) == null) return "";
//		return config.getProperty(key);
		return "";
	}

	/**
	 * Sets a property in the config and saves to disk.
	 * 
	 * @param key
	 *            Key that value will be set to.
	 * @param value
	 *            Value to be set to key.
	 * @throws IOException
	 */
	public void setProperty(String key, String value) {
		config.setProperty(key, value);
		save();
	}

	/**
	 * Checks if password matches given username.
	 * 
	 * @param username
	 *            Username to check.
	 * @param password
	 *            Password to check.
	 * @return True if Username and password combination are correct false if
	 *         not.
	 */
	public boolean checkOpPassword(String username, String password) {
		return password.equals(ops.get(username));
	}

	/**
	 * Adds Op O-Line and saves it to disk.
	 * 
	 * @param username
	 *            Username of Op.
	 * @param password
	 *            Ops password.
	 * @throws IOException
	 */
	public void addOp(String username, String password) {
		ops.setProperty(username, password);
		save();
	}

	private void save() {
//		config.store(new FileWriter(new File("config.txt")), "RBruno's server config.");
//		ops.store(new FileWriter(new File("ops.txt")), "RBruno's op list.");
	}
}
