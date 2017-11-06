package spacegraph.layout;

import spacegraph.Surface;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class MutableLayout extends Layout {

    public final CopyOnWriteArrayList<Surface> children = new Children();


    public MutableLayout(Surface... children) {
        super();
        set(children);
    }

    public MutableLayout(List<Surface> children) {
        set(children);
    }

    @Override
    public void doLayout() {
        children.forEach(Surface::layout);
    }

    public Layout set(Surface... next) {
        if (!equals(this.children, next)) {
            synchronized (mustLayout) {
                children.clear();
                for (Surface c : next) {
                    if (c != null)
                        children.add(c);
                }
            }
        }
        return this;
    }

    public Layout set(List<Surface> next) {
        if (!equals(this.children, next)) {
            synchronized (mustLayout) {
                children.clear();
                children.addAll(next);
            }
        }
        return this;
    }

    @Override
    public void stop() {
        children.clear();
        super.stop();
    }

    @Override
    public void forEach(Consumer<Surface> o) {
        children.forEach(o);
    }
    private class Children extends CopyOnWriteArrayList<Surface> {
        @Override
        public boolean add(Surface surface) {
            synchronized (mustLayout) {
                if (!super.add(surface)) {
                    return false;
                }
                if (surface != null) {
                    surface.start(MutableLayout.this);
                    layout();
                }
            }
            return true;
        }

        @Override
        public Surface set(int index, Surface neww) {
            Surface old;
            synchronized (mustLayout) {
                while (size() <= index) {
                    add(null);
                }
                old = super.set(index, neww);
                if (old == neww)
                    return neww;
                else {
                    if (old != null) {
                        old.stop();
                    }
                    if (neww != null) {
                        neww.start(MutableLayout.this);
                    }
                }
            }
            layout();
            return old;
        }

        @Override
        public boolean addAll(Collection<? extends Surface> c) {
            synchronized (mustLayout) {
                for (Surface s : c)
                    add(s);
            }
            layout();
            return true;
        }

        @Override
        public Surface remove(int index) {
            Surface x;
            synchronized (mustLayout) {
                x = super.remove(index);
                if (x == null)
                    return null;
                x.stop();
            }
            layout();
            return x;
        }

        @Override
        public boolean remove(Object o) {
            synchronized (mustLayout) {
                if (!super.remove(o))
                    return false;
                ((Surface) o).stop();
            }
            layout();
            return true;
        }


        @Override
        public void add(int index, Surface element) {
            synchronized (mustLayout) {
                super.add(index, element);
                element.start(MutableLayout.this);
            }
            layout();
        }

        @Override
        public void clear() {
            synchronized (mustLayout) {
                this.removeIf(x -> {
                    x.stop();
                    return true;
                });
            }
            layout();
        }
    }

}
