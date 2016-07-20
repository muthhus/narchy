package nars.nal.nal8;


import nars.Param;
import nars.nar.Default;
import nars.nar.Terminal;
import org.junit.Test;

public class MathTest {

    @Test
    public void testAdd1() {
        Terminal t = new Terminal();
        t.log();
        t.input("(add(1,2,?x)<->result).");
        t.next();
    }
    @Test
    public void testImplVarAdd1() {
        Param.DEBUG = true;
        Default t = new Default();
        t.log();
        //t.input("i:{0,1,2,3,4}.");
        //t.input("i:{0,1,2}.");
        t.input("i:{1}.");
        t.input("i:{2}.");
        t.input("i:{4}.");
        t.input("((&&,({$x} --> i),({$y} --> i)) ==> ({($x,$y),($y,$x)} --> j)).");
        t.run(1000);

        t.input("(({(#x,#y)} --> j) ==> ({add(#x,#y,?z)} --> i)).");
        t.run(1000);

        //t.input("(({#x,#y} --> i) && (add(#x,#y)<->sum)).");
        //t.input("(({$x,$y} --> i) ==> (add($x,$y)<->sum)).");
        //t.input("(&&,({$x} --> i),({$y} --> i),({add($x,$y)}-->i)).");
        ///t.input("(((#x --> i)&&(#y --> i)) ==> (add(#x,#y)<->sum)).");
        //t.input("((($x --> i)&&($y --> i)) ==> (add($x,$y)<->sum)).");
        //t.input("(({?x,?y} --> i) ==> (add(?x,?y)<->sum))?");
        //t.input("({?x}-->i)?");

    }
}
