/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package alice.tuprolog.scriptengine;

import alice.tuprolog.*;
import alice.tuprolog.event.ExceptionEvent;
import alice.tuprolog.event.ExceptionListener;
import alice.tuprolog.event.OutputEvent;
import alice.tuprolog.event.OutputListener;
import alice.tuprolog.lib.IOLibrary;
import alice.tuprolog.lib.InvalidObjectIdException;
import alice.tuprolog.lib.OOLibrary;
import alice.util.InputStreamAdapter;

import javax.script.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.List;
import java.util.Map;


/**
 * Implementation of the interface ScriptEngine for tuProlog
 *
 * @author Andrea Bucaletti
 */
public class PrologScriptEngine implements ScriptEngine, ExceptionListener, OutputListener {
	
	/*
	 * Engine context keys-
	 */
    public static final String CONTEXT = "context";
    public static final String THEORY = "theory";
    public static final String IS_SUCCESS =  "isSuccess";
    public static final String IS_HALTED = "isHalted";
    public static final String HAS_OPEN_ALTERNATIVES = "hasOpenAlternatives";
    
    // Solution variables bound during the last call of eval(..)
    protected List<Var> solveVars;
    
    // The last evaluated script
    protected String previousScript;
    
    /* 	This is used to decide if the text call of eval(..) is going to use Prolog.solve()
    	or Prolog.solveNext() */
    protected boolean useSolveNext;
   
    /* The default script context */
    protected ScriptContext defaultContext;
    
    /* And instance of prolog used to solve the given scripts */
    protected Prolog prolog;
    
    /* Current Standard Output and Error */
    protected Writer outputWriter, errorWriter;
    
    public PrologScriptEngine() {
        prolog = new Prolog();
        prolog.addExceptionListener(this);
        prolog.addOutputListener(this);

        defaultContext = new SimpleScriptContext();     
        
        useSolveNext = false;
        previousScript = null;
        solveVars = null;
    } 

    @Override
    public Object eval(String string) throws ScriptException {
        return eval(string, getContext());
    }

    @Override
    public Object eval(Reader reader) throws ScriptException {
        return eval(reader, getContext());
    }
    
    @Override
    public Object eval(String script, ScriptContext sc) throws ScriptException {
    	
    	setupStandardIO(sc);
    	
    	/*
        As the jsr-223 part SCR.4.3.4.1.2 Script Execution :
        "In all cases, the ScriptContext used during a script execution must be
        a value in the Engine Scope of the ScriptEngine whose key is the
        String "context"     
         */
        

        sc.getBindings(ScriptContext.ENGINE_SCOPE).put(CONTEXT, sc);
        return eval(script, sc.getBindings(ScriptContext.ENGINE_SCOPE));
    }

