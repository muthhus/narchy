package nars.term.util;

import jcog.bag.impl.hijack.HijackMemoize;
import nars.Op;
import nars.Param;
import nars.conceptualize.ConceptBuilder;
import nars.index.term.AppendProtoCompound;
import nars.index.term.ProtoCompound;
import nars.index.term.TermIndex;
import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static nars.Op.False;

/**
 * note: has an internal cache by extending the MaplikeTermIndex
 */
public class StaticTermIndex extends /*Maplike*/TermIndex {


//        public StaticTermBuilder() {
//            super(new ConceptBuilder.NullConceptBuilder());
//        }

//        @Override
//        protected Term the(ProtoCompound c) {
////            if (Math.random() < 0.01f)
////                System.out.println(build.summary());
//            return super.the(c);
//        }


    @Override
    public @NotNull ConceptBuilder conceptBuilder() {
        return ConceptBuilder.Null;
    }

    @Override
    public
    @Nullable
    Termed get(@NotNull Term t, boolean createIfMissing) {
        return createIfMissing ? t : null;
    }

    @Override
    public int size() {
        return 0;
    }


    @Override
    public @NotNull String summary() {
        return "";
    }

    @Override
    public void remove(@NotNull Term entry) {

    }


    @Override
    public void clear() {

    }

    @Override
    public void forEach(Consumer<? super Termed> c) {

    }


    @Nullable
    @Override
    public void set(@NotNull Term s, Termed t) {
        throw new UnsupportedOperationException();
    }


}
