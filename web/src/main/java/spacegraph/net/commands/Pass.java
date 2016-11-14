package spacegraph.net.commands;

import spacegraph.net.Request;

public class Pass extends Command {

	public Pass() {
		super("PASS", 1);
	}

	@Override
	public void run(Request request) {
        request.connection.setConnectionPassword(request.args[0]);
	}

}
