package prologEngine;

import java.util.stream.Stream;

public class Main {

  static void println(final Object o) {
    System.out.println(o);
  }

  static void pp(final Object o) {
    System.out.println(o);
  }

  public static void run(final String fname0) {
    final boolean p = true;

    final String fname = fname0 + ".nl";
    Engine P;

    if (p) {
      P = new Prog(fname);
      pp("CODE");
      ((Prog) P).ppCode();
    } else {
      P = new Engine(fname);
    }

    pp("RUNNING");
    final long t1 = System.nanoTime();
    P.run();
    final long t2 = System.nanoTime();
    System.out.println("time=" + (t2 - t1) / 1000000000.0);

  }

  public static void srun(final String fname0) {
    final String fname = fname0 + ".nl";
    final Prog P = new Prog(fname);

    pp("CODE");
    P.ppCode();

    pp("RUNNING");
    final long t1 = System.nanoTime();

    final Stream<Object> S = P.stream();
    S.forEach(x -> Main.pp(P.showTerm(x)));

    final long t2 = System.nanoTime();
    System.out.println("time=" + (t2 - t1) / 1000000000.0);
  }

  public static void main(final String[] args) {

    final String path = "/Users/tarau/Desktop/go/IP/iProlog/src/progs/";
    String fname;
    if (args.length == 0)
      fname = path + "perms.pl";
    else
      fname = path + args[0];
    run(fname);
  }
}
