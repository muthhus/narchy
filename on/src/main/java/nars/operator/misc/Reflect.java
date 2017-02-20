/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.operator.misc;

import nars.language.*;
import nars.operator.SynchronousFunctionOperator;
import nars.storage.Memory;

/**
 * Produces canonical "Reflective-Narsese" representation of a parameter term
 * @author me
 */
public class Reflect extends SynchronousFunctionOperator {


    /*
     <(*,<(*,good,property) --> inheritance>,(&&,<(*,human,good) --> product>,<(*,(*,human,good),inheritance) --> inheritance>)) --> conjunction>.
    */
    
    public Reflect() {
        super("^reflect");
    }

    final static String requireMessage = "Requires 1 Term argument";    
    
    
    @Override
    protected Term function(Memory memory, Term[] x) {
        
        if (x.length!=1) {
            throw new RuntimeException(requireMessage);
        }

        Term content = x[0];


        return getMetaTerm(content);
    }


    /**
     * <(*,subject,object) --> predicate>
     */
    public static Term sop(Term subject, Term object, Term predicate) {
        return Inheritance.make(Product.make(getMetaTerm(subject),getMetaTerm(object)), predicate);
    }
    public static Term sop(Statement s, String operatorName) {
        /*Term x = atoms.get(name);
        if (x != null) return x;
        x = new Term(name);
        atoms.put(name, x);
        return x;*/
        return Inheritance.make(Product.make(getMetaTerm(s.getSubject()),getMetaTerm(s.getPredicate())), new Term(operatorName));
    }
    public static Term sop(Statement s, Term predicate) {
        return Inheritance.make(Product.make(getMetaTerm(s.getSubject()),getMetaTerm(s.getPredicate())), predicate);
    }
    public static Term sop(String operatorName, Term... t) {
        Term[] m = new Term[t.length];
        int i = 0;
        for (Term x : t)
            m[i++] = getMetaTerm(x);
        
        /*Term x = atoms.get(name);
        if (x != null) return x;
        x = new Term(name);
        atoms.put(name, x);
        return x;*/
        return Inheritance.make(Product.make(m), new Term(operatorName));
    }
    
    public static Term getMetaTerm(Term node) {
        if (!(node instanceof CompoundTerm)) {
            return node;
        }
        CompoundTerm t = (CompoundTerm)node;
        switch (t.operator()) {
            case INHERITANCE: return sop((Inheritance)t, "inheritance");
            case SIMILARITY:  return sop((Similarity)t, "similarity");
            default: return sop(t.operator().toString(), t.term);                
        }
        
    }

    @Override
    protected Term getRange() {
        return new Term("reflect");
        /*Term x = atoms.get(name);
        if (x != null) return x;
        x = new Term(name);
        atoms.put(name, x);
        return x;*/
    }
    
    
}
