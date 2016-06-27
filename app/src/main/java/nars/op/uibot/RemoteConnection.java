package nars.op.uibot;

import boofcv.struct.image.ImageBase;
import boofcv.struct.image.InterleavedU8;
import ch.qos.logback.classic.Level;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import javafx.geometry.Dimension2D;
import javafx.scene.ImageCursor;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import org.jfxvnc.net.rfb.codec.ProtocolHandler;
import org.jfxvnc.net.rfb.codec.ProtocolState;
import org.jfxvnc.net.rfb.codec.decoder.ServerDecoderEvent;
import org.jfxvnc.net.rfb.codec.encoder.InputEventListener;
import org.jfxvnc.net.rfb.codec.security.SecurityType;
import org.jfxvnc.net.rfb.render.*;
import org.jfxvnc.net.rfb.render.rect.CopyImageRect;
import org.jfxvnc.net.rfb.render.rect.CursorImageRect;
import org.jfxvnc.net.rfb.render.rect.ImageRect;
import org.jfxvnc.net.rfb.render.rect.RawImageRect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.IntStream;

/**
 * VNC/RDP client to a server
 */
public class RemoteConnection implements RenderProtocol, ImageProcess {

    private static final Logger logger = LoggerFactory.getLogger(RemoteConnection.class);
    static {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger)LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.toLevel("info"));
    }

    WritableImage image;

    protected InterleavedU8 u8img;


    public RemoteConnection(String host, int port, String password) throws InterruptedException {
        ProtocolConfiguration config = new DefaultProtocolConfiguration();
        config.securityProperty().set(SecurityType.VNC_Auth);
        config.sharedProperty().set(Boolean.TRUE);
        config.passwordProperty().set(password);
        config.hostProperty().set(host);
        config.portProperty().set(port);


//            if (args != null && args.length >= 3) {
//                config.securityProperty().set(SecurityType.VNC_Auth);
//                config.hostProperty().set(args[0]);
//                config.portProperty().set(Integer.parseInt(args[1]));
//                config.passwordProperty().set(args[2]);
//                config.sharedProperty().set(Boolean.TRUE);
//            } else {
//                System.err.println("arguments missing (host port password)");
//                config.securityProperty().set(SecurityType.VNC_Auth);
//                config.hostProperty().set("127.0.0.1");
//                config.portProperty().set(5902);
//                config.passwordProperty().set("vnc");
//                config.sharedProperty().set(Boolean.TRUE);
//            }


        // final SslContext sslContext =
        // SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);

        EventLoopGroup workerGroup = new NioEventLoopGroup(1);
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup);
            b.channel(NioSocketChannel.class);
            b.option(ChannelOption.SO_KEEPALIVE, true);
            b.option(ChannelOption.TCP_NODELAY, true);


            b.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {

                    // use ssl
                    // ch.pipeline().addLast(sslContext.newHandler(ch.alloc()));
                    ch.pipeline().addLast(new ProtocolHandler(RemoteConnection.this, config));
                }
            });

            ChannelFuture f = b.connect(host, port).sync();

            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }

    }


    @Override
    public void render(ImageRect rect, RenderCallback callback) {
        //System.out.println(rect);
        render(rect);
    }

    @Override
    public void exceptionCaught(Throwable t) {
        logger.error("Exception: {}", t);
    }

    @Override
    public void stateChanged(ProtocolState state) {
        //System.out.println(state);
    }

    @Override
    public void registerInputEventListener(InputEventListener listener) {
        //System.out.println(listener);
    }

    @Override
    public void eventReceived(ServerDecoderEvent evnt) {
        //System.out.println(evnt);
        if (evnt instanceof ConnectInfoEvent) {
            ConnectInfoEvent ce = (ConnectInfoEvent) evnt;
            resize(ce.getFrameWidth(), ce.getFrameHeight());
        }
    }

    public void resize(int frameWidth, int frameHeight) {
        image = new WritableImage(frameWidth, frameHeight);
    }


    protected void render(ImageRect rect) {

        if (image == null) {
            logger.error("canvas image has not been initialized");
            return;
        }

        PixelReader ir = image.getPixelReader();
        try {
            switch (rect.getEncoding()) {
                case DESKTOP_SIZE:
                    logger.debug("resize image: {}", rect);
                    resize(rect.getWidth(), rect.getHeight());
                    //vncView.setImage(vncImage);
                    break;
                case RAW:
                case ZLIB:
                    RawImageRect rawRect = (RawImageRect) rect;
                    image.getPixelWriter().setPixels(rawRect.getX(), rawRect.getY(), rawRect.getWidth(),
                            rawRect.getHeight(), PixelFormat.getIntArgbInstance(), rawRect.getPixels(), 0,
                            rawRect.getWidth());

                    break;
                case COPY_RECT:
                    CopyImageRect copyImageRect = (CopyImageRect) rect;

                    PixelReader reader = ir;
                    WritableImage copyRect = new WritableImage(copyImageRect.getWidth(), copyImageRect.getHeight());
                    copyRect.getPixelWriter().setPixels(0, 0, copyImageRect.getWidth(), copyImageRect.getHeight(), reader,
                            copyImageRect.getSrcX(), copyImageRect.getSrcY());
                    image.getPixelWriter().setPixels(copyImageRect.getX(), copyImageRect.getY(),
                            copyImageRect.getWidth(), copyImageRect.getHeight(), copyRect.getPixelReader(), 0, 0);
                    break;
                case CURSOR:
//                        if (!prop.clientCursorProperty().get()) {
//                            logger.warn("ignore cursor encoding");
//                            return;
//                        }
                    final CursorImageRect cRect = (CursorImageRect) rect;

                    if (cRect.getHeight() < 2 && cRect.getWidth() < 2) {
                        //vncView.setCursor(Cursor.NONE);
                        return;
                    }

                    if (cRect.getBitmask() != null && cRect.getBitmask().length > 0) {
                        // remove transparent pixels
                        int maskBytesPerRow = Math.floorDiv((cRect.getWidth() + 7), 8);
                        IntStream.range(0, cRect.getHeight())
                                .forEach(
                                        y -> IntStream.range(0, cRect.getWidth())
                                                .filter(x -> (cRect.getBitmask()[(y * maskBytesPerRow)
                                                        + Math.floorDiv(x, 8)] & (1 << 7 - Math.floorMod(x, 8))) < 1)
                                                .forEach(x -> cRect.getPixels()[y * cRect.getWidth() + x] = 0));
                    }

                    Dimension2D dim = ImageCursor.getBestSize(cRect.getWidth(), cRect.getHeight());
                    WritableImage cImage = new WritableImage((int) dim.getWidth(), (int) dim.getHeight());
                    cImage.getPixelWriter().setPixels(0, 0, (int) Math.min(dim.getWidth(), cRect.getWidth()),
                            (int) Math.min(dim.getHeight(), cRect.getHeight()), PixelFormat.getIntArgbInstance(),
                            cRect.getPixels(), 0, cRect.getWidth());
                    //remoteCursor = new ImageCursor(cImage, cRect.getHotspotX(), cRect.getHotspotY());
                    //vncView.setCursor(remoteCursor);
                    break;
                default:
                    logger.error("not supported encoding rect: {}", rect);
                    break;
            }
        } catch (Exception e) {
            logger.error("rect: " + String.valueOf(rect), e);
        }



    }

    @Override
    public InterleavedU8 apply(ImageBase notUsed) {
        return u8img;
    }
}
