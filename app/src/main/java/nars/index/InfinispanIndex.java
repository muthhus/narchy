package nars.index;

import jcog.data.byt.RawByteSeq;
import jcog.data.random.XorShift128PlusRandom;
import nars.$;
import nars.IO;
import nars.NAR;
import nars.Task;
import nars.concept.*;
import nars.concept.util.ConceptBuilder;
import nars.index.term.TermIndex;
import nars.index.term.map.MaplikeTermIndex;
import nars.index.term.tree.TermKey;
import nars.nar.Default;
import nars.nar.util.DefaultConceptBuilder;
import nars.task.MutableTask;
import nars.term.Term;
import nars.term.Termed;
import nars.time.FrameTime;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.AdvancedExternalizer;
import org.infinispan.commons.util.concurrent.ConcurrentHashSet;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


public class InfinispanIndex extends MaplikeTermIndex {

    static final Logger logger = LoggerFactory.getLogger(InfinispanIndex.class);

    private final Cache<RawByteSeq, Termed> concepts;

    private final Set<Term> permanents = new ConcurrentHashSet(1024);


    public InfinispanIndex(ConceptBuilder conceptBuilder) {
        super(conceptBuilder);

        GlobalConfiguration global = new GlobalConfigurationBuilder()
                .serialization()
                //.classResolver(new SimpleClassResolver(false,getClass().getClassLoader()))
                .addAdvancedExternalizer(new TaskExternalizer())
                //.addAdvancedExternalizer(new PermanentConceptExternalizer())
                .addAdvancedExternalizer(new ConceptExternalizer())
                .build();


        Configuration infinispanConfiguration = new ConfigurationBuilder()
                .unsafe()
                .versioning().disable()
                .storeAsBinary().storeKeysAsBinary(true).storeValuesAsBinary(true)
                .persistence().passivation(true).addSingleFileStore().location("/tmp/concepts")
                //cb.locking().concurrencyLevel(1);
                //cb.customInterceptors().addInterceptor();
                .jmxStatistics().disable()
                .build();



        DefaultCacheManager cm = new DefaultCacheManager(global, infinispanConfiguration);

        concepts = cm.getCache();

        Runtime.getRuntime().addShutdownHook(new Thread(()->{
            stop();
        }));
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
        if (src instanceof PermanentConcept) {
            permanents.add(src);
            concepts.put(k, target); //replace
        } else
            concepts.putIfAbsent(k, target);
    }


    public synchronized void stop() {
        logger.info("stop: {}", concepts );

        //remove permanents to avoid their persistence
        for (Term p : permanents) {
            if (concepts.remove(key(p))==null)
                logger.warn("{} not in cache on removal", p) ;
        }

        concepts.shutdown();
    }

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
        concepts.remove(key(entry));
    }

    public static void main(String[] args) {

        InfinispanIndex index = new InfinispanIndex(new DefaultConceptBuilder());
        NAR nar = new Default(1024, 1, 1, 3,
                new XorShift128PlusRandom(1), index, new FrameTime());

        nar.input("(x,y).", "a:b.");

        nar.concepts.print(System.out);

    }

    public class TaskExternalizer implements AdvancedExternalizer<Task> {

        @Override
        public Set<Class<? extends Task>> getTypeClasses() {
            return Set.of(Task.class);
        }

        @Override
        public Integer getId() {
            return 4443;
        }

        @Override
        public void writeObject(ObjectOutput output, Task t) throws IOException {
            IO.writeTask(output, t);
        }

        @Override
        public Task readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            MutableTask t = IO.readTask(input, InfinispanIndex.this);
            return t;
        }
    }

    public class ConceptExternalizer implements AdvancedExternalizer<Concept> {

        @Override
        public void writeObject(ObjectOutput output, Concept c) throws IOException {
            List<Task> ll = $.newArrayList(); //TODO do this streaming by using a 'null task' terminator
            c.forEachTask(true, true, true, true, ll::add);

            IO.writeTerm(output, c.term());

            output.writeInt(ll.size());
            for (Task x : ll) {
                IO.writeTask(output, x);
            }
        }

        @Override
        public Concept readObject(ObjectInput input) throws IOException, ClassNotFoundException {

            Term t = IO.readTerm(input, InfinispanIndex.this);
            Concept c = (Concept) get(t, true);

            int numTasks = input.readInt();
            for (int i = 0; i < numTasks; i++) {
                Task x = IO.readTask(input, InfinispanIndex.this);
                if (c != null)
                    nar.input(x);
            }

            return c;
        }

        @Override
        public Set<Class<? extends Concept>> getTypeClasses() {
            return Set.of(AtomConcept.class, CompoundConcept.class);
        }

        @Override
        public Integer getId() {
            return 4444;
        }
    }

    public class PermanentConceptExternalizer implements AdvancedExternalizer<Concept> {

        @Override
        public void writeObject(ObjectOutput output, Concept c) throws IOException {
            output.writeUTF(c.getClass().getName());
        }

        @Override
        public Concept readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            //Term t = IO.readTerm(input, InfinispanIndex.this);
            String className = input.readUTF();
            try {
                return (Concept) Class.forName(className).newInstance();
            } catch (InstantiationException e) {
                return null;
            } catch (IllegalAccessException e) {
                return null;
            }
            //return null; //(Concept) get(t, false);
        }

        @Override
        public Set<Class<? extends Concept>> getTypeClasses() {
            return Set.of(PermanentConcept.class, Functor.class, Functor.LambdaFunctor.class);
        }

        @Override
        public Integer getId() {
            return 4445;
        }
    }
}
