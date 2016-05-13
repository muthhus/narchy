package nars.rover.obj;

import com.artemis.Component;
import nars.NAR;

/**
 * Created by me on 3/29/16.
 */
public class RunNAR extends Component {

    public NAR nar;

    public RunNAR() {

    }

    public RunNAR(NAR nar) {
        this.nar = nar;
    }

}
