package nars.index;

import jcog.data.byt.RawByteSeq;
import jcog.data.random.XorShift128PlusRandom;
import nars.$;
import nars.IO;
import nars.NAR;
import nars.Task;
import nars.concept.AtomConcept;
import nars.concept.CompoundConcept;
import nars.concept.Concept;
import nars.concept.PermanentConcept;
import nars.concept.dynamic.DynamicConcept;
import nars.conceptualize.ConceptBuilder;
import nars.conceptualize.DefaultConceptBuilder;
import nars.index.term.map.MaplikeTermIndex;
import nars.index.term.tree.TermKey;
import nars.nar.Default;
import nars.op.data.differ;
import nars.op.data.intersect;
import nars.op.data.union;
import nars.term.Term;
import nars.term.Termed;
import nars.term.transform.Functor;
import nars.time.FrameTime;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.cache.impl.DecoratedCache;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;



public class InfinispanIndex extends MaplikeTermIndex {

    public static final String INIT = "init";

    static final Logger logger = LoggerFactory.getLogger(InfinispanIndex.class);

    private final Cache<RawByteSeq, Termed> concepts;
    private final DecoratedCache conceptsLocal;
    private final AdvancedCache conceptsLocalNoResult;

    //private final Set<Term> permanents = new ConcurrentHashSet(1024);


    public InfinispanIndex(ConceptBuilder conceptBuilder) {
        super(conceptBuilder);

        GlobalConfiguration global = new GlobalConfigurationBuilder()
                .serialization()
                //.addAdvancedExternalizer(new TaskExternalizer())
                .addAdvancedExternalizer(new PermanentConceptExternalizer())
                .addAdvancedExternalizer(new ConceptExternalizer())
                .build();



        Configuration infinispanConfiguration = new ConfigurationBuilder()
                .unsafe()
                //.versioning().disable()
                .storeAsBinary().storeKeysAsBinary(true).storeValuesAsBinary(true)
                .persistence()
                    //.passivation(true)
                    .addSingleFileStore().location("/tmp/concepts")
                //cb.locking().concurrencyLevel(1);
                //cb.customInterceptors().addInterceptor();
                .jmxStatistics().disable()
                .build();



        DefaultCacheManager cm = new DefaultCacheManager(global, infinispanConfiguration);

        concepts = cm.getCache();


        this.conceptsLocal = new DecoratedCache<>(
                concepts.getAdvancedCache(),
                Flag.CACHE_MODE_LOCAL, /*Flag.SKIP_LOCKING,*/ Flag.SKIP_OWNERSHIP_CHECK,
                Flag.SKIP_REMOTE_LOOKUP);
        this.conceptsLocalNoResult = conceptsLocal.withFlags(Flag.IGNORE_RETURN_VALUES, Flag.SKIP_CACHE_LOAD, Flag.SKIP_REMOTE_LOOKUP);

//        Runtime.getRuntime().addShutdownHook(new Thread(()->{
//            stop();
//        }));
    }

    @Override
    public @Nullable Termed get(@NotNull Term t, boolean createIfMissing) {
        RawByteSeq key = key(t);
        if (createIfMissing) {
            return concepts.computeIfAbsent(key, (x) -> conceptBuilder().apply(t));
        } else {
            return concepts.get(key);
        }
    }

    static RawByteSeq key(@NotNull Term t) {
        return new RawByteSeq(TermKey.term(t));
    }

    @Override
    public void set(@NotNull Term src, Termed target) {
        RawByteSeq k = key(src);
        conceptsLocalNoResult.put(k, target); //replace
    }

    @Override
    public void onStateChanged(Concept c) {

        //handle attached pending tasks
        List<? extends Task> pending = c.remove(INIT);
        if (pending!=null) {
            nar.inputLater(pending);
        }

        commit(c);
    }

    @Override public void commit(Concept c) {
        RawByteSeq key = key(c.term());
        concepts.getAdvancedCache().replace(key, c);

    }

