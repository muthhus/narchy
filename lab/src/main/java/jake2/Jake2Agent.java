package jake2;

import jake2.client.VID;
import jake2.client.refexport_t;
import nars.NAR;
import nars.remote.SwingAgent;

/**
 * Created by me on 9/22/16.
 */
public class Jake2Agent extends SwingAgent {

    public Jake2Agent(NAR nar) {
        super(nar, 1);

        /*
        //
//        Thread game = j.spawnMain("jake2.Jake2", "/home/me/jake2",
//            "+map neighborhood" //selects level and begins game, get more: http://aq2maps.quadaver.org/
//        );
//
//        Thread.sleep(5000); //wait for game to load, TODO: poll a state variable to know when loaded
//
//        String clientInput = "@jake2.client.CL_input"; //client input interface class
//        for (int i = 0; i < 10; i++) {
//            j.eval(clientInput + "@IN_ForwardDown()"); Thread.sleep(100);
//            j.eval(clientInput + "@IN_ForwardUp()"); Thread.sleep(100);
//        }
         */

        //http://aq2maps.quadaver.org/
        //https://www.quaddicted.com/reviews/
        //http://tastyspleen.net/~quake2/baseq2/maps/
        Jake2.main(new String[] {
            //"+map disaster"
            //"+connect .."
        });

        new Thread(()-> {
            refexport_t r = Globals.re;

        } );

    }

    @Override
    protected float reward() {
        return 0;
    }

    public static void main(String[] args) {
        SwingAgent.run(Jake2Agent::new, 500);
    }
}


