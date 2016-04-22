package nars.web;

import nars.NAR;
import nars.util.data.MutableInteger;
import nars.util.data.Util;
import ognl.Ognl;
import ognl.OgnlException;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;

/**
 * Created by me on 4/21/16.
 */
public class EvalService<R,V> extends WebsocketService implements Runnable {

    private final MutableInteger updatePeriodMS;
    private final R root;
    private final Object expression;
    private Thread thread;

    final static Logger logger = LoggerFactory.getLogger(EvalService.class);

    public EvalService(R root, String expression, int updatePeriodMS) throws OgnlException {
        super();
        this.root = root;
        this.expression = Ognl.parseExpression( expression );
        this.updatePeriodMS = new MutableInteger(updatePeriodMS);
    }

    @Override
    public void onStart() {
        thread = new Thread(this);
        thread.start();
    }

    @Override
    public void onStop() {
        thread.interrupt();
        thread = null;
    }

    @Override
    public void run() {
        while (thread!=null) {
            update();
            Util.pause(updatePeriodMS.intValue());
        }
    }

    @Nullable
    public final V get() {
        try {
            return (V)Ognl.getValue(expression, root);
        } catch (OgnlException e) {
            logger.error("{}", e);
            return null;
        }
    }

    protected void update() {


        V v = get();
        if (v != null)
            send(v);


//        new NARSummary(nar) {
//            @Override public void get() {
//                send(data);
//            }
//        };
    }

//    /** evaluates an expression in thread-safe way that will not interfere with a NAR */
//    abstract public static class NAREvalService implements Runnable {
//
//        transient private final NAR nar;
//        final HashMap data = new HashMap();
//
//        public NARSummary(NAR nar) {
//            this.nar = nar;
//
//            nar.runLater(this);
//        }
//
//        @Override
//        public void run() {
//            put("emotion", nar.emotion );
//
//            get();
//
//        }
//
//        private void put(String key, Serializable value) {
//            data.put(key, value);
//        }
//
//        /** called when finished */
//        abstract protected void get();
//    }

}

