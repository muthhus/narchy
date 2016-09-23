package com.rbruno.irc.commands;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Config;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Request;

public class Admin extends Command {

	public Admin() {
		super("ADMIN", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		if (request.getArgs().length == 0)  {
			request.connection.send(Reply.RPL_ADMINME, request.getClient(), Config.getProperty("hostname") + " :Administrative info");
			request.connection.send(Reply.RPL_ADMINLOC1, request.getClient(), Config.getProperty("hostname") + " :" + Config.getProperty("AdminName"));
			request.connection.send(Reply.RPL_ADMINLOC2, request.getClient(), Config.getProperty("hostname") + " :" + Config.getProperty("AdminNick"));
			request.connection.send(Reply.RPL_ADMINMAIL, request.getClient(), Config.getProperty("hostname") + " :" + Config.getProperty("AdminEmail"));
		} else {
			//TODO: Server
		}
	}
}
