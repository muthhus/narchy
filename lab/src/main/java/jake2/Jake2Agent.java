package jake2;

import boofcv.io.image.ConvertBufferedImage;
import com.jogamp.nativewindow.NativeSurface;
import com.jogamp.nativewindow.SurfaceUpdatedListener;
import com.jogamp.opengl.util.GLPixelBuffer;
import com.jogamp.opengl.util.GLReadBufferUtil;

import jake2.client.VID;
import jake2.client.refexport_t;
import jake2.game.PlayerView;
import jake2.game.edict_t;
import jake2.qcommon.Qcommon;
import jake2.render.Base;
import jake2.render.JoglGL2Renderer;
import jake2.render.opengl.JoglGL2Driver;
import jogamp.newt.WindowImpl;
import nars.NAR;
import nars.experiment.minicraft.PixelAutoClassifier;
import nars.remote.SwingAgent;
import nars.video.MatrixSensor;
import nars.video.PixelBag;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.function.Supplier;

import static jake2.game.PlayerView.current_player;
import static jake2.render.Base.vid;
import static nars.$.t;
import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 9/22/16.
 */
public class Jake2Agent extends SwingAgent implements Runnable {

    private final PixelAutoClassifier camAE;
    ByteBuffer seen = null;
    int width, height;
    boolean see = true;

    final int[] nBits = new int[]{8, 8, 8};
    final int[] bOffs1 = new int[]{2, 1, 0};

    final ColorSpace raster = ColorSpace.getInstance(1000);
    final ComponentColorModel colorModel = new ComponentColorModel(raster, nBits, false, false, 1, 0);

    final Supplier<BufferedImage> screenshotter = () -> {
        byte[] bb = seen.array();

        WritableRaster raster1 = Raster.createInterleavedRaster(
                new DataBufferByte(bb, bb.length), width, height, width * 3, 3, bOffs1, new Point(0, 0));

        BufferedImage b = new BufferedImage(colorModel, raster1, false, (Hashtable) null);
        return b;
    };

    public Jake2Agent(NAR nar) {
        super(nar, 1);


        MatrixSensor<PixelBag> qcam = addCamera("q", screenshotter, 64, 64, (v) -> t(v, alpha));
        qcam.src.vflip = true;

        camAE = new PixelAutoClassifier("cra", qcam.src.pixels, 16, 16, 16, this);
        window(camAE.newChart(), 500, 500);


        new Thread(this).start();
    }

    @Override
    protected float reward() {

        camAE.frame();

        edict_t p = PlayerView.current_player;
        if (p == null)
            return 0;

//        System.out.println(p.health + " " + p.speed + Arrays.toString(p.velocity) + " "
//        + p.client.weaponstate + " "
//        );
        //p.health

        return -(1f - ((float)p.health)/(p.max_health));
    }

    @Override
    public void run() {
        //http://aq2maps.quadaver.org/
        //https://www.quaddicted.com/reviews/
        //http://tastyspleen.net/~quake2/baseq2/maps/
        Jake2.run(new String[]{
                "+god mode", "+give all", "+map train"
                //"+connect .."
        }, this::onDraw);


        /*
        Outer Base		base1.bsp
        Installation		base2.bsp
        Comm Center	base3.bsp
        Lost Station		train.bsp
        Ammo Depot	bunk1.bsp
        Supply Station	ware1.bsp
        Warehouse		ware2.bsp
        Main Gate		jail1.bsp
        Destination Center	jail2.bsp
        Security Complex	jail3.bsp
        Torture Chambers	jail4.bsp
        Guard House	jail5.asp
        Grid Control		security.bsp
        Mine Entrance	mintro.asp
        Upper Mines		mine1.bsp
        Borehole		mine2.bsp
        Drilling Area		mine3.bsp
        Lower Mines		mine4.bsp
        Receiving Center	fact1.bsp
        Sudden Death	fact3.bsp
        Processing Plant	fact2.bsp
        Power Plant		power1.bsp
        The Reactor		power2.bsp
        Cooling Facility	cool1.bsp
        Toxic Waste Dump	waste1.bsp
        Pumping Station 1	waste2.bsp
        Pumping Station 2	waste3.bsp
        Big Gun		biggun.bsp
        Outer Hangar	hangar1.bsp
        Comm Satelite	space.bsp
        Research Lab	lab.bsp
        Inner Hangar	hangar2.bsp
        Launch Command	command.bsp
        Outlands		strike.bsp
        Outer Courts	city1.bsp
        Lower Palace	city2.bsp
        Upper Palace	city3.bsp
        Inner Chamber	boss1.bsp
        Final Showdown	boss2.bsp


        Read more: http://www.cheatcodes.com/quake-2-pc/#ixzz4L7BYreED
        Under Creative Commons License: Attribution Non-Commercial No Derivatives
        Follow us: @CheatCodes on Twitter | CheatCodes on Facebook

         */

    }

    protected synchronized void onDraw() {

        refexport_t r = Globals.re;
        JoglGL2Renderer renderer = (JoglGL2Renderer) r;

        if (see) {
            if (seen != null) {
                seen.rewind();
            }

            width = vid.getWidth();
            height = vid.getHeight();
            seen = ((Base) renderer.impl).see(seen);
        }

    }



    public static void main(String[] args) {
        SwingAgent.run(Jake2Agent::new, 66500);
    }
}


