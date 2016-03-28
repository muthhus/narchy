package nars.web;

import com.addthis.basis.util.LessBytes;
import com.addthis.meshy.MeshyClient;
import com.addthis.meshy.MeshyServer;
import com.addthis.meshy.service.file.FileReference;
import com.addthis.meshy.service.file.FileSource;
import com.addthis.meshy.service.message.MessageFileProvider;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

public class TestMessageFileSystem  {


    public static void main(String[] args) throws IOException {
        final MeshyServer a = new MeshyServer(11001);// getServer("/tmp");
        //final MeshyServer b = new MeshyServer(10002); // getClient(server);
        final MeshyClient b = new MeshyClient("127.0.0.1", 11001);
        final MeshyClient c = new MeshyClient("127.0.0.1", 11001);

        {

            MessageFileProvider provider = new MessageFileProvider(b);
            provider.setListener("/rpc.test/one.rpc", (fileName, options, out) -> {
            /* this is the client rpc reply endpoint implementation */
                LessBytes.writeString("rpc.reply", out);
            /* bytes are accumulated and sent on close */
                out.close();
            });
        }
//
        {
            /*
             * client registers rpc endpoint in mesh filespace: /rpc.test/one.rpc
             */
            FileSource files = new FileSource(c, new String[]{"/rpc.test/*.rpc"});
            files.waitComplete();
            Map<String, FileReference> map = files.getFileMap();

            System.out.println("files = " + map);
        }



//        /*
//         * rpc is called/read as a normal file
//         */
//        FileReference ref = map.get("/rpc.test/one.rpc");
//        InputStream in = client.readFile(ref);
//        String str = LessBytes.readString(in);
//        in.close();
//
//        assertEquals("rpc.reply", str);
//        provider.close();
    }

}
