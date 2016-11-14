package spacegraph.net.commands;

import spacegraph.net.Config;
import spacegraph.net.IRCServer;
import spacegraph.net.Request;
import spacegraph.net.reply.Reply;

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
