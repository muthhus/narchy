package spacegraph.irc.commands;

import spacegraph.irc.Channel;
import spacegraph.irc.Channel.ChannelMode;
import spacegraph.irc.Client;
import spacegraph.irc.IRCServer;
import spacegraph.irc.Request;
import spacegraph.irc.reply.Error;
import spacegraph.irc.reply.Reply;

import java.util.Iterator;
import java.util.Map;

public class Mode extends Command {

	public Mode() {
		super("MODE", 1);
	}

	@Override
	public void run(Request request) throws java.io.IOException { IRCServer server = request.server();
        if (request.args.length <= 1) {
            String target = request.args[0];
			Channel channel = server.getChannel(target);
			if (channel == null) {
				request.connection.send(Error.ERR_NOSUCHCHANNEL, request.client, target + " :No such channel");
			}
			Map<ChannelMode, Boolean> modeMap = channel.getModeMap();
			Iterator<ChannelMode> modeKey = modeMap.keySet().iterator();
			String modes = "";
			while (modeKey.hasNext()) {
				ChannelMode mode = modeKey.next();
				if (modeMap.get(mode)) modes = modes + mode.getSymbol();
			}
			request.connection.send(Reply.RPL_CHANNELMODEIS, request.client, target + " +" + modes);
		} else {
            String modeFlag = request.args[1];
			if (!(modeFlag.startsWith("+") || modeFlag.startsWith("-"))) request.connection.send(Error.ERR_UMODEUNKNOWNFLAG, request.client, "Unknown MODE flag");
			boolean add = true;
			if (modeFlag.startsWith("-")) add = false;
            if (request.args[0].startsWith("#") || request.args[0].startsWith("&")) {
				// Channels
                Channel target = server.getChannel(request.args[0]);
				if (target == null) {
					request.connection.send(Error.ERR_NOSUCHCHANNEL, request.client, modeFlag + " :No such channel");
					return;
				}
				for (char mode : modeFlag.toLowerCase().toCharArray()) {
					if (request.client.isServerOP() || target.checkOP(request.client)) {
						switch (mode) {
						case 'o':
                            Client clientTarget = server.getClient(request.args[2]);
							if (clientTarget == null) {
								request.connection.send(Error.ERR_NOSUCHNICK, request.client, request.args[2] + " :No such nick");
								continue;
							}
							if (add) {
								target.addOP(clientTarget);
							} else {
								target.takeOP(clientTarget);
							}
							target.send(Reply.RPL_CHANNELMODEIS, target.id + (add ? " +" : " -") + "o " + clientTarget.id);
							break;
						case 'p':
							target.setMode(ChannelMode.PRIVATE, add, request.client);
							break;
						case 's':
							target.setMode(ChannelMode.SECRET, add, request.client);
							break;
						case 'i':
							target.setMode(ChannelMode.INVITE_ONLY, add, request.client);
							break;
						case 't':
							target.setMode(ChannelMode.TOPIC, add, request.client);
							break;
						case 'n':
							target.setMode(ChannelMode.NO_MESSAGE_BY_OUTSIDE, add, request.client);
							break;
						case 'm':
							target.setMode(ChannelMode.MODERATED_CHANNEL, add, request.client);
							break;
						case 'l':
							try {
                                int limit = Integer.parseInt(request.args[2]);
								target.setUserLimit(limit);
							} catch (NumberFormatException ignored) {
								request.connection.send(Error.ERR_NEEDMOREPARAMS, request.client, ":Not enough parameters");
							}
							break;
						case 'b':
							break;
						case 'v':
                            Client voicee = server.getClient(request.args[2]);
							if (voicee != null) {
								if (add) {
									target.giveVoice(voicee);
								} else {
									target.takeVoice(voicee);
								}
							} else {
								request.connection.send(Error.ERR_NOSUCHNICK, request.client, ":No such channel");
							}
							target.send(Reply.RPL_CHANNELMODEIS, target.id + (add ? " +" : " -") + "v " + voicee.id);
							break;
						case 'k':
                            target.setPassword(request.args[2]);
							break;
						}
					} else {
						request.connection.send(Error.ERR_NOPRIVILEGES, request.client, ":Permission Denied- You're not an IRC operator");
					}
				}

			} else {
				// Not channel
                if (server.getClient(request.args[0]) != null) {
                    Client target = server.getClient(request.args[0]);
					if (request.client.isServerOP()) {
						for (char mode : modeFlag.toLowerCase().toCharArray()) {
							switch (mode) {
							case 'i':
								if (target == request.client) {
									target.setMode(Client.ClientMode.INVISIBLE, add, request.client);
								} else {
									request.connection.send(Error.ERR_NOPRIVILEGES, request.client, ":Permission Denied- You're not an IRC operator");
								}
								break;
							case 's':
								if (target == request.client) {
									target.setMode(Client.ClientMode.SERVER_NOTICES, add, request.client);
								} else {
									request.connection.send(Error.ERR_NOPRIVILEGES, request.client, ":Permission Denied- You're not an IRC operator");
								}
								break;
							case 'w':
								target.setMode(Client.ClientMode.WALLOPS, add, request.client);
								break;
							case 'o':
								target.setMode(Client.ClientMode.OPERATOR, add, request.client);
								target.connection.send(Reply.RPL_YOUREOPER, request.connection.getClient(), ":You are now an IRC operator");
								break;
							}
						}
					} else {
						request.connection.send(Error.ERR_NOPRIVILEGES, request.client, ":Permission Denied- You're not an IRC operator");
					}
				} else {
					request.connection.send(Error.ERR_NOSUCHNICK, request.client, modeFlag + " :No such nick/channel");
				}
			}
		}
	}

}
