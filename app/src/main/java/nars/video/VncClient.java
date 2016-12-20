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
package nars.video;

import com.airhacks.afterburner.injection.Injector;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.image.WritablePixelFormat;
import javafx.scene.layout.BorderPane;
import nars.FX;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.net.vnc.rfb.codec.decoder.ServerDecoderEvent;
import spacegraph.net.vnc.rfb.codec.encoder.KeyButtonEvent;
import spacegraph.net.vnc.rfb.codec.security.SecurityType;
import spacegraph.net.vnc.rfb.render.rect.ImageRect;
import spacegraph.net.vnc.ui.VncCanvas;
import spacegraph.net.vnc.ui.control.VncImageView;
import spacegraph.net.vnc.ui.service.VncRenderService;
import spacegraph.render.Draw;

import static nars.FX.scrolled;
import static nars.video.VideoBag.EventTimeline.tgaTexture;

/**
 * /usr/bin/qemu-system-x86_64 -boot c  -m 512 -hda '/home/me/img/Linux.qcow' -cdrom  '/home/me/Downloads/cm-x86-13.0-rc1.iso' -net nic,vlan=0 -net user,vlan=0 -localtime -vnc :1 -monitor stdio
 */
public class VncClient {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(VncClient.class);
    private final VncCanvas renderer;
    private final VncRenderService vncService;


    private PixelReader vncReader;
    public WritableImage image;


    public VncClient(String host, int port /* TODO password, etc */ ) {

        Injector.setLogger(logger::trace);

        vncService = Injector.instantiateModelOrService(VncRenderService.class);
        vncService.getConfiguration().hostProperty().set(host);
        vncService.getConfiguration().portProperty().set(port);
        vncService.getConfiguration().securityProperty().set(SecurityType.NONE);
        //vncService.getConfiguration().securityProperty().set(SecurityType.VNC_Auth);
        //vncService.getConfiguration().passwordProperty().set("admin");
        vncService.connect();

        renderer = new VncCanvas() {

            @Override
            public void accept(ServerDecoderEvent event, ImageRect rect) {
                super.accept(event, rect);
                //System.out.println(event + " " + rect);
            }


            @Override
            protected void setImage(WritableImage vncImage) {
                super.setImage(vncImage);
                image = vncImage;
                vncReader = vncImage.getPixelReader();
            }
        };
        vncService.setEventConsumer(renderer);

//        SpaceGraph.window(new Surface() {
//
//
//
//            public GLTexture td;
//            int[] data = null;
//
//            @Override
//            protected void paint(GL2 gl) {
//                super.paint(gl);
//
//
//                if (view.vncImage != null) {
//                    int pw = (int) view.vncImage.getWidth();
//                    int ph = (int) view.vncImage.getHeight();
//                    if (pw == 0 || ph == 0)
//                        return;
//
//                    if (td == null) {
//                        td = new GLTexture(gl.getGL4(), UnsignedByte, 4, pw, ph, 1, true, 2);
//                    }
//
//                    int bufSize = 4 * pw * ph;
//                    if (data == null || data.length != bufSize/4)
//                        data = new int[bufSize/4];  // your byte array, 4 * int count
//                    view.vncImage.getPixelReader().getPixels(
//                        0, 0, pw, ph, WritablePixelFormat.getIntArgbInstance(), data,
//                        0, pw
//                    );
//
//
//
////                    td.clear();
//                    IntBuffer bdata = IntBuffer.wrap(data);
////                    td.copyFrom(bdata);
////                    td.updateMipMaps();
//
////                    td.bind();
////                                ) TextureIO.newTextureData(gl.getGLProfile(),
////                                new ByteArrayInputStream(data), GL2.GL_BGRA, GL2.GL_BGRA, false, (String)null);
////                    mGL.glActiveTexture(GL.GL_TEXTURE0);
////                    mGL.glBindTexture(mTextureTarget, getId());
//                    gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGBA, pw, ph, 0,
//                            GL2.GL_RGBA, GL2.GL_UNSIGNED_INT_8_8_8_8, bdata);
//
//                    Draw.rectTex(gl, 0, 0, 10, 10, 0);
//                    td.unbind();
//
//
//                }
//
//            }
//        }, 800, 800);


    }

    public void newFXWindow() {
        FX.run(()->{
            final VncImageView imageView = new VncImageView() {

                @Override
                protected void onFired(KeyButtonEvent msg) {
                    System.out.println("KEY: " + msg);
                }
            };

            //vncService.inputEventListenerProperty().addListener(l -> );


            renderer.vncImage.addListener((images, prev, next)->{
                if (next!=null)
                    imageView.setImage(next);
            });

            imageView.setImage(image);

//            imageView.setScaleX(2);
//            imageView.setScaleY(2);

            FX.newWindow("x", new BorderPane(scrolled((imageView))));

            //imageView.registerInputEventListener(vncService.inputEventListenerProperty().get());

        });
    }

    public Surface newSurface() {
//        //TODO
//        int pw = 200;
//        int ph = 200;
//        return new MatrixView(pw, ph, (x, y, gl) -> {
//            final PixelReader reader = this.vncReader;
//            if (reader !=null) {
//                int argb = reader.getArgb(x, y);
//                gl.glColor3f(decodeRed(argb), decodeGreen(argb), decodeBlue(argb));
//            }
//            return 0;
//        });

        return new Surface() {

            Texture texture = null;

            @Override
            protected void paint(GL2 gl) {
                super.paint(gl);

                //VideoBag.Frame f = ff.get();

                Texture tt = texture;
                if (tt!=null) {
                    tt.destroy(gl);
                    tt = null;
                }

                if (vncReader!=null) {



                    int ww = (int)renderer.vncImage.get().getWidth();
                    int hh = (int)renderer.vncImage.get().getHeight();
                    byte[] data = new byte[ww * hh * 4];
                    vncReader.getPixels(0,0,ww,hh, WritablePixelFormat.getByteBgraInstance(), data, 0, ww*4);

                    tt = tgaTexture(ww, hh, true, data);

                    texture = tt;

                }

                float xx = 0, yy = 0;
                float sc = 1f;
                if (tt!=null) {
                    Draw.rectTex(gl, tt, xx, yy, sc, sc, 0);
                } else {
                    Draw.colorPolarized(gl, 0.5f);
                    Draw.rect(gl, xx, yy, sc, sc);
                }
            }
        };
    }

    public PixelBag newSensor(int pw, int ph) {
        return new PixelBag(pw, ph) {

            @Override public int sw() { return image==null ? 0 : (int) image.getWidth(); }

            @Override public int sh() { return image==null ? 0 : (int) image.getHeight(); }

            @Override public int rgb(int sx, int sy) { return vncReader.getArgb(sx, sy); }
        };
    }

    public static void main(String[] args) {
        VncClient v = new VncClient("localhost", 5901);

        //1.
        v.newFXWindow();

        //2.
        SpaceGraph.window(v.newSurface(), 800, 800);

//        Default nar = NAgents.newMultiThreadNAR(2, new RealTime.CS(true).dur(0.25f));
//
//        NAgents a = new NAgents(nar) {
//
//            {
//                PixelBag pb = v.newSensor(64, 64);
//                pb.addActions("vnc", this);
//
//                addCamera("vnc", pb, (v) -> t(v, alpha));
//            }
//
//            @Override
//            protected float act() {
//                return 0;
//            }
//
//
//        };
//        NAgents.chart(a);
//
//        a.runRT(55f);


    }


}
