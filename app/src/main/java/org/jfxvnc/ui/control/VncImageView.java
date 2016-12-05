package org.jfxvnc.ui.control;

import io.netty.buffer.ByteBuf;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Dimension2D;
import javafx.scene.Cursor;
import javafx.scene.ImageCursor;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.jfxvnc.net.rfb.codec.decoder.ColourMapEvent;
import org.jfxvnc.net.rfb.codec.decoder.ServerDecoderEvent;
import org.jfxvnc.net.rfb.codec.encoder.InputEventListener;
import org.jfxvnc.net.rfb.render.ConnectInfoEvent;
import org.jfxvnc.net.rfb.render.rect.*;
import org.jfxvnc.ui.CutTextEventHandler;
import org.jfxvnc.ui.KeyButtonEventHandler;
import org.jfxvnc.ui.PointerEventHandler;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

public class VncImageView extends ImageView implements BiConsumer<ServerDecoderEvent, ImageRect> {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(VncImageView.class);

    private WritableImage vncImage;

    private PointerEventHandler pointerHandler;
    private CutTextEventHandler cutTextHandler;
    private KeyButtonEventHandler keyHandler;

    private ImageCursor remoteCursor;

    private boolean useClientCursor;

    private final PixelFormat<ByteBuffer> DEFAULT_PIXELFORMAT = PixelFormat.getByteRgbInstance();

    private final AtomicReference<PixelFormat<ByteBuffer>> pixelFormat = new AtomicReference<>(DEFAULT_PIXELFORMAT);

    private SimpleDoubleProperty zoomLevel;

    public VncImageView() {
        setPreserveRatio(true);
        registerListener();
    }

    public void registerListener() {

        setOnMouseEntered(event -> {
            if (!isDisabled()) {
                requestFocus();
                setCursor(remoteCursor != null ? remoteCursor : Cursor.DEFAULT);
            }
        });

        setOnMouseExited(event -> {
            if (!isDisabled()) {
                setCursor(Cursor.DEFAULT);
            }
        });

        zoomLevelProperty().addListener(l -> {
            if (getImage() != null) {
                setFitHeight(getImage().getHeight() * zoomLevelProperty().get());
            }
        });
    }

    public void setPixelFormat(ColourMapEvent event) {

        int[] colors = new int[event.getNumberOfColor()];
        ByteBuf c = event.getColors();
        for (int i = event.getFirstColor(); i < colors.length; i++) {
            colors[i] =
                    (0xff << 24) |
                    ((c.readUnsignedShort() >> 8) << 16) //R
                |   ((c.readUnsignedShort() >> 8) << 8) //G
                |    (c.readUnsignedShort() >> 8); //B
        }

        pixelFormat.set(PixelFormat.createByteIndexedInstance(colors));
    }

    @Override
    public void accept(ServerDecoderEvent event, ImageRect rect) {
        if (event instanceof ConnectInfoEvent) {
            Platform.runLater(() -> setConnectInfoEvent((ConnectInfoEvent) event));
        } else if (event instanceof ColourMapEvent) {
            Platform.runLater(() -> setPixelFormat((ColourMapEvent) event));
        }
        if (rect != null) {
            Platform.runLater(() -> render(rect));
        }
    }

