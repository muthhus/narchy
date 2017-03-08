package jcog.net;

import jcog.Util;
import org.eclipse.collections.impl.factory.Maps;

import java.net.InetSocketAddress;
import java.net.SocketException;

/**
 * Created by me on 3/8/17.
 */
public class UDPNetworkSimulation {

    UDPeer[] peers;

    public UDPNetworkSimulation(int size) throws SocketException {
        int port = 10000;

        peers = new  UDPeer[size];
        for (int i  = 0; i < size; i++) {
            peers[i] = new UDPeer(port + i) {

            };
        }

        for (int i = 1; i < size; i++) {
            peers[i].ping((InetSocketAddress) peers[i-1].in.getLocalSocketAddress());
        }

        Util.sleep(1000);

        peers[1].send(Maps.mutable.of("_", "y"), 0.5f);

        Util.sleep(10000);


    }

    public static void main(String[] args) throws SocketException {
        new UDPNetworkSimulation(16);
    }
}
