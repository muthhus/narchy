package jcog.net;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;

/**
 * from https://github.com/apache/incubator-gossip/blob/master/gossip-base/src/main/java/org/apache/gossip/secure/KeyTool.java
 *
 * https://github.com/apache/incubator-gossip/blob/master/gossip-itest/src/test/java/org/apache/gossip/SignedMessageTest.java
 */
public class Crypto {

    public static void generatePubandPrivateKeyFiles(String path, String id)
            throws NoSuchAlgorithmException, NoSuchProviderException, IOException, java.io.FileNotFoundException {
        SecureRandom r = new SecureRandom();
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");
        keyGen.initialize(1024, r);
        KeyPair pair = keyGen.generateKeyPair();
        PrivateKey priv = pair.getPrivate();
        PublicKey pub = pair.getPublic();
        {
            FileOutputStream sigfos = new FileOutputStream(new File(path, id));
            sigfos.write(priv.getEncoded());
            sigfos.close();
        }
        {
            FileOutputStream sigfos = new FileOutputStream(new File(path, id + ".pub"));
            sigfos.write(pub.getEncoded());
            sigfos.close();
        }
    }

    public static void main(String[] args) throws
            NoSuchAlgorithmException, NoSuchProviderException, IOException {
        generatePubandPrivateKeyFiles(args[0], args[1]);
    }

}
