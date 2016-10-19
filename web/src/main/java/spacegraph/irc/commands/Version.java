package spacegraph.irc.commands;

import spacegraph.irc.Config;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Reply;

public class Version extends Command {

	public Version() {
		super("VERSION", 0);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        if (request.args.length == 0) {
			request.connection.send(Reply.RPL_VERSION, request.client, IRCServer.getVersion() + ' ' + Config.getProperty("hostname"));
		} else {
			//TODO: Server
		}
	}

}
