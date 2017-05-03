package jcog.net;

import jcog.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by me on 3/8/17.
 */
public abstract class UDPNetworkSimulation {

    final Timer sim = new Timer();

    UDPeer[] peers;

    public UDPNetworkSimulation(int size) throws SocketException, UnknownHostException {
        int port = 10000;

        peers = new  UDPeer[size];
        for (int i  = 0; i < size; i++) {
            peers[i] = new MyUDPeer(port, i);
        }

        //for (int j = 0; j < 3; j++) {
            //System.out.println("round " + j);

            for (int i = 0; i < size; i++) {
                peers[i].ping(peers[ (i + 1) % size ].port());
                //peers[i].ping(peers[ (i + 2) % size ].port());
                //peers[i].ping(peers[ (i + 3) % size ].port());
            }

        ///}

        Util.sleep(2000);

        peers[0].believe("hi", (byte)4);


        Util.sleep(10000);


    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        new UDPNetworkSimulation(5) {
            @Override
            long delay(InetSocketAddress from, InetSocketAddress to, int length) {
                return 25 + Math.abs(from.getPort() - to.getPort()) * 50;
            }
        };
    }

    private class MyUDPeer extends UDPeer {

        public MyUDPeer(int port, int i) throws SocketException, UnknownHostException {
            super(port + i);
        }

        public void actuallySend(Msg o, InetSocketAddress to) {
            super.send(o, to);
        }

        @Override
        public void send(Msg o, InetSocketAddress to) {
            sim.schedule(new TimerTask() {
                @Override public void run() {
                    actuallySend(o, to);
                }
            }, delay((InetSocketAddress) in.getLocalSocketAddress(), to, o.length()));
        }

        @Override
        protected void receive(@Nullable UDPeer.UDProfile connected, @NotNull UDPeer.Msg m) {
            System.out.println(me + " receive: " + m);
        }
    }

    abstract long delay(InetSocketAddress from, InetSocketAddress to, int length);

}
