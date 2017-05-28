package nars.gui.graph.run;

import jcog.net.UDPeer;
import jcog.net.UDPeerSim;
import jcog.pri.RawPLink;
import nars.$;
import nars.concept.Concept;
import nars.conceptualize.DefaultConceptBuilder;
import nars.control.ConceptFire;
import nars.gui.Vis;
import nars.gui.graph.ConceptSpace;
import nars.gui.graph.ConceptWidget;
import nars.index.term.map.MapTermIndex;
import nars.nar.Default;
import nars.time.CycleTime;
import nars.util.exe.BufferedSynchronousExecutor;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.Surface;
import spacegraph.math.v2;
import spacegraph.phys.Collidable;
import spacegraph.phys.collision.ClosestRay;
import spacegraph.render.JoglPhysics;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import static jcog.Texts.i;
import static spacegraph.SpaceGraph.window;

/**
 * represents a live UDP mesh network as concepts and links
 */
public class SimpleUDPeerGraph {

    public static void main(String[] args) throws IOException {

        Default n = new Default(
                new MapTermIndex(new DefaultConceptBuilder(), new ConcurrentHashMap<>()),
                new CycleTime(), new BufferedSynchronousExecutor(512, 0.5f));

        int population = 256;

        SimpleConceptGraph1 s = new SimpleConceptGraph1(n,
                () -> (((BufferedSynchronousExecutor) (n.exe)).active)
                        .stream()
                        .map(x -> x instanceof ConceptFire ? ((ConceptFire) x) : null)
                        .filter(Objects::nonNull)
                        .iterator()
                /* TODO */, population+1, population+1, population/2, population/2);

        new SpaceGraph(s).camPos(0,0,200).show(800, 800);

        UDPeerSim u = new UDPeerSim(population) {

            int WorldX = 300;
            int WorldY = 200;
            float KmPerSec = 1000;

            final List<v2> locations = $.newArrayList();

            {
                for (int i = 0; i < peer.length; i++) {
                    locations.add(new v2(n.random().nextFloat() * WorldX - WorldX/2, n.random().nextFloat() * WorldY - WorldY/2));
                    peer[i].them.capacity(8);
                }

                s.nodeBuilder = (x) -> {
                    int i = i(x.term().toString()) - 10000;
                    return new ConceptWidget(x) {
                        final MyUDPeer pp = peer[i];
                        v2 p = locations.get(i);

                        @Override
                        public Surface onTouch(Collidable body, ClosestRay hitPoint, short[] buttons, JoglPhysics space) {
                            //if (buttons!=null && buttons.length > 0) {
                                pp.packetLossRate.setValue(1f);
                            //} else {
                             //   pp.packetLossRate.setValue(0.05f); //back to normal
                            //}
                            return super.onTouch(body, hitPoint, buttons, space);
                        }

                        @Override
                        public void commit(ConceptVis conceptVis, ConceptSpace space) {
                            move(p.x, p.y, 0); //fix at its location
                            super.commit(conceptVis, space);
                        }
                    };
                };
            }


            @Override
            protected long delay(InetSocketAddress from, InetSocketAddress to, int length) {
                int a = from.getPort() - 10000; /* HACK */
                int b = to.getPort() - 10000;

                return Math.round(
                        new v2().sub(locations.get(a), locations.get(b)).length() / (KmPerSec / 1000f /* ms */)
                );
            }

            @Override
            public void onTell(@Nullable UDPeer.UDProfile sender, UDPeer recv, UDPeer.Msg msg) {

                @Nullable Concept from = n.conceptualize($.the(msg.port()));
                @Nullable Concept to = n.conceptualize($.the(recv.port()));
                float p = msgPri(msg);
                from.termlinks().put(new RawPLink(to, p));
                from.termlinks().commit();

                //mirror the routing table by removing missing entries from it from the termlinks
                to.termlinks().forEach(l -> {
                    if (!recv.them.contains(i(l.get().toString()) - 10000))
                        l.delete();
                });


                n.input(new ConceptFire(from, 0.5f + p / 2f));
                n.input(new ConceptFire(to, 0.5f + p / 2f));

                //System.out.println(from + " " + to.me + " " + m.edgeValueOrDefault(from, to.me, null));
            }

            private float msgPri(UDPeer.Msg m) {
                return 0.001f;
            }


        };

        u.start(1);

        n.onCycle(() -> {
            u.pingRandom(1);
            u.tellSome(n.random().nextInt(u.peer.length), 2, 2);
        });

        n.startFPS(4);
    }
}
