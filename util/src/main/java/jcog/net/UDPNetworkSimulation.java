package jcog.net;

import jcog.Util;
import org.eclipse.collections.impl.factory.Maps;

import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

/**
 * Created by me on 3/8/17.
 */
public class UDPNetworkSimulation {

    UDPeer[] peers;

    public UDPNetworkSimulation(int size) throws SocketException, UnknownHostException {
        int port = 10000;

        peers = new  UDPeer[size];
        for (int i  = 0; i < size; i++) {
            peers[i] = new UDPeer(port + i) {

            };
        }

        //for (int j = 0; j < 3; j++) {
            //System.out.println("round " + j);

            for (int i = 0; i < size; i++) {
                peers[i].ping(peers[ (i + 1) % size ].me());
                //peers[i].ping(peers[ (i + 2) % size ].me());
            }

        ///}

        Util.sleep(1000);

        peers[0].say("hi", (byte)6);


        Util.sleep(10000);


    }

    public static void main(String[] args) throws SocketException, UnknownHostException {
        new UDPNetworkSimulation(8);
    }
}
