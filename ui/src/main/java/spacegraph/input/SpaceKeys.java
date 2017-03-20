package spacegraph.input;

import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import org.eclipse.collections.api.block.procedure.primitive.FloatProcedure;
import org.eclipse.collections.impl.map.mutable.primitive.IntBooleanHashMap;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.jetbrains.annotations.Nullable;
import spacegraph.SpaceGraph;
import spacegraph.render.JoglPhysics;

import java.util.function.Consumer;

/**
 * Created by me on 11/20/16.
 */
public abstract class SpaceKeys extends KeyAdapter implements Consumer<SpaceGraph> {

    public final JoglPhysics space;

    //TODO merge these into one Map
    final IntBooleanHashMap keyState = new IntBooleanHashMap();
    final IntObjectHashMap<FloatProcedure> keyPressed = new IntObjectHashMap();
    final IntObjectHashMap<FloatProcedure> keyReleased = new IntObjectHashMap();

    public SpaceKeys(SpaceGraph g) {
        this.space = g;


        g.addFrameListener(this);
    }

    @Override public void accept(SpaceGraph j) {
        float dt = j.getLastFrameTime();
        keyState.forEachKeyValue((k, s) -> {
            FloatProcedure f = (s) ? keyPressed.get(k) : keyReleased.get(k);
            if (f != null) {
                f.value(dt);
            }
        });
    }

    protected void watch(int keyCode, @Nullable FloatProcedure ifPressed, @Nullable FloatProcedure ifReleased) {
        keyState.put(keyCode, false); //initialized
        if (ifPressed != null)
            keyPressed.put(keyCode, ifPressed);
        if (ifReleased != null)
            keyReleased.put(keyCode, ifReleased);
    }

    //TODO unwatch

    @Override
    public void keyReleased(KeyEvent e) {
        setKey((int) e.getKeyCode(), false);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        setKey((int) e.getKeyCode(), true);
    }

    protected void setKey(int c, boolean state) {
        //TODO use a compute-like lambda to avoid duplicating lookup
        if (keyState.containsKey(c)) {
            keyState.put(c, state);
        }
    }
}
