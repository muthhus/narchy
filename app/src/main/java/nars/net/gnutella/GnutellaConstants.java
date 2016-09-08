package nars.net.gnutella;

/**
 * Class that defines the constants of the Gnutella Protocol v0.4
 *
 * @author Ismael Fernandez
 */
public enum GnutellaConstants {
    ;

    /* Constantes de solicitud conexion */
    public static final String CONNECTION_REQUEST_PRE = "GNUTELLA CONNECT";

    public static final String CONNECTION_REQUEST = "GNUTELLA CONNECT/<0.4>\n\n ";
    public static final byte[] CONNECTION_REQUESTbytes = CONNECTION_REQUEST.getBytes();

    public static final String CONNECTION_ACCEPTED = "GNUTELLA OK\n\n";

    /* Constantes de tipos de mensaje */
    public static final byte PING = 0x00;
    public static final byte PONG = 0x01;
    public static final byte PUSH = 0x40;
    public static final byte QUERY = 80;
    public static final byte QUERY_HIT = 81;

    /* Constantes de tamanio */
    public static final int ID_LENGTH = 16;
    public static final int PLD_LENGTH = 1;
    public static final int TTL_LENGTH = 1;
    public static final int HOP_LENGTH = 1;
    public static final int PLL_LENGTH = 4;
    public static final int PORT_LENGTH = 2;
    public static final int IP_LENGTH = 4;
    public static final int NF_LENGTH = 4;
    public static final int NK_LENGTH = 4;
    public static final int HEADER_LENGTH = 23;
    public static final byte PING_PLL = 0;
    public static final byte PONG_PLL = 14;
    //public static final int QUERYHIT_PART_L = 19;
    public static final int SERVER_ID_L = 16;

    /* Constantes de implementacion */
    public static final byte DEFAULT_TTL = 7;
    public static final int MY_MESSAGE = -1;
    public static final int MINSPEEDL = 2;
    public static final byte INITIAL_HOP = -1;
    public static final byte EOS = 0x0000;
    public static final int EOS_L = 1;
    public static final int DFLT_SPEED = 1;
    public static final short DFLTMIN_SPEED = 0;
    public static final byte END = -1;
    public static final short MIN_PORT = 1024;

    /* Constantes tipo de nodos */
    public static final int DOWNLOAD_NODE = 2;
    public static final int SERVENT_NODE = 1;
    public static final int FAILURE_NODE = -1;

    public static final boolean ACCEPTED = true;

    /*Constantes del Protocolo de HTTP*/
    public static final String HTTP_GET = "GET ";
    public static final String HTTP_GETLC = "get";
    public static final String HTTP_STRING = " HTTP";
    public static final String HTTP_VERSION = "1.0";
    public static final String HTTP_CONNECTION = "Connection:";
    public static final String HTTP_RANGE = "Range:";
    public static final String HTTP_BYTES = "bytes";
    public static final String HTTP_OK = "HTTP 200 OK\r\nServer: Gnutella\r\nContent-type: application/binary\r\nContent-length: ";
    public static final String HTTP_GETPART = "GET /get/";
    public static final String HTTP_REST = "/ HTTP/1.0\r\nConnection: Keep-Alive\r\nRange: bytes=";
    public static final String HTTP_DENY = " HTTP 400 Bad Request\r\n\r\n";


    public static final int AUTO_PING_PER_N_MESSAGES = 10;
    static final long DEAD_CONNECTION_REMOVAL_INTERVAL_MS = 30000;

    public static int MAX_MESSAGE_SIZE = 4096; //maybe up to 64k, max UDP message length
}
