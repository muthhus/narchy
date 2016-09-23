package com.rbruno.irc.plugin;

import java.io.*;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;

import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

/**
 * Starts and reads plugins.
 */
public class PluginManager {

	private final ArrayList<Plugin> plugins = new ArrayList<>();

	/**
	 * Looks in plugins folder for jar files and tries to run them.
	 * 
	 * @throws IOException
	 */
	public PluginManager() throws IOException {
		File pluginFolder = new File("plugins/");
		if (!pluginFolder.exists()) pluginFolder.mkdir();

		for (String path : pluginFolder.list()) {
			if (!path.endsWith(".jar")) continue;
			File jar = new File("plugins/" + path);

			URLClassLoader loader = new URLClassLoader(new URL[] { jar.toURI().toURL() });
			URL config = loader.findResource("plugin.txt");
			if (config == null) {
				Logger.log(path + " was not loaded becuase plugin.txt was not found.", Level.FINE);
				continue;
			}
			BufferedReader inputStream = new BufferedReader(new InputStreamReader(loader.getResourceAsStream("plugin.txt")));
			String main = null;
			String name = null;
			while (inputStream.ready()) {
				String line = inputStream.readLine();
				if (line.startsWith("main") && line.split("=").length > 1) main = line.split("=")[1];
				if (line.startsWith("name") && line.split("=").length > 1) name = line.split("=")[1];
			}
			if (main == null) {
				Logger.log(path + " was not loaded because no main class was found.", Level.FINE);
				continue;
			}
			if (name == null) {
				Logger.log(path + " was not loaded because no plugin name was found.", Level.FINE);
				continue;
			}
			if (this.getPlugin(name) != null) {
				Logger.log(path + " was not loaded because a plugin with the same name is already loaded.", Level.FINE);
				continue;
			}
			Class<?> mainClass;
			try {
				mainClass = loader.loadClass(main);
			} catch (ClassNotFoundException ignored) {
				Logger.log(path + " was not loaded because there was a problem loading your main class.", Level.FINE);
				continue;
			}
			Plugin plugin = null;
			try {
				plugin = (Plugin) mainClass.newInstance();
			} catch (ClassCastException ignored) {
				Logger.log(path + " was not loaded because your main class does not extend Plugin.", Level.FINE);
				continue;
			} catch (InstantiationException ignored) {
				Logger.log(path + " was not loaded because your main class does not extend Plugin.", Level.FINE);
				continue;
			} catch (IllegalAccessException ignored) {
				Logger.log(path + " was not loaded because your main class does not extend Plugin.", Level.FINE);
				continue;
			}
			if (plugin == null) {
				Logger.log(path + " was not loaded because there was a problem loading your main class.", Level.FINE);
				continue;
			}
			this.add(plugin, name);
			loader.close();

		}
	}

	private void add(Plugin plugin, String name) {
		plugin.setName(name);
		plugin.onEnable();
		plugins.add(plugin);
	}

	public Plugin getPlugin(String name) {
		for (Plugin current : getPlugins())
			if (current.getName().equals(name)) return current;
		return null;
	}

	public void runOnRequest(Request request) {
		for (Plugin current : getPlugins()) {
			try {
				current.onRequest(request);
			} catch (Exception e) {
				Logger.log("An error occured while passing a request to a plugin.", Level.FINE);
				e.printStackTrace();
			}
		}
	}

	public ArrayList<Plugin> getPlugins() {
		return new ArrayList<>(plugins);
	}

	public void runOnClientLogin(Client client) {
		for (Plugin current : getPlugins()) {
			try {
				current.onClientLogin(client);
			} catch (Exception e) {
				Logger.log("An error occured while passing a new client to a plugin.", Level.FINE);
				e.printStackTrace();
			}
		}		
	}

	private static class Logger {

		/**
		 * Logs a message to the console and the log file.
		 *
		 * @param message
		 *            The message in which needs to be logged.
		 * @param level
		 *            The level of the log.
		 */
		public static void log(String message, Level level) {

			Date date = new Date();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd H:mm:ss ");
			String formattedDate = sdf.format(date);
			String formattedMessage = formattedDate + '[' + level.toString() + "] " + message;
			System.out.println(formattedMessage);
			try {
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("log.txt", true)));
				out.println(formattedMessage);
				out.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		/**
		 * Logs a message with the console and the log file. Defaults to using the
		 * INFO level.
		 *
		 * @param string
		 *            The message in which needs to be logged.
		 */
		public static void log(String string) {
			log(string, Level.INFO);
		}

	}

}
