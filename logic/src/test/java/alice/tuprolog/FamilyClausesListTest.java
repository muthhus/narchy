package alice.tuprolog;

import org.junit.jupiter.api.Test;

import java.util.ListIterator;

public class FamilyClausesListTest {
    // Short test about the new implementation of the ListItr
    // Alessandro Montanari - alessandro.montanar5@studio.unibo.it


    @Test
    public void test1() {
        FamilyClausesList clauseList = new FamilyClausesList();
        ClauseInfo first = new ClauseInfo(new Struct(new Struct("First"), new Struct("First")), "First Element");
        ClauseInfo second = new ClauseInfo(new Struct(new Struct("Second"), new Struct("Second")), "Second Element");
        ClauseInfo third = new ClauseInfo(new Struct(new Struct("Third"), new Struct("Third")), "Third Element");
        ClauseInfo fourth = new ClauseInfo(new Struct(new Struct("Fourth"), new Struct("Fourth")), "Fourth Element");

        clauseList.add(first);
        clauseList.add(second);
        clauseList.add(third);
        clauseList.add(fourth);

        // clauseList = [First, Second, Third, Fourh]

        ListIterator<ClauseInfo> allClauses = clauseList.listIterator();
        // Get the first object and remove it
        allClauses.next();


//        allClauses.remove();
//        if (clauseList.contains(first)) {
//            System.out.println("Error!");
//            System.exit(-1);
//        }

        // First object removed
        // clauseList = [Second, Third, Fourh]

        // Get the second object
        allClauses.next();
        // Get the third object
        allClauses.next();
        // Get the third object
        allClauses.previous();
        // Get the second object and remove it
        allClauses.previous();
//        allClauses.remove();
//        if (clauseList.contains(second)) {
//            System.out.println("Error!");
//            System.exit(-2);
//        }

        // clauseList = [Third, Fourh]

        System.out.println("Ok!!!");
    }


}