package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Request;
import com.rbruno.irc.reply.Reply;

public class Info extends Command {

	public Info() {
		super("INFO", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		if (request.args.length == 0) {
			/*if (new File("/info.txt").exists())
				for (String current : Utilities.read("/info.txt"))
					request.connection.send(Reply.RPL_INFO, request.getClient(), ':' + current);*/
			request.connection.send(Reply.RPL_ENDOFINFO, request.client, ":End of /INFO list");
		} else {
			// TODO: Server
		}
	}

}
