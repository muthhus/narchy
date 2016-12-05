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
package org.jfxvnc.ui;

import com.airhacks.afterburner.injection.Injector;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;
import nars.util.FX;
import nars.video.PixelBag;
import org.jfxvnc.net.rfb.codec.security.SecurityType;
import org.jfxvnc.ui.service.VncRenderService;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.obj.widget.MatrixView;

import static nars.video.Bitmap2D.*;

/**
 * /usr/bin/qemu-system-x86_64 -boot c  -m 512 -hda '/home/me/img/Linux.qcow' -cdrom  '/home/me/Downloads/cm-x86-13.0-rc1.iso' -net nic,vlan=0 -net user,vlan=0 -localtime -vnc :1 -monitor stdio
 */
public class VncClient {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(VncClient.class);


    private PixelReader reader;
    public WritableImage image;


    public VncClient(String host, int port /* TODO password, etc */ ) {

        Injector.setLogger(logger::trace);

        VncRenderService vncService = Injector.instantiateModelOrService(VncRenderService.class);
        vncService.getConfiguration().hostProperty().set(host);
        vncService.getConfiguration().portProperty().set(port);
        vncService.getConfiguration().securityProperty().set(SecurityType.NONE);
        vncService.connect();

        VncCanvas view = new VncCanvas() {


            @Override
            protected void setImage(WritableImage vncImage) {
                super.setImage(vncImage);
                image = vncImage;
                reader = vncImage.getPixelReader();
            }
        };
        vncService.setEventConsumer(view);

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
            FX.newWindow("x", new Pane(new ImageView(image)));
        });
    }

    public Surface newSurface() {
        //TODO
        int pw = 200;
        int ph = 200;
        return new MatrixView(pw, ph, (x, y, gl) -> {
            final PixelReader reader = this.reader;
            if (reader !=null) {
                int argb = reader.getArgb(x, y);
                gl.glColor3f(decodeRed(argb), decodeGreen(argb), decodeBlue(argb));
            }
            return 0;
        });
    }

    public PixelBag newSensor(int pw, int ph) {
        return new PixelBag(pw, ph) {

            @Override
            public int sw() {
                return image==null ? 0 : (int) image.getWidth();
            }

            @Override
            public int sh() {
                return image==null ? 0 : (int) image.getHeight();
            }

            @Override
            public int rgb(int sx, int sy) {
                return reader.getArgb(sx, sy);
            }
        };
    }

    public static void main(String[] args) {
        VncClient v = new VncClient("localhost", 5901);
        v.newFXWindow();

        SpaceGraph.window(v.newSurface(), 800, 800);
    }


}
