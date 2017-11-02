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

import com.jogamp.nativewindow.util.PixelFormat;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.util.texture.Texture;
import net.propero.rdp.applet.RdpApplet;
import org.slf4j.LoggerFactory;
import spacegraph.SpaceGraph;
import spacegraph.Surface;

import spacegraph.render.Draw;


/**
 * /usr/bin/qemu-system-x86_64 -boot c  -m 512 -hda '/home/me/img/Linux.qcow' -cdrom  '/home/me/Downloads/cm-x86-13.0-rc1.iso' -net nic,vlan=0 -net user,vlan=0 -localtime -vnc :1 -monitor stdio
 */
public class Rdp {

    private final static org.slf4j.Logger logger = LoggerFactory.getLogger(Rdp.class);
    final RdpApplet rdp;

    public Rdp(String host, int port) {
        rdp = new RdpApplet();
        //rdp.host, port);
    }


//    public void newFXWindow() {
//        FX.run(()->{
//            final VncImageView imageView = new VncImageView() {
//
//                @Override
//                protected void onFired(KeyButtonEvent msg) {
//                    System.out.println("KEY: " + msg);
//                }
//            };
//
//            //vncService.inputEventListenerProperty().addListener(l -> );
//
//
//            renderer.vncImage.addListener((images, prev, next)->{
//                if (next!=null)
//                    imageView.setImage(next);
//            });
//
//            imageView.setImage(image);
//
////            imageView.setScaleX(2);
////            imageView.setScaleY(2);
//
//            FX.newWindow("x", new BorderPane(scrolled((imageView))));
//
//            //imageView.registerInputEventListener(vncService.inputEventListenerProperty().get());
//
//        });
//    }


////        //TODO
////        int pw = 200;
////        int ph = 200;
////        return new MatrixView(pw, ph, (x, y, gl) -> {
////            final PixelReader reader = this.vncReader;
////            if (reader !=null) {
////                int argb = reader.getArgb(x, y);
////                gl.glColor3f(decodeRed(argb), decodeGreen(argb), decodeBlue(argb));
////            }
////            return 0;
////        });
//
//        return new Surface() {
//
//            Texture texture;
//
//            @Override
//            protected void paint(GL2 gl) {
//
//                //VideoBag.Frame f = ff.get();
//
//                Texture tt = texture;
//                if (tt!=null) {
//                    tt.destroy(gl);
//                    tt = null;
//                }
//
//                if (vncReader!=null) {
//
//
//
//                    int ww = (int)renderer.vncImage.get().getWidth();
//                    int hh = (int)renderer.vncImage.get().getHeight();
//                    byte[] data = new byte[ww * hh * 4];
//                    vncReader.getPixels(0,0,ww,hh, PixelFormat.getByteBgraInstance(), data, 0, ww*4);
//
//                    tt = tgaTexture(ww, hh, true, data);
//
//                    texture = tt;
//
//                }
//
//                float xx = 0, yy = 0;
//                float sc = 1f;
//                if (tt!=null) {
//                    Draw.rectTex(gl, tt, xx, yy, sc, sc, 0);
//                } else {
//                    Draw.colorBipolar(gl, 0.5f);
//                    Draw.rect(gl, xx, yy, sc, sc);
//                }
//            }
//        };
//    }
//
//    public PixelBag newSensor(int pw, int ph) {
//        return new PixelBag(pw, ph) {
//
//            @Override public int sw() { return image==null ? 0 : (int) image.getWidth(); }
//
//            @Override public int sh() { return image==null ? 0 : (int) image.getHeight(); }
//
//            @Override public int rgb(int sx, int sy) { return vncReader.getArgb(sx, sy); }
//        };
//    }

    public static void main(String[] args) {
        Rdp v = new Rdp("localhost", 5901);

        //1.
        //v.newFXWindow();

        //2.
        //SpaceGraph.window(v.newSurface(), 800, 800);

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
