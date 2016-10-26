package nars.web;

import io.undertow.server.handlers.PathHandler;
import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import nars.$;
import nars.IO;
import nars.NAR;
import nars.time.Tense;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spacegraph.web.WebsocketService;

import java.io.IOException;

import static nars.web.WebServer.socket;

/**
 * Created by me on 9/23/16.
 *
 * TODO https://github.com/minimaxir/big-list-of-naughty-strings/blob/master/blns.txt
 */
@Deprecated public class NARServices {

    private static final Logger logger = LoggerFactory.getLogger(NARServices.class);

    public NARServices(NAR nar, PathHandler path) {

        path
                .addPrefixPath("/terminal", socket(new NarseseIOService(nar)))
                .addPrefixPath("/emotion", socket(new EvalService(nar, "emotion", 200)))
                .addPrefixPath("/active", socket(new ActiveConceptService(nar, 100, 64)))
                .addPrefixPath("/json/in", socket(new WebsocketService() {

                    @Override
                    protected void onFullTextMessage(WebSocketChannel channel, BufferedTextMessage message) throws IOException {
                        String s = message.getData();
                        logger.info("in: {}", s);
                        nar.believe(IO.fromJSON(s), Tense.Present, 1f);
                    }

                    @Override
                    public void onStart() {

                    }

                    @Override
                    public void onStop() {

                    }
                }));


    }
}
