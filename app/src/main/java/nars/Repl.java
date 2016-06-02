//package nars;
//
//import nars.nar.Default;
//
//import java.io.IOException;
//
///**
// * TODO
// *      add volume +/- hotkeys that throttle output
// *      and throttle CPU
// *
// */
//public class Repl {
//
//    public Repl(NAR nar) throws IOException {
//
//
//        ConsoleReader reader = new ConsoleReader();
//        reader.setHistoryEnabled(true);
//        reader.setCopyPasteDetection(true);
//
//
//        nar.logSummaryGT(reader.getOutput(), 0.1f);
//
//        NARLoop loop = nar.loop(20);
//
//        String line;
//        do {
//            line = reader.readLine("$ ");
//            if (line!=null && !line.isEmpty())
//                nar.input(line);
//        }
//        while (line != null && line.length() > 0);
//    }
//
//    public static void main(String[] args) throws IOException {
//        new Repl(new Default());
//    }
//}
