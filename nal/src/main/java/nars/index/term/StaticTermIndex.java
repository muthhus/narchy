package nars.index.term;

import nars.term.Term;
import nars.term.Termed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.stream.Stream;


public class StaticTermIndex extends TermIndex {


//        public StaticTermBuilder() {
//            super(new ConceptBuilder.NullConceptBuilder());
//        }

//        @Override
//        protected Term the(ProtoCompound c) {
////            if (Math.random() < 0.01f)
////                System.out.println(builder.summary());
//            return super.the(c);
//        }


    @Override
    public Stream<Termed> stream() {
        return Stream.empty();
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
