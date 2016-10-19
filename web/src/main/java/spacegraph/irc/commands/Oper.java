package spacegraph.irc.commands;

import spacegraph.irc.Config;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Error;
import spacegraph.irc.reply.Reply;
import spacegraph.irc.Client;

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