    @Override
    public Object eval(Reader reader, ScriptContext sc) throws ScriptException {           
    	
    	setupStandardIO(sc);
    	
    	/*
        As the jsr-223 part SCR.4.3.4.1.2 Script Execution :
        "In all cases, the ScriptContext used during a script execution must be
        a value in the Engine Scope of the ScriptEngine whose key is the
        String "context"     
         */        
        
        sc.getBindings(ScriptContext.ENGINE_SCOPE).put(CONTEXT, sc);
        return eval(reader, sc.getBindings(ScriptContext.ENGINE_SCOPE));
    }    
    /**
     * Evaluates a script. After the evaluation, informations
     * about the found solution are put in the Bindings passed as parameter. The key pair values
     * that contains general informations about the solution are:
     * { "isSuccess", Boolean }
     * { "isHalted", Boolean }
     * { "hasOpenAlternatives", Boolean }
     * If the solution has bound variables, those values are put in the engine context
     * as the key pair { String variableName, String value }. Those keys are removed from the context 
     * when this method is called again.
     * If the same script is executed 2 or more times in row, and the solution has open alternatives, solveNext()
     * is used instead of solve() after the first evaluation.
     * @param script The script to be executed.  
     * @param bindings The Bindings to be used as engine context for evaluation
     * @return true if the script is executed correctly.
     */
    @Override
    public Object eval(String script, Bindings bindings) throws ScriptException {
    	String theory = (String)bindings.get(THEORY);
        Solution info = null;
        
        /*
        As the jsr-223 part SCR.4.2.6 Bindings :
        "Each Java Script Engine has a Bindings known as its Engine Scope
        containing the mappings of script variables to their values. The
        values are often Objects in the host application. Reading the value of
        a key in the Engine Scope of a ScriptEngine returns the value of the
        corresponding script variable. Adding an Object as a value in the
        scope usually makes the object available in scripts using the specified
        key as a variable name. 
        
        So, all the objects in the engine scope are registered using the JavaLibrary,
        if available. This is done using the method 
            boolean register(Struct id, Object obj)
        of the JavaLibrary class. Any exception raised by this method will be
        forwarded, and the Object won't be registered.
         */
        
        OOLibrary ooLib = (OOLibrary) prolog.library("alice.tuprolog.lib.OOLibrary");
        
        if(ooLib != null) {
            for(Map.Entry<String, Object> keyPair: bindings.entrySet()) {
                try {
                    ooLib.register(new Struct(keyPair.getKey()), keyPair.getValue());
                }
                catch(InvalidObjectIdException ex) {
                    throw new ScriptException("Could not register object(" + keyPair.getKey() + "): " + ex.getMessage());
                }
            }
        }
        
        try {
            
            if(!script.equals(previousScript))
                useSolveNext = false;
            
            if(theory != null)
                prolog.setTheory(new Theory(theory));

            info = useSolveNext ? prolog.solveNext() : prolog.solve(script);
           
            previousScript = script;
            
            if(solveVars != null)
	            for(Var v : solveVars) 
	                bindings.remove(v.getName());

            bindings.put(IS_SUCCESS, info.isSuccess());
            bindings.put(IS_HALTED, info.isHalted());
            bindings.put(HAS_OPEN_ALTERNATIVES, info.hasOpenAlternatives());
            
            if(info.isSuccess()) {
                solveVars = info.getBindingVars();
                for(Var v : solveVars)            
                    bindings.put(v.getName(), v.getTerm().toString());             
            }
            
            useSolveNext = info.hasOpenAlternatives();
            
            return true;
        }
        catch(NoSolutionException | InvalidTheoryException | 
                MalformedGoalException | NoMoreSolutionException ex) {
            throw new ScriptException(ex);
        }
    }

    @Override
    public Object eval(Reader reader, Bindings bndngs) throws ScriptException {
        BufferedReader bReader = new BufferedReader(reader);
        String script = "";
        try {
            while(bReader.ready()) {
                script += bReader.readLine();
            }
        }
        catch(IOException ex) {
            throw new ScriptException(ex);
        }
        return eval(script, bndngs);
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return PrologScriptEngineFactory.DEFAULT_FACTORY;
    }

    @Override
    public void put(String key, Object o) {
       getBindings(ScriptContext.ENGINE_SCOPE).put(key, o);
    }

    @Override
    public Object get(String key) {
        return getBindings(ScriptContext.ENGINE_SCOPE).get(key);
    }

    @Override
    public Bindings getBindings(int i) {
        return getContext().getBindings(i);
    }

    @Override
    public void setBindings(Bindings bndngs, int i) {
        getContext().setBindings(bndngs, i);
    }

    @Override
    public ScriptContext getContext() {
        return defaultContext;
    }

    @Override
    public void setContext(ScriptContext sc) {
    	if(sc == null)
    		throw new NullPointerException("Given ScriptContext is null");
        defaultContext = sc;
    }
    
    /**
     * Sets the IOLibray's standard input/output with the streams specified in the ScriptContext
     * @param sc the ScriptContext to use for the next evaluation
     */
    private void setupStandardIO(ScriptContext sc) {
        IOLibrary ioLib = (IOLibrary) prolog.library("alice.tuprolog.lib.IOLibrary");
        
        if(ioLib != null) {
        	ioLib.setStandardInput(new InputStreamAdapter(sc.getReader()));
        	outputWriter = sc.getWriter();
        	errorWriter = sc.getErrorWriter();
        }    	
    }

	@Override
	public void onException(ExceptionEvent e) {
		try {
			if(errorWriter != null) {
				errorWriter.write(e.getMsg() + '\n');
				errorWriter.flush();
			}
		}
		catch(IOException ex) {}
	}

	@Override
	public void onOutput(OutputEvent e) {
		try {
			if(outputWriter != null) {
				outputWriter.write(e.getMsg());
			}
		}
		catch(IOException ex) {}
	}
    
}
