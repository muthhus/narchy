package com.rbruno.irc.plugin;

import com.rbruno.irc.Client;
import com.rbruno.irc.Request;

public class Plugin {

	private String name;

	public void onEnable() {
	}

	public void onClientLogin(Client client) {
	}

	public void onRequest(Request request) {
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

}
