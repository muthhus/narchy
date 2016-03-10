package nars.guifx.annotation;

import javafx.beans.property.SimpleObjectProperty;
import nars.guifx.graph2.layout.Grid;

/**
 * Annotate with @Implementation
 */
public class ImplementationProperty<C> extends SimpleObjectProperty<Class<? extends C>> {


    public ImplementationProperty() {
        super();
    }
    public ImplementationProperty(Class c) {
        super(c);
    }

    public C getInstance() {
        Class<? extends C> lc = get();
        if (lc != null) {
            try {
                C il = lc.newInstance();
                return il;
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        }
        return null;
    }

}
