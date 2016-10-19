package spacegraph.irc.commands;

import spacegraph.irc.Channel;
import spacegraph.irc.Client;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Error;
import spacegraph.irc.reply.Reply;

public class Invite extends Command {

	public Invite() {
		super("INVITE", 2);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        Channel channel = server.getChannel(request.args[1]);
		if (channel == null) {
			request.connection.send(Error.ERR_NOSUCHCHANNEL, request.client, request.args[1] + " :No such channel");
			return;
		}
		if (!channel.checkOP(request.client) && !request.client.isServerOP()) {
			request.connection.send(Error.ERR_CHANOPRIVSNEEDED, request.client, request.args[1] + " :You're not channel operator");
			return;
		}
        Client target = server.getClient(request.args[0]);
		if (target == null) {
			request.connection.send(Error.ERR_NOSUCHNICK, request.client, request.args[1] + " :No such nick");
			return;
		}
		channel.inviteUser(target);
		request.connection.send(Reply.RPL_INVITING, request.client, target.id + ' ' + channel.id);
		target.connection.send(':' + request.client.getAbsoluteName() + " INVITE " + target.id + ' ' + channel.id);
	}

}
