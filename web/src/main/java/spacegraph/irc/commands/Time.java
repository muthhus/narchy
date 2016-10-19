package spacegraph.irc.commands;

import spacegraph.irc.Config;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Reply;

import java.sql.Timestamp;
import java.util.Date;

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
