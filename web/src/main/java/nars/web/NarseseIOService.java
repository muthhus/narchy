package nars.web;

import io.undertow.websockets.core.BufferedTextMessage;
import io.undertow.websockets.core.WebSocketChannel;
import nars.NAR;
import nars.util.event.Active;

import java.io.IOException;

/**
 * Created by me on 4/21/16.
 */
@Deprecated
public class NarseseIOService extends WebsocketService {


    private final NAR nar;
    private Active active;

    public NarseseIOService(NAR n) {
        super();
        this.nar =  n;
    }

    @Override
    public void onStart() {


        active = new Active(
                nar.eventInput.on(t -> send(
                        " IN: " + t)),
            /*nar.memory.eventDerived.on(t -> send(socket,
                    "DER: " + t)),*/
                nar.eventAnswer.on(t -> send(
                        "ANS: " + t)),
                nar.eventError.on(t -> send(
                        "ERR: " + t))
        );
    }

    @Override
    public void onStop() {
        active.off();
    }

    @Override
    protected void onFullTextMessage(WebSocketChannel socket, BufferedTextMessage message) throws IOException {

        //System.out.println("onFullTextMessage: " + message);
        nar.input(message.getData());

//            if (attemptJSONParseOfText) {
//                try {
//                    System.out.println(socket + " recv txt: " + message.getData());
//                    //JsonNode j = Core.json.readValue(message.getData(), JsonNode.class);
//                    //onJSONMessage(socket, j);
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
//            }
    }


}
