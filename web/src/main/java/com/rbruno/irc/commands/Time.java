package com.rbruno.irc.commands;

import java.sql.Timestamp;
import java.util.Date;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.Config;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Request;

public class Time extends Command {

	public Time() {
		super("TIME", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        if (request.args.length == 0) {
			Date date = new Date();
			request.connection.send(Reply.RPL_TIME, request.client, Config.getProperty("hostname") + " :" + new Timestamp(date.getTime()));
		} else {
			//TODO: Servers
		}
	}
}
