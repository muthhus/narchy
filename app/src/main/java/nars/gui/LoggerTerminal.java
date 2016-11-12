package nars.gui;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import nars.$;
import nars.util.DefaultConsoleAppender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class LoggerTerminal extends DefaultVirtualTerminal {

    public LoggerTerminal(int c, int r) {
        this($.logger, c, r);
    }

    public LoggerTerminal(Logger logger, int c, int r) {
        super(c, r);

        DefaultConsoleAppender app = new MyDefaultConsoleAppender();
        app.start();
        ((ch.qos.logback.classic.Logger) logger).addAppender(app);

    }

    private final class MyDefaultConsoleAppender extends DefaultConsoleAppender {

        @Override
        public void append(ILoggingEvent eventObject) {
            try {
                putLinePre(eventObject.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
