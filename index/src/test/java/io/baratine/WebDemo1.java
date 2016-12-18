package io.baratine;

import io.baratine.service.Service;
import io.baratine.web.Get;
import io.baratine.web.RequestWeb;
import io.baratine.web.Web;

/**
 * Created by me on 12/17/16.
 */

@Service
public class WebDemo1 {
    @Get("/hello")
    public void doHello(RequestWeb request) {
        request.ok("hello");
    }

    public static void main(String[] args) throws Exception {
        Web.include(WebDemo1.class);

        Web.go(args);
    }
}
