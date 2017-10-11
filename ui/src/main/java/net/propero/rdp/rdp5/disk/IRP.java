package net.propero.rdp.rdp5.disk;

import net.propero.rdp.RdpPacket;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

public class IRP {

    public final int fileId;

    public final int majorFunction;

    public final int minorFunction;

    public final DataOutputStream out;
    public final ByteArrayOutputStream bout;

    public RdpPacket data;

    public int deviceId;

    public int completionId;


    public IRP(int fileId, int majorFunction, int minorFunction) {
        this.fileId = fileId;
        this.majorFunction = majorFunction;
        this.minorFunction = minorFunction;

        bout = new ByteArrayOutputStream();
        out = new DataOutputStream(bout);
    }

}
