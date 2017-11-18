//package astar;
//
//import astar.model.GraphFind;
//
//import java.util.HashMap;
//
///**
// * Test case from wikipedia
// * http://de.wikipedia.org/wiki/A*-Algorithmus
// */
//
//public class City extends GraphFind<String>  {
//    public City(String name) {
//        super(name);
//        HashMap<String, Integer> fromSaarbrucken = new HashMap<String, Integer>();
//        fromSaarbrucken.put("Kaiserslautern", 70);
//        fromSaarbrucken.put("Karlsruhe", 145);
//        HashMap<String, Integer> fromKaiserslautern = new HashMap<String, Integer>();
//        fromKaiserslautern.put("Frankfurt", 103);
//        fromKaiserslautern.put("Ludwigshafen", 53);
//        HashMap<String, Integer> fromKarlsruhe = new HashMap<String, Integer>();
//        fromKarlsruhe.put("Heilbronn", 84);
//        HashMap<String, Integer> fromFrankfurt = new HashMap<String, Integer>();
//        fromFrankfurt.put("Würzburg", 116);
//        HashMap<String, Integer> fromLudwigshafen = new HashMap<String, Integer>();
//        fromLudwigshafen.put("Würzburg", 183);
//        HashMap<String, Integer> fromHeilbronn = new HashMap<String, Integer>();
//        fromHeilbronn.put("Würzburg", 102);
//        adjacencyMatrix.put("Saarbrücken", fromSaarbrucken);
//        adjacencyMatrix.put("Kaiserslautern",fromKaiserslautern);
//        adjacencyMatrix.put("Frankfurt", fromFrankfurt);
//        adjacencyMatrix.put("Karlsruhe", fromKarlsruhe);
//        adjacencyMatrix.put("Ludwigshafen", fromLudwigshafen);
//        adjacencyMatrix.put("Heilbronn", fromHeilbronn);
//    }
//
//    //heuristic cost to the goal node
//    public double h() {
//        switch(this.id) {
//            case "Saarbrücken":     return 222;
//            case "Kaiserslautern":  return 158;
//            case "Karlsruhe":       return 140;
//            case "Frankfurt":       return 96;
//            case "Ludwigshafen":    return 108;
//            case "Heilbronn":       return 87;
//            case "Würzburg":        return 0;
//            default:                return 0;
//        }
//    }
//    private City castToSearchNodeCity(Find other) {
//        return (City) other;
//    }
//
//
//}
