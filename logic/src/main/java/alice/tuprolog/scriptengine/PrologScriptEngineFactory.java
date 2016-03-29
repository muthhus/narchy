/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package alice.tuprolog.scriptengine;

import alice.util.VersionInfo;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
* Implementation of the ScriptEngineFactory interface
* @author Andrea Bucaletti
*/
public class PrologScriptEngineFactory implements ScriptEngineFactory {
    
    public static final PrologScriptEngineFactory DEFAULT_FACTORY;
    
    private static final String ENGINE_NAME;
    private static final String ENGINE_VERSION;
    
    private static final String LANGUAGE_NAME;
    private static final String LANGUAGE_VERSION;
    
    private static final List<String> EXTENSIONS;
    private static final List<String> MIME_TYPES;
    private static final List<String> NAMES;
    
    private static final HashMap<String, String> PARAMETERS;
    
    static {
    	DEFAULT_FACTORY = new PrologScriptEngineFactory();
    	
    	ENGINE_NAME = "tuProlog";
    	ENGINE_VERSION = VersionInfo.getEngineVersion();
    	
    	LANGUAGE_NAME = "Prolog";
    	LANGUAGE_VERSION = VersionInfo.getEngineVersion();
    	
    	EXTENSIONS = Collections.unmodifiableList(Arrays.asList("pro", "pl", "2p"));
    	
    	MIME_TYPES  = Collections.unmodifiableList(Collections.singletonList("text/plain"));
    	
    	NAMES = Collections.unmodifiableList(Arrays.asList("tuProlog", "Prolog", "prolog"));
    	
    	PARAMETERS = new HashMap<>();
    	
    	PARAMETERS.put("ENGINE", ENGINE_NAME);
    	PARAMETERS.put("ENGINE_VERSION", ENGINE_VERSION);
    	PARAMETERS.put("NAME", NAMES.get(0));
    	PARAMETERS.put("LANGUAGE", LANGUAGE_NAME);
    	PARAMETERS.put("LANGUAGE_VERSION", LANGUAGE_VERSION);
    	PARAMETERS.put("THREADING", null);
    }

    @Override
    public String getEngineName() {
        return ENGINE_NAME;
    }

    @Override
    public String getEngineVersion() {
        return ENGINE_VERSION;
    }

    @Override
    public List<String> getExtensions() {
        return EXTENSIONS;
    }

    @Override
    public List<String> getMimeTypes() {
        return MIME_TYPES;
    }

    @Override
    public List<String> getNames() {
        return NAMES;
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public String getLanguageVersion() {
    	return LANGUAGE_VERSION;
    }

    @Override
    public Object getParameter(String name) {
        return PARAMETERS.get(name);
    }

    @Override
    public String getMethodCallSyntax(String obj, String methodName, String... args) {
        String result = "";
        
        result += obj + " <- " + methodName;
        
        if(args.length > 0) {
            result += "(";
            
            for(int i = 0; i < args.length; i++) {
                result += args[i] + (i == args.length - 1 ? ")" : ", ");
            }
        }
        
        return result;
    }

    @Override
    public String getOutputStatement(String string) {
        return "write('" + string + "')";
    }

    @Override
    public String getProgram(String... instr) {
        
    	String program = "";
        
    	for(int i = 0; i < instr.length; i++)
    		program += instr[i] + (i == instr.length - 1 ? "." : ",");
    	
    	return program;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new PrologScriptEngine();
    }
}
