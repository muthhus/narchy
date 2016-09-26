package nars.gui;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.opengl.GL2;
import nars.$;
import nars.NAR;
import nars.nar.Default;
import nars.test.DeductiveChainTest;
import nars.test.DeductiveMeshTest;
import nars.util.Texts;
import nars.util.time.Between;
import nars.util.time.IntervalTree;
import spacegraph.ForceDirected;
import spacegraph.SimpleSpatial;
import spacegraph.SpaceGraph;
import spacegraph.Spatial;
import spacegraph.layout.Flatten;
import spacegraph.obj.RectWidget;
import spacegraph.obj.XYSlider;

import java.util.*;
import java.util.function.Function;


public class TimeSpace extends NARSpace implements NARSpace.TriConsumer<NAR, SpaceGraph, List<Spatial>>, Function<Object,Spatial> {

    final IntervalTree<Long,Object> data = new IntervalTree<>();
    private final int capacity;
    private SortedSet<Between<Long>> keys;
    private long minT, maxT;
    float dx = 1f;
    private long now;

    public static void main(String[] args) {

        Default n = new Default(1024, 4, 2, 2);
        //n.nal(4);


        new DeductiveMeshTest(n, new int[]{4, 4}, 16384);
        new DeductiveChainTest(n, 10, 9999991, (x, y) -> $.p($.the(x), $.the(y)));

        //new ArithmeticInduction(n);

        newTimeWindow(n, 32);

        //n.run(20); //headstart

        n.loop(25f);

    }

    public static GLWindow newTimeWindow(Default n, int capacity) {

        SpaceGraph s = new SpaceGraph(
                new TimeSpace(n, capacity).with(
                        new Flatten()
                        //new Spiral()
                        //new FastOrganicLayout()
                )
        );
        ForceDirected forceDirect = new ForceDirected();
        forceDirect.repelSpeed = 0.5f;
        s.dyn.addBroadConstraint(forceDirect);


        return s.show(1300, 900);
    }


    public TimeSpace(NAR n, int capacity) {
        super(n, null, capacity);
        this.capacity = capacity;

    }


    @Override
    public Spatial apply(Object o) {
        if (o instanceof FloatValues) {
            RectWidget s = new RectWidget(o, new XYSlider(), 1, 1) {

            };

            Value vv = (Value) o;
            //Draw.hsb(vv.id.hashCode()/1000f, 0.5f, 0.5f, 0.8f, shapeColor);
            return s;

        } else if (o instanceof Value) {
            SimpleSpatial s = new SimpleSpatial(o) {

                @Override
                protected void renderRelativeAspect(GL2 gl) {
                    renderLabel(gl, 0.001f);
                }

            };
            Value vv = (Value) o;
            //s.colorShape(vv.id.hashCode()/1000f, 0.5f, 0.5f, 0.8f);
            return s;
        }
        return null;
    }

    abstract public static class Value implements Comparable<Value> {
        public final String id;
        public long start, end;

        public Value(String id) {
            this.id = id;
        }
        public Value(String id, long when) {
            this(id);
            this.start = when-1;
            this.end = when;
        }

        @Override
        public String toString() {
            return id + " " + valueString();
        }

        abstract public String valueString();

        @Override
        public int compareTo(Value o) {
            return id.compareTo(o.id);
        }

        @Override
        public boolean equals(Object _obj) {
            if (this == _obj) return true;
            Value obj = (Value)_obj;
            return id.equals(obj);
        }


    }

    public static final class ObjectValue extends Value {
        public Object value;

        public ObjectValue(String id, long when, Object value) {
            super(id, when);
            this.value = value;
        }

        @Override
        public String valueString() {
            return value.toString();
        }

    }
    public static final class FloatValue extends Value {
        public float value;

        public FloatValue(String id, long when, float value) {
            super(id, when);
            this.value = value;
        }

        public FloatValue(String id, long when, double value) {
            this(id, when, (float)value);
        }

        @Override
        public String valueString() {
            return Texts.n4(value);
        }

    }