    private void render(ImageRect rect) {
        try {
            if (vncImage == null) {
                logger.error("canvas image has not been initialized");
                return;
            }
            switch (rect.getEncoding()) {
                case HEXTILE:
                    HextileImageRect hextileRect = (HextileImageRect) rect;
                    //PixelWriter writer = vncImage.getPixelWriter();
                    for (RawImageRect rawRect : hextileRect.getRects()) {
                        vncImage.getPixelWriter().setPixels(rawRect.x, rawRect.y, rawRect.width, rawRect.height, pixelFormat.get(),
                                rawRect.getPixels().nioBuffer(), rawRect.getScanlineStride());
                    }
                    break;
                case RAW:
                case ZLIB:
                    RawImageRect rawRect = (RawImageRect) rect;
                    vncImage.getPixelWriter().setPixels(rawRect.x, rawRect.y, rawRect.width, rawRect.height, pixelFormat.get(),
                            rawRect.getPixels().nioBuffer(), rawRect.getScanlineStride());
                    break;
                case COPY_RECT:
                    CopyImageRect copyImageRect = (CopyImageRect) rect;
                    PixelReader reader = vncImage.getPixelReader();
                    WritableImage copyRect = new WritableImage(copyImageRect.width, copyImageRect.height);
                    copyRect.getPixelWriter().setPixels(0, 0, copyImageRect.width, copyImageRect.height, reader, copyImageRect.getSrcX(),
                            copyImageRect.getSrcY());
                    vncImage.getPixelWriter().setPixels(copyImageRect.x, copyImageRect.y, copyImageRect.width, copyImageRect.height,
                            copyRect.getPixelReader(), 0, 0);
                    break;
                case CURSOR:
                    if (!useClientCursor) {
                        logger.warn("ignore cursor encoding");
                        return;
                    }
                    final CursorImageRect cRect = (CursorImageRect) rect;

                    if (cRect.height < 2 && cRect.width < 2) {
                        setCursor(Cursor.NONE);
                        return;
                    }

                    Dimension2D dim = ImageCursor.getBestSize(cRect.width, cRect.height);
                    WritableImage cImage = new WritableImage((int) dim.getWidth(), (int) dim.getHeight());
                    cImage.getPixelWriter().setPixels(0, 0, (int) Math.min(dim.getWidth(), cRect.width), (int) Math.min(dim.getHeight(), cRect.height),
                            PixelFormat.getIntArgbInstance(), cRect.getPixels().nioBuffer().asIntBuffer(), cRect.width);
                    remoteCursor = new ImageCursor(cImage, cRect.getHotspotX(), cRect.getHotspotY());
                    setCursor(remoteCursor);
                    break;
                case DESKTOP_SIZE:
                    logger.debug("resize image: {}", rect);
                    vncImage = new WritableImage(rect.width, rect.height);
                    setImage(vncImage);
                    break;
                default:
                    logger.error("not supported encoding rect: {}", rect);
                    break;
            }
        } catch (Exception e) {
            logger.error("rect: {} {}", rect, e);
        } finally {
            rect.release();
        }
    }

    public void registerInputEventListener(InputEventListener listener) {
        Objects.requireNonNull(listener, "input listener must not be null");
        if (pointerHandler == null) {

            pointerHandler = new PointerEventHandler();
            pointerHandler.register(this);
            pointerHandler.registerZoomLevel(zoomLevelProperty());
            pointerHandler.enabledProperty().bind(disabledProperty().not());
        }
        pointerHandler.setInputEventListener(listener);

        if (keyHandler == null) {
            keyHandler = new KeyButtonEventHandler();
            keyHandler.register(getScene());
            keyHandler.enabledProperty().bind(disabledProperty().not());
        }
        keyHandler.setInputEventListener(listener);

        if (cutTextHandler == null) {
            cutTextHandler = new CutTextEventHandler();
            cutTextHandler.enabledProperty().bind(disabledProperty().not());
        }
        cutTextHandler.setInputEventListener(listener);
    }

    public void unregisterInputEventListener() {
        if (pointerHandler != null) {
            pointerHandler.unregister(this);
            pointerHandler = null;
        }

        if (keyHandler != null) {
            keyHandler.unregister(getScene());
            keyHandler = null;
        }

        if (cutTextHandler != null) {
            cutTextHandler.setInputEventListener(null);
            cutTextHandler = null;
        }
    }

    public DoubleProperty zoomLevelProperty() {
        if (zoomLevel == null) {
            zoomLevel = new SimpleDoubleProperty(1.0);
        }
        return zoomLevel;
    }

    public boolean isUseClientCursor() {
        return useClientCursor;
    }

    public void setUseClientCursor(boolean flag) {
        this.useClientCursor = flag;
        if (!useClientCursor) {
            setCursor(Cursor.DEFAULT);
        }
    }

    public boolean addClipboardText(String text) {
        if (cutTextHandler != null) {
            cutTextHandler.addClipboardText(text);
            return true;
        }
        return false;
    }

    public void setConnectInfoEvent(ConnectInfoEvent e) {
        setImage(vncImage = new WritableImage(e.getFrameWidth(), e.getFrameHeight()));
        setFitHeight(getImage().getHeight() * zoomLevelProperty().get());
        pixelFormat.set(DEFAULT_PIXELFORMAT);
    }

}

