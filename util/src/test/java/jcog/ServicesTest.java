package jcog;

import com.google.common.util.concurrent.AbstractIdleService;
import org.junit.Test;

public class ServicesTest {

    @Test
    public void testServices1() {
        Services<String> s = new Services();
        StringBuilder sb = new StringBuilder();

        s.add("x", new DummyIdleService(sb));
        s.add("y", new DummyIdleService(sb));

        s.print(System.out);

        s.stopAsync();

        s.print(System.out);
    }

    private static class DummyIdleService extends AbstractIdleService {
        private final StringBuilder sb;

        public DummyIdleService(StringBuilder sb) {
            this.sb = sb;
        }

        @Override
        protected void startUp() throws Exception {
            sb.append(this).append(" start\n");
        }

        @Override
        protected void shutDown() throws Exception {
            sb.append(this).append(" stop\n");
        }
    }
}