//package nars.budget;
//
//import nars.$;
//import nars.NAR;
//import nars.bag.BLink;
//import nars.nar.Default;
//import nars.task.Task;
//import nars.util.data.MutableDouble;
//import org.jetbrains.annotations.NotNull;
//import org.junit.Test;
//
//import java.util.ArrayList;
//import java.util.List;
//
//import static junit.framework.TestCase.assertEquals;
//import static org.junit.Assert.assertTrue;
//
//
//public class ItemAccumulatorTest {
//
//    @NotNull
//    NAR n = new Default();
//
//    @Test
//    public void testAccumulatorDeduplication() {
//        ItemAccumulator<Task> ii = new ItemAccumulator<>(
//                2, //capacity = 2 but this test will only grow to size 1 if successful
//                BudgetMerge.plusDQDominant
//        );
//
//        assertEquals(0, ii.bag().size());
//
//        Task t = n.task("$0.1$ <a --> b>. %1.00;0.90%");
//        assertEquals(0.1f, t.pri(), 0.001);
//
//        ii.bag().put(t);
//        assertEquals(1, ii.bag().size());
//
//        ii.bag().put(t);
//        assertEquals(1, ii.bag().size());
//
//        ii.bag().commit();
//
//        ii.bag().forEach(System.out::println);
//
//        //mergePlus:
//        assertEquals(0.1f+0.1f, ii.bag().sample().pri(), 0.001f);
//
//    }
//
//    /** test batch dequeing the highest-ranking tasks from an ItemAccumulator
//     *
//     * */
//    @Test public void testAccumulatorBatchDeque() {
//
//        int capacity = 4;
//
//        ItemAccumulator<Task> ii = new ItemAccumulator<>(
//                capacity,
//                BudgetMerge.plusDQBlend
//        );
//
//
//        String s = ". %1.00;0.90%";
//        Task n1 = n.task("$0.05$ <z-->x>" + s);
//
//        ii.bag().put(n1);
//        ii.bag().put(n.task("$0.09$ <a-->x>" + s ));
//        ii.bag().put(n.task("$0.1$ <b-->x>" + s ));
//        ii.bag().put(n.task("$0.2$ <c-->x>" + s ));
//        ii.bag().put(n.task("$0.3$ <d-->x>" + s ));
//        ii.bag().commit();
//        assertEquals(4, ii.bag().size());
//
//        //z should be ignored
//        //List<Task> buffer = Global.newArrayList();
//
//
//        assertEquals(capacity, ii.bag().size());
//
//        assertTrue(ii.bag().isSorted());
//
//        //System.out.println(ii);
//        ii.bag().forEach(System.out::println);
//
//        BLink<Task> oneLink = ii.bag().pop();
//        Task one = oneLink.get();
//        assertEquals("$.30;.50;.95$ (d-->x). :0: %1.0;.90%", one.toString());
//
//        List<Task> two = new ArrayList();
//        two.add(ii.bag().pop().get());
//        two.add(ii.bag().pop().get());
//        assertEquals("[$.20;.50;.95$ (c-->x). :0: %1.0;.90%, $.10;.50;.95$ (b-->x). :0: %1.0;.90%]", two.toString());
//
//        assertEquals(1, ii.bag().size());
//
////        ii.update(capacity, buffer);
////        System.out.println(buffer);
////        System.out.println(ii.size());
////        assertEquals(ii.size(), capacity);
//
//        //batch remove should return these in order: (d,c,b|a,)
//
//    }
//
//
//    @Test public void testForEachOrder() {
//
//        //highest first
//
//        int capacity = 8;
//
//        ItemAccumulator<Task> ii = new ItemAccumulator<>(capacity, BudgetMerge.plusDQBlend);
//        assertTrue(ii.bag().isSorted());
//
//        for (int i = 0; i < capacity - 1; i++) {
//            ii.bag().put($.task($.$("a:" + i), '?', null).budget( (float)Math.random() * 0.95f, 0.5f, 0.5f));
//        }
//
//        ii.bag().commit();
//
//        MutableDouble prev = new MutableDouble(Double.POSITIVE_INFINITY);
//
//        ii.bag().forEach( (Budgeted t) -> {
//            float p = t.budget().pri();
//            assertTrue(p <= prev.floatValue()); //decreasing
//            prev.set(p);
//        });
//
//        //this will use an Iterator to determine sorting
//        assertTrue(ii.bag().isSorted());
//    }
//
//    @Test public void testRankDurQuaForEqualPri() {   }
//
//    @Test public void testRankQuaForEqualPriDur() {   }
//
//    @Test public void testRankDurForEqualPriQua() {
//
//        int capacity = 8;
//
//        ItemAccumulator<Task> ii = new ItemAccumulator<>(capacity, BudgetMerge.plusDQBlend);
//
//        for (int i = 0; i < capacity-1; i++) {
//            float dur = i * 0.05f;
//            ii.bag().put($.task($.$("a:" + i), '?', null).budget(0.5f, dur, 0.5f));
//        }
//
//        assertTrue(ii.bag().isSorted());
//
//        ii.print(System.out);
//
//    }
//}