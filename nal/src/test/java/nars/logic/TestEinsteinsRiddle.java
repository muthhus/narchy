package nars.logic;

import nars.NAR;
import nars.Narsese;
import nars.derive.Deriver;
import nars.derive.DeriverTransform;
import nars.derive.TrieDeriver;
import nars.nar.Default;
import org.junit.Test;

import java.io.IOException;
import java.net.URL;

/**
 * Created by me on 4/17/17.
 */
public class TestEinsteinsRiddle {

    @Test
    public void testRiddle1() throws IOException, Narsese.NarseseException {
        NAR n = new Default() {
//            @Override
//            public Deriver newDeriver() {
//                return new DeriverTransform(b -> new DeriverTransform.TracedBoolPredicate(b))
//                        .apply((TrieDeriver) super.newDeriver());
//            }
        };
        n.termVolumeMax.setValue(1024);
        n.log();
        URL resource = TestEinsteinsRiddle.class.getResource("einsteinsRiddle.nal");
        n.inputNarsese(
            resource.openStream()
        );
        n.run(64);



    }


}
