//package clojure.lang;
//
//import java.io.ObjectStreamException;
//import java.io.Serializable;
//
//public class Symbol extends AFn implements IObj, Comparable, Named, Serializable, IHashEq {
//    final String ns;
//    final String name;
//    private int _hasheq;
//    final IPersistentMap _meta;
//    transient String _str;
//
//    public String toString() {
//        String str = _str;
//        if (str == null) {
//            String ns = this.ns;
//            this._str = str = (ns != null) ? ns + "/" + name : name;
//        }
//        return str;
//    }
//
//    public String getNamespace() {
//        return ns;
//    }
//
//    public String getName() {
//        return name;
//    }
//
//    // the create thunks preserve binary compatibility with code compiled
//// against earlier version of Clojure and can be removed (at some point).
//    static public Symbol create(String ns, String name) {
//        return Symbol.intern(ns, name);
//    }
//
//    static public Symbol create(String nsname) {
//        return Symbol.intern(nsname);
//    }
//
//    static public Symbol intern(String ns, String name) {
//        return new Symbol(ns, name);
//    }
//
//    static public Symbol intern(String nsname) {
//        int i = nsname.indexOf('/');
//        if (i == -1 || nsname.equals("/"))
//            return new Symbol(null, nsname);
//        else
//            return new Symbol(nsname.substring(0, i), nsname.substring(i + 1));
//    }
//
//    private Symbol(String ns_interned, String name_interned) {
//        this.name = name_interned;
//        this.ns = ns_interned;
//        this._meta = null;
//    }
//
//    public boolean equals(Object o) {
//        if (this == o)
//            return true;
//        if (!(o instanceof Symbol))
//            return false;
//
//        Symbol symbol = (Symbol) o;
//
//        return Util.equals(ns, symbol.ns) && name.equals(symbol.name);
//    }
//
//
//    public int hashCode() {
//        //reversed seed=NS
//        return Util.hashCombine(Util.hash(ns), name.hashCode());
//    }
//
//    public int hasheq() {
//        int hasheq = _hasheq;
//        if (hasheq == 0) {
//            //reversed seed=NS
//            this._hasheq = hasheq = Util.hashCombine(Util.hash(ns), Murmur3.hashUnencodedChars(name));
//        }
//        return hasheq;
//    }
//
//    public IObj withMeta(IPersistentMap meta) {
//        return new Symbol(meta, ns, name);
//    }
//
//    private Symbol(IPersistentMap meta, String ns, String name) {
//        this.name = name;
//        this.ns = ns;
//        this._meta = meta;
//    }
//
//    public int compareTo(Object o) {
//        Symbol s = (Symbol) o;
//        if (this.equals(o))
//            return 0;
//        String ns = this.ns;
//        String sns = s.ns;
//        if (ns == null && sns != null)
//            return -1;
//        if (ns != null) {
//            if (sns == null)
//                return 1;
//            int nsc = ns.compareTo(sns);
//            if (nsc != 0)
//                return nsc;
//        }
//        return this.name.compareTo(s.name);
//    }
//
//    private Object readResolve() throws ObjectStreamException {
//        return intern(ns, name);
//    }
//
//    public Object invoke(Object obj) {
//        return RT.get(obj, this);
//    }
//
//    public Object invoke(Object obj, Object notFound) {
//        return RT.get(obj, this, notFound);
//    }
//
//    public IPersistentMap meta() {
//        return _meta;
//    }
//}
