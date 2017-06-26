package nars.nal.multistep;

import nars.NAR;
import nars.Narsese;
import nars.nar.NARBuilder;
import org.junit.Test;

/**
 * https://en.wikipedia.org/wiki/STRIPS
 */
public class STRIPSTest {

    /*
    A monkey is at location A in a lab. There is a box in location C. The monkey wants the bananas that are hanging from the ceiling in location B, but it needs to move the box and climb onto it in order to reach them.

    Initial state: At(A), Level(low), BoxAt(C), BananasAt(B)
    Goal state:    Eat(bananas)

    Actions:
               // move from X to Y
               _Move(X, Y)_
               Preconditions:  At(X), Level(low)
               Postconditions: not At(X), At(Y)

               // climb up on the box
               _ClimbUp(Location)_
               Preconditions:  At(Location), BoxAt(Location), Level(low)
               Postconditions: Level(high), not Level(low)

               // climb down from the box
               _ClimbDown(Location)_
               Preconditions:  At(Location), BoxAt(Location), Level(high)
               Postconditions: Level(low), not Level(high)

               // move monkey and box from X to Y
               _MoveBox(X, Y)_
               Preconditions:  At(X), BoxAt(X), Level(low)
               Postconditions: BoxAt(Y), not BoxAt(X), At(Y), not At(X)

               // take the bananas
               _TakeBananas(Location)_
               Preconditions:  At(Location), BananasAt(Location), Level(high)
               Postconditions: Eat(bananas)

     */
    @Test
    public void testBanana1() throws Narsese.NarseseException {
        NAR n = new NARBuilder().get();
        n.log();
        n.input(
                "At(A). :|:",
                "Level(low). :|:",
                "BoxAt(C). :|:",
                "BananasAt(B). :|:",

                "Eat(bananas)!",

                //_Move(X, Y)_
                "((At($X) &&+0 Level(low)) ==>+1 (--At($X) &&+0 At(#Y))).",

                // _ClimbUp(Location)_ climb up the box
                "( ((At(Location) &&+0 BoxAt(Location)) &&+0 Level(low)) ==>+1 (Level(high) &&+0 --Level(low)))."
        );
        n.run(10);

    }
}
