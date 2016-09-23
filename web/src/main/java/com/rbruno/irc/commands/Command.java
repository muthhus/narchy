package com.rbruno.irc.commands;

import java.util.HashMap;
import java.util.Map;

import com.rbruno.irc.Request;

/**
 * A command that can be called when requested by a client. Also statically hold
 * all commands in an array.
 */
abstract public class Command {

	public final String id;
	public final int arity;


	/**
	 * Creates a new Command object.
	 * 
	 * @param command
	 *            Name of the Command. Not cap sensitive.
	 * @param arity
	 *            Required number of parameters.
	 */
	public Command(String command, int arity) {
		this.id = command;
		this.arity = arity;
	}

	/**
	 * Override this.
	 * 
	 * @param request
	 *            Request that was sent.
	 * @throws Exception
	 */
	abstract public void run(Request request) throws Exception;


	public static Command the(String command) {
		return commands.get(command);
	}


	private static final Map<String, Command> commands = new HashMap(32);

	private static void addCommand(Command c) {
		commands.put(c.id, c);
	}

	public static void init() {
		addCommand(new Pass());
		addCommand(new Nick());
		addCommand(new User());
		// addCommand(new Server());
		addCommand(new Oper());
		addCommand(new Quit());
		// addCommand(new Squit());
		addCommand(new Join());
		addCommand(new Part());
		addCommand(new Mode());
		addCommand(new Topic());
		addCommand(new Names());
		addCommand(new List());
		addCommand(new Invite());
		addCommand(new Kick());
		addCommand(new Version());
		// addCommand(new Stats());
		// addCommand(new Links());
		addCommand(new Time());
		// addCommand(new Connect());
		// addCommand(new Trace());
		addCommand(new Admin());
		addCommand(new Info());
		addCommand(new Privmsg());
		addCommand(new Notice());
		addCommand(new Who());
		addCommand(new Whois());
		// addCommand(new Whowas());
		// addCommand(new Kill());
		addCommand(new Ping());
		addCommand(new Pong());
		// addCommand(new Error());
		// Optional Commands
		addCommand(new Away());
	}

}