    //    public synchronized void stop() {
//        logger.info("stop: {}", concepts );
////
////        //remove permanents to avoid their persistence
////        /*for (Term p : permanents) {
////            if (concepts.remove(key(p))==null)
////                logger.warn("{} not in cache on removal", p) ;
////        }*/
////
//
//        //concepts.clear();
//
//        concepts.st
//    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void forEach(@NotNull Consumer<? super Termed> c) {
        concepts.forEach((k, v) -> {
            c.accept(v);
        });
    }

    @Override
    public int size() {
        return concepts.size();
    }

    @Override
    public @NotNull String summary() {
        return "size=" + concepts.size();
    }

    @Override
    public void remove(@NotNull Term entry) {
        conceptsLocalNoResult.remove(key(entry));
    }

    public static void main(String[] args) {

        InfinispanIndex index = new InfinispanIndex(new DefaultConceptBuilder());
        NAR nar = new Default(1024, 1, 1, 3,
                new XorShift128PlusRandom(1), index, new FrameTime());

        nar.log();
        nar.input("(x,y).", "x:b.", "b(\"" + new Date() + "\").");
        nar.run(10);
        System.out.println(nar.concepts.summary());

        //nar.concepts.print(System.out);



    }

//    public class TaskExternalizer implements AdvancedExternalizer<Task> {
//
//        @Override
//        public Set<Class<? extends Task>> getTypeClasses() {
//            return Set.of(Task.class);
//        }
//
//        @Override
//        public Integer getId() {
//            return 4443;
//        }
//
//        @Override
//        public void writeObject(ObjectOutput output, Task t) throws IOException {
//            IO.writeTask(output, t);
//        }
//
//        @Override
//        public Task readObject(ObjectInput input) throws IOException, ClassNotFoundException {
//            MutableTask t = IO.readTask(input, InfinispanIndex.this);
//            return t;
//        }
//    }

    public class ConceptExternalizer implements AdvancedExternalizer<Concept> {


        @Override
        public void writeObject(ObjectOutput output, Concept c) throws IOException {
            List<Task> ll = $.newArrayList(); //TODO do this streaming by using a 'null task' terminator
            c.forEachTask(true, true, true, true, ll::add);

            IO.writeTerm(output, c.term());

            int s = ll.size();
            output.writeInt(s);
            for (int i = 0, llSize = s; i < llSize; i++) {
                IO.writeTask(output, ll.get(i));
            }
        }

        @Override
        public Concept readObject(ObjectInput input) throws IOException, ClassNotFoundException {

            Term t = IO.readTerm(input, InfinispanIndex.this);

            Concept c = (Concept) conceptBuilder().apply(t);

            int numTasks = input.readInt();
            if (numTasks > 0) {
                List<Task> toInput = $.newArrayList(numTasks);
                for (int i = 0; i < numTasks; i++) {
                    Task x = IO.readTask(input, InfinispanIndex.this);
                    //if (c != null)
                    toInput.add(x);
                }
                c.put(INIT, toInput);
            }

            return c;
        }

        @Override
        public Set<Class<? extends Concept>> getTypeClasses() {
            return Set.of(AtomConcept.class, CompoundConcept.class, DynamicConcept.class );
        }

        @Override
        public Integer getId() {
            return 4444;
        }
    }

    public class PermanentConceptExternalizer implements AdvancedExternalizer<Concept> {

        @Override
        public void writeObject(ObjectOutput output, Concept c) throws IOException {
            //just write the term which will be used as a placeholder.
            //an actual permanent concept instance, once inserted, will replace it in the cache
            IO.writeTerm(output, c.term());
        }

        @Override
        public Concept readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            Term t = IO.readTerm(input, InfinispanIndex.this);
            return (Concept) conceptBuilder().apply(t);
        }

        @Override
        public Set<Class<? extends Concept>> getTypeClasses() {
            return Set.of(PermanentConcept.class, intersect.class, differ.class, union.class,Functor.LambdaFunctor.class);
        }

        @Override
        public Integer getId() {
            return 4445;
        }
    }
}
