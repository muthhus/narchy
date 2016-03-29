package clojure.lang;

import static clojure.lang.RT.*;

/**
 * Created by me on 3/9/16.
 */
public class Dynajure {

    public static final ClassLoader PARENT = baseLoader();

    final ClassLoader cl = new DynamicClassLoader(PARENT);
    static {
        Var.intern(CLOJURE_NS, Symbol.intern("*allow-unresolved-vars*"), T).setDynamic();
    }

    public Object eval(String s) {
        return eval(RT.readString(s));
    }

    public Object eval(Object form) {
        return Compiler.eval(form, cl);
    }


}
