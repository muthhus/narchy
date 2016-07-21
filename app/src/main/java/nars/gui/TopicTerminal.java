package nars.gui;

import com.googlecode.lanterna.TextColor;
import com.googlecode.lanterna.terminal.virtual.DefaultVirtualTerminal;
import nars.util.event.Topic;

import java.io.IOException;
import java.util.function.Function;

/**
 * Prints events received by a Topic
 */
public class TopicTerminal<T> extends DefaultVirtualTerminal {

    public TopicTerminal(Topic<T> logger, int c, int r) {

        this(logger, Object::toString, null, null, c, r);
    }

    public TopicTerminal(Topic<T> logger,
                         Function<T, String> stringify,
                         Function<T, TextColor> fore,
                         Function<T, TextColor> back,
                         int c, int r) {
        super(c, r);

        logger.on(x -> {
            try {
                if (fore!=null) fore(fore.apply(x));
                if (back!=null) back(back.apply(x));
                putLinePre(stringify.apply(x));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

}
