//package nars.web;
//
//import nars.$;
//import nars.NAR;
//import nars.nar.Default;
//import nars.term.Term;
//import org.rapidoid.annotation.Controller;
//import org.rapidoid.annotation.GET;
//import org.rapidoid.config.Conf;
//import org.rapidoid.gui.Debug;
//import org.rapidoid.gui.GUI;
//import org.rapidoid.gui.Grid;
//import org.rapidoid.http.fast.HttpWrapper;
//import org.rapidoid.http.fast.On;
//import org.rapidoid.http.fast.ServerSetup;
//import org.rapidoid.http.fast.ViewRenderer;
//import org.rapidoid.model.Items;
//import org.rapidoid.model.Models;
//import org.rapidoid.web.Rapidoid;
//
///**
// * http://www.rapidoid.org/http-fast.html
// */
//public class TestRapidoid {
//
//    @Controller
//    public static class NARController {
//
//        final NAR nar = new Default();
//
//        @GET
//        public NAR getNAR() {
//            return nar;
//        }
//    }
//
//    public static void main(String[] args) {
//
//        Conf.args("cpus=2", "threads=8");
//
//        NAR nar = new Default();
//        nar.input("<c-->d>!");
//        nar.step();
//
//        On.address("127.0.0.1")
//            .port(9999)
//            .page("/").gui(
//                GUI.page(
//                    GUI.containerFluid(
//                       GUI.grid(Models.beanItemsInfer($.$("<a-->b>")), null, 10),
//                       GUI.grid(Models.beanItemsInfer(nar.concept("<c-->d>")), null, 10)
//                    )
//                )
//            ).listen();
//
//    }
//}
