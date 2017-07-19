package nars.term;

import com.google.common.base.Joiner;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ProxyTermTest {

    @Test
    public void testEveryTermMethodProxied() {

        System.out.println(
                Joiner.on("\n").join(List.of(Term.class.getMethods()))
        );

    }
}