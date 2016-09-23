package com.rbruno.irc.commands;

import java.io.File;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Request;
import com.rbruno.irc.util.Utilities;

public class Info extends Command {

	public Info() {
		super("INFO", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		if (request.getArgs().length == 0) {
			if (new File("/info.txt").exists())
				for (String current : Utilities.read("/info.txt"))
					request.connection.send(Reply.RPL_INFO, request.getClient(), ':' + current);
			request.connection.send(Reply.RPL_ENDOFINFO, request.getClient(), ":End of /INFO list");
		} else {
			// TODO: Server
		}
	}

}
