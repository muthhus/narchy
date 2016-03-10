package alice.tuprolog;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestEinsteinRiddle {
    
    @Test
    public void einsteinsRiddle() throws InterruptedException {

        String ein =
                "next_to(X,Y,List) :-\n" +
                        "    iright(X,Y,List).\n" +
                        "next_to(X,Y,List) :-\n" +
                        "    iright(Y,X,List).\n" +
                        "einstein(Houses,Fish_Owner) :-\n" +
                        "    '='(Houses, [[house,norwegian,_,_,_,_],_,[house,_,_,_,milk,_],_,_]),\n" +
                        "    member([house,brit,_,_,_,red],Houses),\n" +
                        "    member([house,swede,dog,_,_,_],Houses),\n" +
                        "    member([house,dane,_,_,tea,_],Houses),\n" +
                        "    iright([house,_,_,_,_,green],[house,_,_,_,_,white],Houses),\n" +
                        "    member([house,_,_,_,coffee,green],Houses),\n" +
                        "    member([house,_,bird,pallmall,_,_],Houses),\n" +
                        "    member([house,_,_,dunhill,_,yellow],Houses),\n" +
                        "    next_to([house,_,_,dunhill,_,_],[house,_,horse,_,_,_],Houses),\n" +
                        "    member([house,_,_,_,milk,_],Houses),\n" +
                        "    next_to([house,_,_,marlboro,_,_],[house,_,cat,_,_,_],Houses),\n" +
                        "    next_to([house,_,_,marlboro,_,_],[house,_,_,_,water,_],Houses),\n" +
                        "    member([house,_,_,winfield,beer,_],Houses),\n" +
                        "    member([house,german,_,rothmans,_,_],Houses),\n" +
                        "    next_to([house,norwegian,_,_,_,_],[house,_,_,_,_,blue],Houses),\n" +
                        "    member([house,Fish_Owner,fish,_,_,_],Houses).\n" +
                        "iright(L,R,[L,R|_]).\n" +
                        "iright(L,R,[_|Rest]) :-\n" +
                        "    iright(L,R,Rest).\n"
                        ;

        String[] s = new String[1];
        //The answer is the German owns the fish.
        Agent a = new Agent(ein, "einstein(_,X), write(X).");
        a.addOutputListener(o-> {
            if (s[0]==null)
                s[0] = o.getMsg();
        });
        a.spawn().join();

        assertEquals("german", s[0]);


    }
    
}
