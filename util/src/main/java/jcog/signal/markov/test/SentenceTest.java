//package jcog.signal.markov.test;
//
//
//import jcog.signal.markov.MarkovSentence;
//
//import java.io.IOException;
//import java.net.MalformedURLException;
//import java.net.URL;
//
//public class SentenceTest {
//
//    public static void main(String[] args) {
//        MarkovSentence ms = new MarkovSentence(1);
//
//        URL url;
//        //String u = "http://students.mint.ua.edu/~pmkilgo/tmp/21597.txt";
//        String u = "https://raw.githubusercontent.com/automenta/narchy/skynet1/LICENSE";
//
//        try {
//
//            url = new URL(u);
//            ms.parseSentence(url.openStream());
//
//            System.out.println(ms.generateSentence(25));
//        } catch (MalformedURLException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//
//    }
//}
