package clojure.lang;




public class clojure {

    public static void main(String[] args) {

        System.out.println(new Dynajure().eval("(+ 1 2)"));

    }


//    public static String eval(String script) {
//        // We don't actually need the context object here, but we need it to have
//        // been initialized since the
//        // constructor for Ctx sets static state in the Clojure runtime.
//
//        Object result = Compiler.eval(RT.readString(script));
//
//        return RT.printString(result);
//    }

//    static class Ctx implements Serializable {
//        private static final long serialVersionUID = 1L;
//
//        Symbol USER = Symbol.create("user");
//        Symbol CLOJURE = Symbol.create("clojure.core");
//
//        transient Var in_ns;
//        transient Var refer;
//        transient Var ns;
//        transient Var compile_path;
//        transient Var warn_on_reflection;
//        transient Var print_meta;
//        transient Var print_length;
//        transient Var print_level;
//
//        Ctx() {
//            in_ns = RT.var("clojure.core", "in-ns");
//            refer = RT.var("clojure.core", "refer");
//            ns = RT.var("clojure.core", "*ns*");
//            compile_path = RT.var("clojure.core", "*compile-path*");
//            warn_on_reflection = RT.var("clojure.core", "*warn-on-reflection*");
//            print_meta = RT.var("clojure.core", "*print-meta*");
//            print_length = RT.var("clojure.core", "*print-length*");
//            print_level = RT.var("clojure.core", "*print-level*");
//
//            try {
//                Var.pushThreadBindings(RT.map(ns, ns.get(), warn_on_reflection,
//                        warn_on_reflection.get(), print_meta, print_meta.get(),
//                        print_length, print_length.get(), print_level, print_level.get(),
//                        compile_path, "classes"));
//                in_ns.invoke(USER);
//                refer.invoke(CLOJURE);
//            } catch (Exception e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            } finally {
//                Var.popThreadBindings();
//            }
//        }
//    }

}