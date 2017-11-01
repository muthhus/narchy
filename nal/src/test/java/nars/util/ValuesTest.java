package nars.util;

import jcog.pri.Priority;
import nars.Task;
import nars.term.Term;
import nars.truth.Truth;
import net.openhft.chronicle.bytes.BytesStore;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.function.Function;


public class ValuesTest {


    /** TODO */
    static class RawTask implements Task {

        private final BytesStore i;

        public RawTask(BytesStore b) {
            this.i = b;
        }

        public RawTask(byte[] b) {
            this(BytesStore.wrap(b));
        }

        @Override
        public short[] cause() {
            return new short[0];
        }

        @Override
        public <X> X meta(String key) {
            return null;
        }

        @Override
        public void meta(String key, Object value) {

        }

        @Override
        public <X> X meta(String key, Function<String, Object> valueIfAbsent) {
            return null;
        }

        @Override
        public float setPri(float p) {
            return 0;
        }

        @Override
        public @Nullable Priority clonePri() {
            return null;
        }

        @Override
        public boolean delete() {
            return false;
        }

        @Override
        public float pri() {
            return 0;
        }

        @Override
        public double coord(boolean maxOrMin, int dimension) {
            return 0;
        }

        @Override
        public Term term() {
            return null;
        }

        @Override
        public long creation() {
            return 0;
        }

        @Override
        public long start() {
            return 0;
        }

        @Override
        public long end() {
            return 0;
        }

        @Override
        public long[] stamp() {
            return new long[0];
        }

        @Override
        public @Nullable Truth truth() {
            return null;
        }
    }


    @Test
    public void testVal1() {


    }
}
