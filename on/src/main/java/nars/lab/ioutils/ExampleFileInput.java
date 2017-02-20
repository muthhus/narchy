/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.lab.ioutils;

import nars.NAR;
import nars.io.TextInput;
import nars.lab.testutils.OutputCondition;

import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Access to library of examples/unit tests
 */
public class ExampleFileInput extends TextInput {

    @Deprecated public static final String basePath = "/home/me/n/on/";
    static final String[] directories = {
            basePath + "nal/test",
            basePath + "nal/Examples/DecisionMaking",
            basePath + "nal/Examples/ClassicalConditioning"
    };

    public static String load(String path) throws IOException {
        StringBuilder  sb  = new StringBuilder();
        String line;
        File fp = new File(path);
        BufferedReader br = new BufferedReader(new FileReader(fp));
        while ((line = br.readLine())!=null) {
            sb.append(line).append('\n');
        }
        return sb.toString();
    }
    
    /** narsese source code, one instruction per line */
    private final String source;

    protected ExampleFileInput(String input) {
        super(input);
        this.source = input;
    }
    
    public static ExampleFileInput get(String id) throws Exception {
        return new ExampleFileInput(load(basePath + "./nal/" + id +".nal"));
    }
    
    public List<OutputCondition> enableConditions(NAR n, int similarResultsToSave) {
        return OutputCondition.getConditions(n, source, similarResultsToSave);
    }

    public static Map<String,Object> getUnitTests() {
        Map<String,Object> l = new TreeMap();
        

        for (String dir : directories ) {

            File folder = new File(dir);
        
            for (final File file : folder.listFiles()) {
                if (file.getName().equals("README.txt") || file.getName().contains(".png"))
                    continue;
                if(!("extra".equals(file.getName()))) {
                    l.put(file.getName(), new Object[] { file.getAbsolutePath() } );
                }
            }
            
        }
        return l;
    }

    public String getSource() {
        return source;
    }
    
}
