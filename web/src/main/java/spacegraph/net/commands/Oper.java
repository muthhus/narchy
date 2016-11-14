package spacegraph.net.commands;

import spacegraph.net.Client;
import spacegraph.net.Config;
import spacegraph.net.IRCServer;
import spacegraph.net.Request;
import spacegraph.net.reply.Error;
import spacegraph.net.reply.Reply;

public class Oper extends Command {

	public Oper() {
		super("OPER", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
		if (Config.getProperty("DisableOps").equals("true")) {
			request.connection.send(Error.ERR_NOOPERHOST, request.connection.getClient(), ":No O-lines for your host");
			return;
		}
        if (server.getConfig().checkOpPassword(request.args[0], request.args[1])) {
			request.client.setMode(Client.ClientMode.OPERATOR, true, request.client);
			request.connection.send(Reply.RPL_YOUREOPER, request.connection.getClient(), ":You are now an IRC operator");
		} else {
			request.connection.send(Error.ERR_PASSWDMISMATCH, request.connection.getClient(), ":Password incorrect");
		}
	}
}
