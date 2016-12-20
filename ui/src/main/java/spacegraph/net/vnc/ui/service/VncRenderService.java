/*******************************************************************************
 * Copyright (c) 2016 comtel inc.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package spacegraph.net.vnc.ui.service;

import javafx.beans.property.*;
import org.slf4j.LoggerFactory;
import spacegraph.net.vnc.rfb.VncConnection;
import spacegraph.net.vnc.rfb.codec.ProtocolState;
import spacegraph.net.vnc.rfb.codec.decoder.BellEvent;
import spacegraph.net.vnc.rfb.codec.decoder.ColourMapEvent;
import spacegraph.net.vnc.rfb.codec.decoder.ServerCutTextEvent;
import spacegraph.net.vnc.rfb.codec.decoder.ServerDecoderEvent;
import spacegraph.net.vnc.rfb.codec.encoder.InputEventListener;
import spacegraph.net.vnc.rfb.render.ConnectInfoEvent;
import spacegraph.net.vnc.rfb.render.ProtocolConfiguration;
import spacegraph.net.vnc.rfb.render.RenderCallback;
import spacegraph.net.vnc.rfb.render.RenderProtocol;
import spacegraph.net.vnc.rfb.render.rect.ImageRect;

import java.util.function.BiConsumer;

public class VncRenderService implements RenderProtocol {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(VncRenderService.class);

    private final VncConnection con;

    private BiConsumer<ServerDecoderEvent, ImageRect> eventConsumer;

    private final BooleanProperty listeningMode = new SimpleBooleanProperty(false);

    private final ReadOnlyBooleanWrapper online = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyBooleanWrapper bell = new ReadOnlyBooleanWrapper(false);
    private final ReadOnlyStringWrapper serverCutText = new ReadOnlyStringWrapper();

    private final ReadOnlyObjectWrapper<ConnectInfoEvent> connectInfo = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<ProtocolState> protocolState = new ReadOnlyObjectWrapper<>(ProtocolState.CLOSED);
    private final ReadOnlyObjectWrapper<InputEventListener> inputEventListener = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyObjectWrapper<ColourMapEvent> colourMapEvent = new ReadOnlyObjectWrapper<>();

    private final ReadOnlyObjectWrapper<Throwable> exceptionCaught = new ReadOnlyObjectWrapper<>();

    private ReadOnlyObjectWrapper<ImageRect> image;

    private final double minZoomLevel = 0.2;
    private final double maxZoomLevel = 5.0;

    private final DoubleProperty zoomLevel = new SimpleDoubleProperty(1);
    private final BooleanProperty fullSceen = new SimpleBooleanProperty(false);
    private final BooleanProperty restart = new SimpleBooleanProperty(false);

    public VncRenderService() {
        this(new VncConnection());
    }

    public VncRenderService(VncConnection con) {
        this.con = con;
        zoomLevel.addListener((l, a, b) -> {
            if (b.doubleValue() > maxZoomLevel) {
                zoomLevel.set(maxZoomLevel);
            } else if (b.doubleValue() < minZoomLevel) {
                zoomLevel.set(minZoomLevel);
            }
        });

    }

    public void setEventConsumer(BiConsumer<ServerDecoderEvent, ImageRect> c) {
        eventConsumer = c;
    }

    public ProtocolConfiguration getConfiguration() {
        return con.getConfiguration();
    }

    public void connect() {
        con.setRenderProtocol(this);
        con.addFaultListener(exceptionCaught::set);

        if (listeningMode.get()) {
            con.startListeningMode().whenComplete((c, th) -> {
                if (th != null) {
                    exceptionCaught(th);
                    disconnect();
                }
            });
            return;
        }
        con.connect().whenComplete((c, th) -> {
            if (th != null) {
                exceptionCaught(th);
                disconnect();
            }
        });
    }

    public void disconnect() {
        con.disconnect();
        online.set(false);
    }

    @Override
    public void render(ImageRect rect, RenderCallback callback) {
        if (eventConsumer != null) {
            eventConsumer.accept(null, rect);
        }
        if (image != null) {
            image.set(rect);
        }
        callback.renderComplete();
    }

    @Override
    public void eventReceived(ServerDecoderEvent event) {
        logger.debug("event received: {}", event);

        if (eventConsumer != null) {
            eventConsumer.accept(event, null);
        }

        if (event instanceof ConnectInfoEvent) {
            connectInfo.set((ConnectInfoEvent) event);
            online.set(true);
            return;
        }
        if (event instanceof BellEvent) {
            bell.set(!bell.get());
            return;
        }
        if (event instanceof ServerCutTextEvent) {
            serverCutText.set(((ServerCutTextEvent) event).getText());
            return;
        }
        if (event instanceof ColourMapEvent) {
            colourMapEvent.set((ColourMapEvent) event);
            return;
        }

        logger.warn("not handled event: {}", event);
    }

    @Override
    public void exceptionCaught(Throwable t) {
        exceptionCaught.set(t);
    }

    @Override
    public void stateChanged(ProtocolState state) {
        protocolState.set(state);
        if (state == ProtocolState.CLOSED) {
            disconnect();
        }
    }

    @Override
    public void registerInputEventListener(InputEventListener listener) {
        inputEventListener.set(listener);
    }

    public ReadOnlyObjectProperty<ConnectInfoEvent> connectInfoProperty() {
        return connectInfo.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<ProtocolState> protocolStateProperty() {
        return protocolState;
    }

    public ReadOnlyObjectProperty<InputEventListener> inputEventListenerProperty() {
        return inputEventListener.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<ImageRect> imageProperty() {
        if (image == null) {
            image = new ReadOnlyObjectWrapper<>();
        }
        return image.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<ColourMapEvent> colourMapEventProperty() {
        return colourMapEvent.getReadOnlyProperty();
    }

    public ReadOnlyObjectProperty<Throwable> exceptionCaughtProperty() {
        return exceptionCaught.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty connectingProperty() {
        return con.connectingProperty();
    }

    public ReadOnlyBooleanProperty connectedProperty() {
        return con.connectedProperty();
    }

    public ReadOnlyBooleanProperty onlineProperty() {
        return online.getReadOnlyProperty();
    }

    public ReadOnlyBooleanProperty bellProperty() {
        return bell.getReadOnlyProperty();
    }

    public ReadOnlyStringProperty serverCutTextProperty() {
        return serverCutText.getReadOnlyProperty();
    }

    public DoubleProperty zoomLevelProperty() {
        return zoomLevel;
    }

    public BooleanProperty fullSceenProperty() {
        return fullSceen;
    }

    public BooleanProperty restartProperty() {
        return restart;
    }

    public BooleanProperty listeningModeProperty() {
        return listeningMode;
    }

    public IntegerProperty listeningPortProperty() {
        return getConfiguration().listeningPortProperty();
    }
}
