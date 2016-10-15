package nars.term.compound;

import nars.Op;
import nars.term.Compound;
import nars.term.Term;
import nars.term.container.TermContainer;
import nars.term.visit.SubtermVisitorXY;
import org.eclipse.collections.api.bimap.BiMap;
import org.eclipse.collections.api.bimap.MutableBiMap;
import org.eclipse.collections.impl.factory.BiMaps;
import org.eclipse.collections.impl.map.mutable.primitive.ObjectByteHashMap;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static nars.$.$;

/**
 * Annotates a GenericCompound with cached data to accelerate pattern matching
 */
public class FastCompound extends GenericCompound {

    private final ObjectByteHashMap<Term> uniqueSubs;
    private final Term[] uniqueSubIndex;

    public FastCompound(Compound c) {
        this(c.op(), c.dt(), c.subterms());
        if (c.isNormalized())
            setNormalized();
    }

    public FastCompound(@NotNull Op op, int dt, @NotNull TermContainer subs) {
        super(op, dt, subs);

        uniqueSubs = new ObjectByteHashMap<>(volume());
        recurseTerms((x) -> uniqueSubs.getIfAbsentPut(x, ()->(byte)uniqueSubs.size()));

        new SubtermVisitorXY(term()) {

            @Override
            public SubtermVisitorXY.Next accept(int subterm, Compound superterm, int depth) {
                System.out.println(superterm + "(" + subterm + "): " + superterm.term(subterm) + ", depth=" + depth);
                return Next.Next;
            }
        };

        uniqueSubIndex = new Term[uniqueSubs.size()];
        uniqueSubs.forEachKeyValue((k,v) -> {
            uniqueSubIndex[v] = k;
        });

    }

    public void print() {
        System.out.println(toString() + "\n\tuniqueSubs=" + uniqueSubs);
    }

    public static void main(String[] args) {
        FastCompound f = new FastCompound(
                $("(&&,(MedicalCode-->MedicalIntangible),(MedicalIntangible-->#1),(SuperficialAnatomy-->#1),label(MedicalCode,MedicalCode),label(MedicalIntangible,MedicalIntangible),label(SuperficialAnatomy,SuperficialAnatomy))")
        );
        f.print();
    }
}
