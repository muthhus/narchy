package spacegraph.input;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * http://www.java-gaming.org/index.php?PHPSESSID=ecau62mklg3us0870iq44ule60&topic=16866.0
 */
public class GamePad {

    public final Logger logger;

    public GamePad(String device /* ex: js0 */) throws IOException, InterruptedException {
        super();

        logger = LoggerFactory.getLogger(GamePad.class + ":" + device);

        Process proc = new ProcessBuilder().command("jstest", "--event", "/dev/input/" + device).start();

        BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
        new Thread(() -> {
            logger.info("start");
            while (true) {
                try {
                    String l = reader.readLine();
                    logger.info(" {}", l);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        proc.waitFor();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        new GamePad("js0");
    }
}

