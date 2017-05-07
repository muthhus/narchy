package nars.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import nars.$;
import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.atom.Atomic;
import nars.term.compound.GenericCompound;
import nars.term.container.ArrayTermVector;

import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Created by me on 4/2/17.
 */
public enum JsonCompound  { ;


    public static GenericCompound the(JsonNode j) {

        Op op;
        Term[] subterms;
        if (j.isArray()) {
            op = Op.PROD;
            List<Term> s = Lists.newArrayList(
                    Iterators.transform(
                            j.iterator(), JsonCompound::the
                    )
            );
            subterms = s.toArray(new Term[s.size()]);
        } else if (j.isValueNode()) {
            op = Op.PROD;
            subterms = new Term[]{Atomic.the(j.toString())};
        } else if (j.isObject()) {
            throw new UnsupportedOperationException("TODO");
//            op = Op.SETe;
//            j.fields().forEachRemaining(...
//            subterms = StreamSupport.stream(j.spliterator(), false).map(e -> {
//                //return $.p($.the(e.getKey()), fromJSON(e.getValue()));
//                return $.inh( the(e.asText()), Atomic.the(e.getKey()) );
//            }).toArray(Term[]::new);
        } else {// if (j.isJsonObject()) {
            throw new UnsupportedOperationException("TODO");
        }

        return new GenericCompound(op, new ArrayTermVector(subterms));
    }


    public static Compound the(String json) {

        throw new UnsupportedOperationException("TODO");
        //return the( new Gson().fromJson(json, JsonElement.class) );
    }
}
