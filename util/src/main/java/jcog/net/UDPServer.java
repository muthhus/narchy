package jcog.net;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.RemovalCause;
import com.github.benmanes.caffeine.cache.RemovalListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Closeable;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

/**
 * javascript UDP context
 */
public abstract class UDPServer<S extends Consumer<byte[]>> extends UDP implements RemovalListener<InetSocketAddress,UDPServer.Session<S>> {

    final static int MAX_SESSIONS = 32;

    static class Session<A> {
        @NotNull final A api;
        private long last;

        Session(@NotNull A api) {
            this.api = api;
            touch();
        }

        public long stale() {
            return stale(System.currentTimeMillis());
        }

        /** in ms */
        public long stale(long now) {
            return now - last;
        }

        public void touch() { this.last = System.currentTimeMillis(); }
    }

    final Cache<InetSocketAddress,Session<S>> sessions;

    public UDPServer(int port) throws SocketException {
        super(port);
        sessions = Caffeine.newBuilder().maximumSize(MAX_SESSIONS).removalListener(this).build();
    }


    @Override
    public void onRemoval(@Nullable InetSocketAddress key, @Nullable Session<S> value, @NotNull RemovalCause cause) {
        end(value.api, false);
    }

    /** explicitly disconnect a client */
    public void end(@NotNull S s, boolean removeFromCache) {

        if (s instanceof Closeable) {
            try {
                ((Closeable) s).close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (removeFromCache) {
            //HACK
            Set<Map.Entry<InetSocketAddress, Session<S>>> e = sessions.asMap().entrySet();
            for (Map.Entry<InetSocketAddress, Session<S>> ee : e) {
                if (ee.getValue() == s) {
                    sessions.invalidate(ee.getKey());
                }
            }
        }
    }

    @Override
    protected void in(DatagramPacket p, @NotNull byte[] data) {
        Session<S> ss = sessions.get((InetSocketAddress) p.getSocketAddress() , this::session);
        ss.touch();
        ss.api.accept(data);
    }

    abstract protected S get(@NotNull InetSocketAddress socketAddress);

    protected Session<S> session(@NotNull InetSocketAddress a) {
        return new Session<>(get(a));
    }

}
