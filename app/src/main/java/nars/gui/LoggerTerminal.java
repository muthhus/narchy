package nars.gui;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import nars.util.DefaultConsoleAppender;
import org.slf4j.Logger;

import java.io.IOException;

/**
 * Created by me on 7/20/16.
 */
public class LoggerTerminal extends DefaultVirtualTerminal {

    private final Logger logger;

    public LoggerTerminal(Logger logger, int c, int r) {
        super(c, r);
        this.logger = logger;

        fore(TextColor.ANSI.WHITE);

        DefaultConsoleAppender app = new MyDefaultConsoleAppender();
        app.start();

        ((ch.qos.logback.classic.Logger) logger).addAppender(app);
    }

    private final class MyDefaultConsoleAppender extends DefaultConsoleAppender {

        @Override
        public void append(ILoggingEvent eventObject) {
            try {
                String s = eventObject.toString();
                putLine(s);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