    @Override
    public void accept(NAR nar, SpaceGraph space, List<Spatial> target) {

        limit(); //TODO merge limit and collect into one iterator

        collect(nar);

        List<Spatial> next = materialize(space, target, minT, now);

        layout(next);

//        Bag<Concept> x = ((Default) nar).core.concepts;
//        x.topWhile(b -> {
//
//            final float initDistanceEpsilon = 10f;
//            final float initImpulseEpsilon = 25f;
//
//            ConceptWidget w = space.update(b.get().term(),
//                    t -> new ConceptWidget(t, maxEdges, nar) {
//                        @Override
//                        public Dynamic newBody(CollisionShape shape, boolean collidesWithOthersLikeThis) {
//                            Dynamic x = super.newBody(shape, collidesWithOthersLikeThis);
//
//                            //place in a random direction
//                            x.transform().set(SpaceGraph.r(initDistanceEpsilon),
//                                    SpaceGraph.r(initDistanceEpsilon),
//                                    SpaceGraph.r(initDistanceEpsilon));
//
//                            //impulse in a random direction
//                            x.impulse(v(SpaceGraph.r(initImpulseEpsilon),
//                                    SpaceGraph.r(initImpulseEpsilon),
//                                    SpaceGraph.r(initImpulseEpsilon)));
//
//                            return x;
//                        }
//                    });
//
//            w.pri = b.priIfFiniteElseZero();
//
//
//            target.add(w);
//
//            return true;
//
//        }, maxNodes);

    }

    private void layout(List<Spatial> next) {

        next.forEach((Spatial s) -> {
            float speed = 0.2f;

            if (s instanceof SimpleSpatial) {
                SimpleSpatial<Value> ss = (SimpleSpatial)s;
                Value v = ss.key;

                //s.scale(dx*0.95f,dx/4f,0.5f);

                long start = v.start;
                long stop = v.end;
                double center = (start + stop)/2.0;
                double range = Math.max(stop - start, 0.5);
                ss.scale((float)(range * dx*0.95f),dx,1f);
                ss.moveX(x(center), speed); //align X-coordinate as time
            }
        });
    }

    private List<Spatial> materialize(SpaceGraph space, List<Spatial> target, long start, long end) {

        data.forEachContainedBy(start-1, end+1, (b, v) -> {
            Spatial w = space.update(v, this);
            target.add(w);
        });
        return target;
    }

    protected float x(double when) {
        return (float)((when - maxT) * dx);
    }

    public void limit() {
        if (!data.isEmpty()) {
            keys = data.keySetSorted();


            long start = keys.first().low;

            if (data.size() > capacity) {
                Iterator<Between<Long>> l = keys.iterator();
                do {
                    Between<Long> kk = l.next();
                    data.removeContainedBy(new Between(start-1, kk.high));
                    l.remove();
                } while (l.hasNext() && (data.size() > capacity));

                //keys = data.keySetSorted(); //HACK

            }

            minT = keys.first().low;
            maxT = keys.last().high;


        } else {
            keys = Collections.emptySortedSet();
            minT = maxT = 0;
        }
    }

    public void collect(NAR nar) {
        this.now = nar.time();


        set("happy", nar.emotion.happy.getAverage());
        set("busy", nar.emotion.busy.getAverage());
        if (Math.random() < 0.1)
            data.put(now, new FloatValue("busyX", now, nar.emotion.busy.getAverage()));
        if (Math.random() < 0.1)
            set("what", nar.self);

    }

    final Map<String,FloatValues> trend = new HashMap();

    public class FloatValues extends Value {
        final ArrayDeque<double[]> data = new ArrayDeque();
        final int capacity = 32;

        private Between<Long> interval;

        public FloatValues(String key, long start) {
            super(key);
            setInterval(start, start);
        }

        @Override
        public String valueString() {
            return id.toString();
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

        public void next(long t, double v) {
            while (data.size() + 1 > capacity)
                data.removeFirst();
            data.add(new double[] { t, v });

            //re-index
            TimeSpace.this.data.remove(this);
            long start = (long) data.getFirst()[0];
            long end = (long) data.getLast()[0];
            setInterval(start, end);
            TimeSpace.this.data.put(interval, this);
        }

        protected void setInterval(long start, long end) {
            this.start = start;
            this.end = end;
            this.interval = new Between<>(start, end);
        }

    }

    protected void set(String key, double value) {
        FloatValues m = trend.computeIfAbsent(key, k -> {
            FloatValues f = new FloatValues(key, now);
            data.put(f.interval, f);
            return f;
        });
        m.next(now, value);
    }

    protected void set(String key, Object value) {
        data.put(now, new ObjectValue(key, now, value));
    }


}
