package spacegraph.net.commands;

import spacegraph.net.Config;
import spacegraph.net.IRCServer;
import spacegraph.net.Request;
import spacegraph.net.reply.Reply;

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
