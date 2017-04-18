package nars.util;

import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.ArrayTermVector;

import java.util.List;

/**
 * Created by me on 4/2/17.
 */
public enum JsonCompound  { ;


    public static GenericCompound the(JsonElement j) {

        Op op;
        Term[] subterms;
        if (j.isJsonArray()) {
            op = Op.PROD;
            List<Term> s = Lists.newArrayList(
                    Iterators.transform(
                            j.getAsJsonArray().iterator(), x ->
                                    JsonCompound.the(x)
                    )
            );
            subterms = s.toArray(new Term[s.size()]);
        } else if (j.isJsonPrimitive()) {
            op = Op.PROD;
            subterms = new Term[]{Atomic.the(j.toString())};
        } else if (j.isJsonObject()) {
            op = Op.SETe;
            subterms = j.getAsJsonObject().entrySet().stream().map(e -> {
                //return $.p($.the(e.getKey()), fromJSON(e.getValue()));
                return $.inh( the(e.getValue()), Atomic.the(e.getKey()) );
            }).toArray(Term[]::new);
        } else {// if (j.isJsonObject()) {
            throw new UnsupportedOperationException("TODO");
        }

        return new GenericCompound(op, new ArrayTermVector(subterms));
    }


    public static Compound the(String json) {
        return the( new Gson().fromJson(json, JsonElement.class) );
    }
}
