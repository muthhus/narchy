package spacegraph.obj;

import com.google.common.collect.Lists;
import spacegraph.Surface;

import java.util.List;

/**
 * Created by me on 7/20/16.
 */
abstract public class LayoutSurface extends Surface {


    public LayoutSurface(Surface... children) {
        this(Lists.newArrayList(children));
    }

    public LayoutSurface(List<? extends Surface> children) {
        setChildren(children);
    }

    @Override
    abstract protected void layout();
}
