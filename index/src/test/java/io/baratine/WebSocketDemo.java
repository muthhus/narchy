package io.baratine;


import io.baratine.service.Result;
import io.baratine.service.Service;
import io.baratine.web.Path;
import io.baratine.web.ServiceWebSocket;
import io.baratine.web.Web;
import io.baratine.web.WebSocket;

import java.io.IOException;


public class WebSocketDemo {

    @Path("/echo")
    public static class WebSocketEcho implements ServiceWebSocket<String,String>
    {
        @Override
        public void next(String  value, WebSocket<String> ws)
                throws IOException
        {
            ws.write(value);
        }

        public static void main(String []argv)
        {
            Web.include(WebSocketEcho.class);
            Web.go(argv);
        }
    }


    @Service
    public static class CounterService  {
        private long count;

        public void addAndGet(long increment, Result<Long> result) {
            count += increment;

            result.ok(count);
        }
    }
    public static void main(String[] args) throws Exception {
        Web.include(WebSocketEcho.class);
        Web.include(CounterService.class);

        Web.go(args);
    }
}
