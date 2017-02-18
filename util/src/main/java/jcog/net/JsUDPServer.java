package jcog.net;

import jdk.nashorn.api.scripting.NashornScriptEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;


/**
 *
 * @param <A> API interface class
 */
public class JsUDPServer<A> extends UDPServer<JsUDPServer<A>.JsSession> {

    private static final Logger logger = LoggerFactory.getLogger(JsUDPServer.class);

    final static Executor exe = ForkJoinPool.commonPool();

    static transient final ScriptEngineManager engineManager = new ScriptEngineManager();
    static transient final NashornScriptEngine JS = (NashornScriptEngine) engineManager.getEngineByName("nashorn");

    private final BiFunction<UDP, InetSocketAddress, A> apiBuilder;

    public JsUDPServer(int port, Supplier<A> apiBuilder) throws SocketException {
        this(port, (udp, a) -> apiBuilder.get());
    }

    public JsUDPServer(int port, BiFunction<UDP, InetSocketAddress, A> apiBuilder) throws SocketException {
        super(port);
        this.apiBuilder = apiBuilder;
    }

    @Override
    protected JsSession get(InetSocketAddress a) {
        return new JsSession(a, apiBuilder.apply(this, a));
    }


    class JsSession extends SimpleBindings implements Consumer<byte[]> {

        private final A context;
        private final InetSocketAddress host;

        public JsSession(InetSocketAddress s, A api) {
            super();
            this.host = s;
            this.context = api;
            put("i", api); //i.
        }

        @Override
        public void accept(byte[] codeByte) {
            String code = new String(codeByte);

            //END signal
            if (code.equals(";")) {
                end(this, true);
                return;
            }

            exe.execute(() -> {
                Object result = eval(code, this, JS);
                //System.out.println(result + " " + result.getClass());
                if (result != null) {

//                    try {
//                        MessageBufferPacker out = MessagePack.newDefaultBufferPacker();
//                        out.packString(result.toString());
//                        out(out.toByteArray(), host);
//                    } catch (IOException e) {
//                        logger.error("{}", e);
//                    }
                    outJSON(result, host);
                }
            });
        }

    }


    static Object eval(String code, SimpleBindings bindings, NashornScriptEngine engine) {
        Object o;
        //long start = System.currentTimeMillis();

        try {
            if (bindings == null)
                o = engine.eval(code);
            else
                o = engine.eval(code, bindings);

        } catch (Throwable t) {
            o = t;
        }

        return o;

//            if (o == null) {
//                //return null to avoid sending the execution summary
//                return null;
//            } else {
//                long end = System.currentTimeMillis();
//
//                onResult.accept(new JSExec(code, o, start, end));
//            }
    }

}
