package il.technion.tinytable;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TinyCountingTableUnitTest {

    @Test
    public void testStoreBasic() {
        int itemSize = 6;
        int bucketCapacity = 40;
        int nrBuckets = 1;
        TinyCountingTable tt = new TinyCountingTable(itemSize, bucketCapacity, nrBuckets);
        // requires 2 counter items.
        tt.set("Magic", 100);
        assertTrue(tt.get("Magic") == 100);
        // require 3 counter items.
        tt.set("Magic", 8000);
        assertTrue(tt.get("Magic") == 8000);
        // require 1 counter item.
        tt.set("Magic", 20);
        assertTrue(tt.get("Magic") == 20);


    }

    @Test
    public void testStoreAdvanced() {
        int itemSize = 6;
        int bucketCapacity = 40;
        int nrBuckets = 5;
        TinyCountingTable tt = new TinyCountingTable(itemSize, bucketCapacity, nrBuckets);

        int nrItems = 40;
        int loops = 2;
        String salt = "";
        for (int k = 0; k < loops; k++) {
            for (int i = 0; i < nrItems; i++) {

                for (int j = 0; j < i; j++) {
                    assertEquals(j, tt.get("Magic" + j + salt));
                }

                tt.set("Magic" + i + salt, i);

                assertTrue(tt.get("Magic_X") == 0);
            }
        }
    }

    @Test
    public void testStoreAdvanced2() {
        int itemSize = 6;
        int bucketCapacity = 40;
        int nrBuckets = 5;
        TinyCountingTable tt = new TinyCountingTable(itemSize, bucketCapacity, nrBuckets);

        int nrItems = 40;
        int loops = 5;
        String salt = "xyz";

        for (int k = 0; k < loops; k++) {

            for (int i = 0; i < nrItems; i++)
                System.out.println(k + ":\t" + i + " = " + tt.get("Magic" + i + salt));

            for (int i = 0; i < nrItems; i++) {

                for (int j = 0; j < i; j++) {
                    long x = tt.get("Magic" + j + salt);
                    assertEquals(j * (1+k), x);
                }

                tt.set("Magic" + i + salt, i * (1 + k));

                assertTrue(tt.get("Magic_X") == 0);
            }
        }
    }

}
