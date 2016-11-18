package spacegraph.obj.layout;

import com.google.common.collect.Lists;
import spacegraph.Surface;

import java.util.List;

/**
 * Created by me on 7/20/16.
 */
abstract public class Layout extends Surface {


    public Layout(Surface... children) {
        this(Lists.newArrayList(children));
    }

    public Layout(List<Surface> children) {
        setChildren(children);
    }


}
