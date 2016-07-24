package nars.util;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.SleepingWaitStrategy;
import com.lmax.disruptor.WaitStrategy;
import com.lmax.disruptor.dsl.BasicExecutor;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Created by me on 7/24/16.
 */
public class DisruptorTest {

    static class Message {
        public Object data;
    }

    public static void main(String[] args) throws Exception    {

        Disruptor<Message> disruptor =
            new Disruptor<Message>(
                    ()->new Message(), 1024,
                    //Executors.newFixedThreadPool(4)
                    Executors.defaultThreadFactory()
            );

        disruptor.handleEventsWith(DisruptorTest::g, DisruptorTest::g);
        disruptor.handleEventsWithWorkerPool(DisruptorTest::h, DisruptorTest::h);

        disruptor.start();

        for (int i = 0; i < 5; i++)        {
            disruptor.publishEvent((event, sequence, buffer) -> event.data = buffer, Math.random());
            //Thread.sleep(10);
        }

        Thread.sleep(200);
        disruptor.shutdown();
        disruptor.halt();

    }

    public static void h(Message m) {
        System.out.println(Thread.currentThread() + " recv: " + m.data);
        //Util.pause((int)(Math.random()*100));
    }
    public static void g(Message m, long sequence, boolean endOfBatch) {
        System.out.println(Thread.currentThread() + "  " + " " + sequence + endOfBatch + " " + m.data);
        //Util.pause((int)(Math.random()*100));
    }
}