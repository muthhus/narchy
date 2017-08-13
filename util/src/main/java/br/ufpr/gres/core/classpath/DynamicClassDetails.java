package br.ufpr.gres.core.classpath;

import br.ufpr.gres.ClassContext;
import br.ufpr.gres.ClassInfo;
import br.ufpr.gres.core.visitors.methods.RegisterInformationsClassVisitor;
import br.ufpr.gres.core.visitors.methods.empty.NullVisitor;
import org.objectweb.asm.ClassReader;

import java.io.IOException;

public class DynamicClassDetails extends ClassDetails {

    private final byte[] bytes;

    public static DynamicClassDetails get(Class c) {

        String path = path(c);
        System.err.println(path);
        return get(path);
    }

    public static DynamicClassDetails get(String path)  {
        final ClassContext context = new ClassContext();


        byte[] bytes = null;
        try {
            bytes = ClassLoader.getSystemResourceAsStream(path).readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final ClassReader first = new ClassReader(bytes);
        final NullVisitor nv = new NullVisitor();
        final RegisterInformationsClassVisitor mca = new RegisterInformationsClassVisitor(context, nv);

        first.accept(mca, ClassReader.EXPAND_FRAMES);

        return new DynamicClassDetails(context.getClassInfo(), bytes);
    }

    public DynamicClassDetails(ClassInfo info, byte[] bytes) {
        super(info);
        this.bytes = bytes;
    }

    @Override
    public byte[] getBytes() {
        return bytes;
    }
}
