package nars.inter.gnutella.thread;

import nars.inter.gnutella.GnutellaConstants;
import nars.inter.gnutella.Peer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.math.BigInteger;
import java.net.Socket;
import java.nio.ByteBuffer;

/**
 * Created by me on 7/9/16.
 */ //    /*
//     * (non-Javadoc)
//     *
//     * @see java.lang.Runnable#run()
//     */
//    @Override
//    public void run() {
//        flag = true;
//
//        if (!downloadThread) {
//            runServer();
//        } else {
//            runDownload();
//        }
//    }
public class DownloadThread extends PeerThread {

    static final Logger logger = LoggerFactory.getLogger(DownloadThread.class);

    private final boolean server;
    private BigInteger fileLength;
    private String fileName;
    private BigInteger rangeByte;

    public DownloadThread(Socket socket, Peer peer, boolean server) throws IOException {
        super(socket, peer);
        this.server = server;
    }

    /**
     * * Send a HTTP download request string to the server with which is
     * connected
     *
     * @param file  Name of the file of the request
     * @param size  Size of the file of the request
     * @param range Number of bytes that have been download from this file
     * @return true if the request is accepted, otherwise false
     */
    public boolean start(String file, int size, int range) {
        try {
            this.rangeByte = BigInteger.valueOf(range);
            this.fileLength = BigInteger.valueOf(size);
            this.fileName = file;
            out = socket.getOutputStream();
            //outStream = new DataOutputStream(out);
            in = socket.getInputStream();
            inStream = new DataInputStream(in);
            String request = GnutellaConstants.HTTP_GETPART + size + '/' + file
                    + GnutellaConstants.HTTP_REST + range + "\r\n\r\n";
            //outStream.writeUTF(request);
            String answer = inStream.readUTF();
            if (answer.equals(GnutellaConstants.HTTP_OK + size + "\r\n\r\n")) {
                return true;
            }
            inStream.close();
            in.close();
            //outStream.close();
            out.close();
            socket.close();
            return false;
        } catch (IOException e) {
            System.err.println(getClass() + ".downloadConnexion():"
                    + e.getClass() + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public void run() {

        String file = fileName;

        if (server) {
            sendData(file);
        } else {
            recvData(file);
        }
    }

    public void recvData(String file) {
        try {

            int remaining = (int)(fileLength.longValue() - rangeByte.longValue());
            ByteBuffer b = ByteBuffer.allocate(remaining);

            int pos = 0;
            while (remaining > 0 && inStream.available() > 0) {
                int r = inStream.read(b.array(), pos, remaining);
                remaining -= r;
                pos += r;
            }
            b.rewind();
            peer.model.data(peer, file, b, (int)rangeByte.longValue());


        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public void sendData(String file) {
        try {

            byte[] b = peer.model.data(peer, file, (int) rangeByte.longValue());
            if (b != null) {
                out.write(b);
            }

            out.close();


        } catch (FileNotFoundException e) {
            logger.warn("File not found: {}", e);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static byte[] sendFile(String file, int rangePosition) throws IOException, FileNotFoundException {
        RandomAccessFile f = new RandomAccessFile(file, "r");

        byte[] b = new byte[(int) f.length() - rangePosition];

        f.seek(rangePosition);
        int actual = f.read(b);

        logger.info("sent {}/{} bytes from {}", actual, b.length, file);
        return b;
    }

}
