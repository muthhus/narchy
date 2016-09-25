package nars.web;

import com.rbruno.irc.IRCServer;
import com.sorcix.sirc.IrcConnection;
import com.sorcix.sirc.IrcServer;
import com.sorcix.sirc.NickNameException;
import com.sorcix.sirc.PasswordException;

import java.io.IOException;

/**
 * https://sirc.sorcix.com/
 */
public class IRCService extends IRCServer {

    private final WebServer web;

    public IRCService(WebServer web) throws Exception {
        super("localhost", 6667);
        this.web = web;

    }
//
//
//    public static void main(String[] args) throws PasswordException, NickNameException, IOException {
//        new IRCService().connect(new IRC);
//    }
}
