package com.rbruno.irc.commands;

import java.io.IOException;
import java.util.ArrayList;

import com.rbruno.irc.IRCServer;
import com.rbruno.irc.reply.Reply;
import com.rbruno.irc.Channel;
import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Names extends Command {

	public Names() {
		super("NAMES", 0);
	}

	@Override
	public void run(Request request) throws IOException {
		IRCServer server = request.server();
		if (request.getArgs().length == 0) {
			server.forEach(current -> {
                String message = current.id + " :";
				ArrayList<Client> clients = current.getClients();
				for (Client client : clients) {
					message = message + (current.checkOP(client) || client.isServerOP() ? "@" : "+") + client.id + " ";
				}
				try {
					request.connection.send(Reply.RPL_NAMREPLY, request.getClient(), message);
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
		} else {
			String[] stringChannels = request.getArgs()[0].split(",");
			for (String current : stringChannels) {
				Channel channel = server.getChannel(current);
                String message = channel.id + " :";
				ArrayList<Client> clients = channel.getClients();
				for (Client client : clients) {
					message = message + (channel.checkOP(client) || client.isServerOP() ? "@" : "+") + client.id + " ";
				}
				request.connection.send(Reply.RPL_NAMREPLY, request.getClient(), message);
			}
		}
	}

}
