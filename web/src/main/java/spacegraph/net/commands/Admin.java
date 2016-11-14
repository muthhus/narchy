package spacegraph.net.commands;

import spacegraph.net.Config;
import spacegraph.net.IRCServer;
import spacegraph.net.Request;
import spacegraph.net.reply.Reply;

public class Admin extends Command {

	public Admin() {
		super("ADMIN", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        if (request.args.length == 0)  {
			request.connection.send(Reply.RPL_ADMINME, request.client, Config.getProperty("hostname") + " :Administrative info");
			request.connection.send(Reply.RPL_ADMINLOC1, request.client, Config.getProperty("hostname") + " :" + Config.getProperty("AdminName"));
			request.connection.send(Reply.RPL_ADMINLOC2, request.client, Config.getProperty("hostname") + " :" + Config.getProperty("AdminNick"));
			request.connection.send(Reply.RPL_ADMINMAIL, request.client, Config.getProperty("hostname") + " :" + Config.getProperty("AdminEmail"));
		} else {
			//TODO: Server
		}
	}
}
