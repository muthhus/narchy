package jcog.net;

import jcog.Util;

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
                peers[i].ping(peers[ (i + 1) % size ].me());
                //peers[i].ping(peers[ (i + 2) % size ].me());
            }

        ///}

        Util.sleep(5000);

        peers[0].say("hi", (byte)4);


        Util.sleep(10000);


    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        new UDPNetworkSimulation(32) {
            @Override
            long delay(InetSocketAddress from, InetSocketAddress to, int length) {
                return 50 + (int)/*Util.sqr*/(Math.abs(from.getPort() - to.getPort())) * 50;
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
            }, delay(me, to, o.length()));
        }
    }

    abstract long delay(InetSocketAddress from, InetSocketAddress to, int length);

}
