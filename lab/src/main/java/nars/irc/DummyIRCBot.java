package nars.irc;

import nars.inter.InterNAR;
import nars.nal.Tense;
import nars.nar.Terminal;
import nars.time.RealtimeMSClock;
import nars.util.data.random.XorShift128PlusRandom;

/**
 * Created by me on 7/10/16.
 */
public class DummyIRCBot extends IRCBot {

    final Terminal terminal = new Terminal(16, new XorShift128PlusRandom(1), new RealtimeMSClock());

    static boolean running = true;

    private final InterNAR inter;

    public DummyIRCBot(String server, String nick, String channel, int udpPort) throws Exception {
        super(server, nick, channel);

        terminal.log();

        inter = new InterNAR(terminal, (short)udpPort );
        inter.broadcastPriorityThreshold = 0.5f; //lower threshold

        terminal.believe("connect(\"" + server + "\").", Tense.Present, 1f, 0.9f);
    }


    @Override
    protected void onMessage(String channel, String nick, String msg) {

        terminal.believe(
                "irc(\"" + channel + "\",\"" + nick + "\",\"" + msg + "\")",
                Tense.Present, 1f, 0.9f );

    }

    public static void main(String[] args) throws Exception {
        //while (running) {
            try {
                new DummyIRCBot(
                        "irc.freenode.net",
                        //"localhost",
                        "NARchy", "#netention", 11001);
            } catch (Exception e) {
                e.printStackTrace();

            }
        //}
    }
}
