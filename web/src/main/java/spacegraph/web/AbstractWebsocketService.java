package spacegraph.web;

import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.WebSocketCallback;
import io.undertow.websockets.core.WebSocketChannel;

/**
 * Created by me on 9/26/16.
 */
public abstract class AbstractWebsocketService extends AbstractReceiveListener implements WebSocketCallback<Void>,WebSocketConnectionCallback {

    public void send(WebSocketChannel socket, Object object) {

        WebsocketService.send(socket, object, this);
    }

}
