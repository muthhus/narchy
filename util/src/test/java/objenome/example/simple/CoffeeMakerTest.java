///**
// * Copyright (C) 2015 Square, Inc.
// * <p>
// * Licensed under the Apache License, Version 2.0 (the "License");
// * you may not use this file except in compliance with the License.
// * You may obtain a copy of the License at
// * <p>
// * http://www.apache.org/licenses/LICENSE-2.0
// * <p>
// * Unless required by applicable law or agreed to in writing, software
// * distributed under the License is distributed on an "AS IS" BASIS,
// * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// * See the License for the specific language governing permissions and
// * limitations under the License.
// */
//package objenome.example.simple;
//
//import objenome.O;
//
//import objenome.in;
//
//import objenome.out;
//import objenome.the;
//import org.junit.Before;
//import org.junit.Test;
//import org.mockito.Mockito;
//
//import javax.inject.Singleton;
//
//import static org.junit.Assert.assertFalse;
//import static org.junit.Assert.assertTrue;
//
//public class CoffeeMakerTest {
//
//    @the(
//        extend = DripCoffeeModule.class,
//        in = CoffeeMakerTest.class,
//        override = true
//    ) static class TestModule {
//        public @in CoffeeMaker coffeeMaker;
//        public @in Heater heater;
//    }
//
//    @Test
//    public void testHeaterIsTurnedOnAndThenOff() {
//        TestModule t = O.of(new TestModule());
//        Mockito.when(t.heater.isHot()).thenReturn(true);
//        t.coffeeMaker.brew();
//        t.heater.on();
//        assertTrue(t.heater.isHot());
//        t.heater.off();
//        assertFalse(t.heater.isHot());
//        //Mockito.verify(heater, Mockito.times(1)).on();
//        //Mockito.verify(heater, Mockito.times(1)).off();
//    }
//
//
//}
