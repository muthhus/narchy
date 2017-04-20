/*
 * Here comes the text of your license
 * Each line should be prefixed with  * 
 */
package nars.op.data;

import nars.$;
import nars.term.Compound;
import nars.term.Term;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Produces canonical "Reflective-Narsese" representation of a parameter term
 * @author me
 */
public class reflect  {


    /**
     * <(*,subject,object) --> predicate>
     */
    @Nullable
    public static Term sop(Term subject, Term object, Term predicate) {
        return $.inh($.p(reflect(subject), reflect(object)), predicate);
    }

    @Nullable
    public static Term sopNamed(String operatorName, @NotNull Compound s) {
        //return Atom.the(Utf8.toUtf8(name));

        //return $.the('"' + t + '"');

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
        return $.inh($.p(reflect(s.get(0)), reflect(s.get(1))), $.quote(operatorName));
    }
    @Nullable
    public static Term sop(@NotNull Compound s, Term predicate) {
        return $.inh($.p(reflect(s.get(0)), reflect(s.get(1))), predicate);
    }
    @Nullable
    public static Term sop(String operatorName, @NotNull Compound c) {
        Term[] m = new Term[c.size()];
        for (int i = 0; i < c.size(); i++) {
            if ((m[i] = reflect(c.get(i))) == null)
                return null;
        }

        //return Atom.the(Utf8.toUtf8(name));

        //return $.the('"' + t + '"');

//        int olen = name.length();
//        switch (olen) {
//            case 0:
//                throw new RuntimeException("empty atom name: " + name);
//
////            //re-use short term names
////            case 1:
////            case 2:
////                return theCached(name);
//
//            default:
//                if (olen > Short.MAX_VALUE/2)
//                    throw new RuntimeException("atom name too long");

        //  }
        return $.inh($.p(m), $.quote(operatorName));
    }
    
    @Nullable
    public static Term reflect(Term node) {
        if (!(node instanceof Compound)) {
            return node;
        }
        Compound t = (Compound)node;
        switch (t.op()) {
            //case INH: return sop(t, "inheritance");
            //case SIM:  return sop(t, "similarity");
            default: return sop(t.op().toString(), t);
        }
        
    }


}
