package jake2;

import jake2.client.CL_input;
import jake2.client.refexport_t;
import jake2.game.PlayerView;
import jake2.game.edict_t;
import jake2.render.Base;
import jake2.render.JoglGL2Renderer;
import jake2.sys.IN;
import nars.NAR;
import nars.experiment.minicraft.PixelAutoClassifier;
import nars.remote.SwingAgent;
import nars.util.signal.NObj;
import nars.video.Sensor2D;
import nars.video.PixelBag;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.nio.ByteBuffer;
import java.util.Hashtable;
import java.util.function.Supplier;

import static jake2.Globals.*;
import static jake2.render.Base.vid;
import static nars.$.t;
import static spacegraph.SpaceGraph.window;

/**
 * Created by me on 9/22/16.
 */
public class Jake2Agent extends SwingAgent implements Runnable {

    private PixelAutoClassifier camAE = null;
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

    public static class PlayerData {
        public float health;
        public float velX;
        public float velY;
        public float velZ;
        public float speed;
        public int weaponState;
        public short frags;
        public float angle;

        protected void update() {
            edict_t p = PlayerView.current_player;
            if (p==null) return;

            health = p.health;
            weaponState = p.client.weaponstate;

            frags = p.client.ps.stats[STAT_FRAGS];


            angle = p.angle;

            float[] v = p.velocity;
            velX = v[0];
            velY = v[1];
            velZ = v[2];
            speed = (float) Math.sqrt(velX*velX+velY*velY+velZ*velZ );

        }
    }
    final PlayerData player = new PlayerData();

    public Jake2Agent(NAR nar) {
        super(nar, 1);


        Sensor2D<PixelBag> qcam = addCamera("q", screenshotter, 64, 64, (v) -> t(v, alpha));
        qcam.src.vflip = true;

//        camAE = new PixelAutoClassifier("cra", qcam.src.pixels, 16, 16, 32, this);
//        window(camAE.newChart(), 500, 500);

        new NObj("p", player, nar).readAllFields(false).into(this);

        actionToggle("(fore)", (x) -> CL_input.in_forward.state = x ? 1 : 0);
        actionToggle("(back)", (x) -> CL_input.in_back.state = x ? 1 : 0);

        //actionToggle("(left)", (x) -> CL_input.in_left.state = x ? 1 : 0);
        //actionToggle("(right)", (x) -> CL_input.in_right.state = x ? 1 : 0);
        actionToggle("(moveleft)", (x) -> CL_input.in_moveleft.state = x ? 1 : 0);
        actionToggle("(moveright)", (x) -> CL_input.in_moveright.state = x ? 1 : 0);
        actionToggle("(jump)", (x) -> CL_input.in_up.state = x ? 1 : 0);
        actionBipolar("(lookyaw)", (x) -> {
            float yawSpeed = 10;
            cl.viewangles[Defines.YAW] += yawSpeed * x;
            //return CL_input.in_lookup.state = x ? 1 : 0;
            return true;
        });
        actionBipolar("(lookpitch)", (x) -> {
            float pitchSpeed = 20; //absolute
            cl.viewangles[Defines.PITCH] = pitchSpeed * x;
            //return CL_input.in_lookup.state = x ? 1 : 0;
            return true;
        });
        //actionToggle("(lookdown)", (x) -> CL_input.in_lookdown.state = x ? 1 : 0);
        actionToggle("(attak)", (x) -> CL_input.in_attack.state = x ? 1 : 0);

        new Thread(this).start();
    }

    @Override
    protected float act() {

        if (camAE!=null)
            camAE.frame();

        player.update();

        return player.health * 4f + player.speed/2f + player.frags * 2f;
    }

    @Override
    public void run() {
        //http://aq2maps.quadaver.org/
        //https://www.quaddicted.com/reviews/
        //http://tastyspleen.net/~quake2/baseq2/maps/
        //https://www.eecis.udel.edu/~portnoi/quake/quakeiicom.html
        IN.mouse_avail = false;
        Jake2.run(new String[]{
                "+god",
                //"+debuggraph",
                //"+give all",
                //"+use chaingun",
                //"+mlook 0", //disable mouse
                //"+in_initmouse 0",
                //"+in_mouse 0",
                "+cl_gun 0", //hide gun
                "+timescale 0.5",
                "+map base1"
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

        refexport_t r = re;
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
        SwingAgent.run(Jake2Agent::new, 100000);
    }
}


