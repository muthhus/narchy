package spacegraph.net.commands;

import spacegraph.net.Channel;
import spacegraph.net.IRCServer;
import spacegraph.net.Request;
import spacegraph.net.reply.Error;
import spacegraph.net.reply.Reply;

public class Topic extends Command {

	public Topic() {
		super("TOPIC", 1);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        Channel channel = server.getChannel(request.args[0]);
		if (channel == null) {
			request.connection.send(Error.ERR_NOSUCHCHANNEL, request.client, request.args[0] + " :No such channel");
			return;
		}
		if (channel.getMode(Channel.ChannelMode.TOPIC) && !channel.checkOP(request.client)) {
			request.connection.send(Error.ERR_CHANOPRIVSNEEDED, request.client, channel.id + " :You're not channel operator");
			request.connection.send(Reply.RPL_TOPIC, request.client, channel.id + ' ' + channel.getTopic());
			return;
		}
        if (request.args.length == 1) {
			request.connection.send(Reply.RPL_TOPIC, request.client, channel.id + ' ' + channel.getTopic());
		} else {
            channel.setTopic(request.args[1]);
		}
	}

}
