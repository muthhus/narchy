package nars.gui.graph.run;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.MutableValueGraph;
import com.google.common.graph.ValueGraphBuilder;
import jcog.Util;
import jcog.net.UDPeer;
import jcog.net.UDPeerSim;
import nars.$;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

public class SimpleUDPeerGraph {

    public static void main(String[] args) throws IOException {
        SimpleGraph1 s = new SimpleGraph1(15);
        s.show(1200, 600);

        final MutableValueGraph<Integer,MutableFloat> m = ValueGraphBuilder.directed().allowsSelfLoops(false).build();

        UDPeerSim u = new UDPeerSim(16) {

            @Override public void onTell(@Nullable UDPeer.UDProfile sender, UDPeer to, UDPeer.Msg msg) {
                synchronized (m) {


                    int from = msg.id();
                    MutableFloat p = m.edgeValueOrDefault(from, to.me, null);
                    if (p == null) {
                        p = new MutableFloat();
                        m.putEdgeValue(from, to.me, p);
                    }
                    p.add( activation(msg) );

                    //System.out.println(from + " " + to.me + " " + m.edgeValueOrDefault(from, to.me, null));
                }
            }

            private float activation(UDPeer.Msg m) {
                return 0.5f;
            }
        };


        for (UDPeer x : u.peer) {
            synchronized (m) {
                m.addNode(x.me);
            }
        }
        u.start(4f);
        u.pingRing(2);

        while (true) {
            synchronized (m) {
                s.commit(m, MutableFloat::floatValue);
            }

            u.tellSome(s.nar.random().nextInt(u.peer.length), 32, 2);

            Util.sleep(50);
        }

    }
}
