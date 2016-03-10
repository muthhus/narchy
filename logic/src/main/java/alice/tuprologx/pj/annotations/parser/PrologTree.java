/*
 * PredicateTree.java
 *
 * Created on April 26, 2007, 5:07 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package alice.tuprologx.pj.annotations.parser;
import java.util.List;
/**
 *
 * @author maurizio
 */
public abstract class PrologTree {
    
    /** Creates a new instance of PredicateTree */
    public static class PredicateExpr extends PrologTree {
        
        public String name;
        public List<VariableExpr> variables;
        
        PredicateExpr(String name, List<VariableExpr> vars) {
            this.name = name;
            this.variables = vars;
        }
        
        PredicateExpr(List<VariableExpr> vars) {
            this.name = "";
            this.variables = vars;
        }
        
        public String toString() {
            return name+" ( "+variables+" ) ";
        }
    }
    
    public static class VariableExpr extends PrologTree {
        
        public String name;
        public List<Character> annotations;
        
        VariableExpr(String name, List<Character> ann) {
            this.name = name;
            this.annotations = ann;
        }
        
        public boolean equals(Object t) {
            if (!(t instanceof VariableExpr)) {
                return false;
            }                
            VariableExpr that = (VariableExpr)t;            
            return (that.name.equals(name));
        }
        
        public String toString() {
            return annotations+" "+name;
        }
    }
    
    public static class SignatureExpr extends PrologTree {
        
        public PredicateExpr inputTree;
        public PredicateExpr outputTree;
        public boolean multipleResult;
        
        SignatureExpr(PredicateExpr left, PredicateExpr right, boolean multiple) {
            this.inputTree = left;
            this.outputTree = right;
            this.multipleResult = multiple;
        }
        
        public String toString() {
            return inputTree + "->" + (multipleResult ? " { "+outputTree+" } " : " ( "+outputTree+" ) ");
        }
    }
    
}
