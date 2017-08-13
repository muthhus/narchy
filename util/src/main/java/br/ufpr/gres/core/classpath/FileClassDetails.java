package br.ufpr.gres.core.classpath;//package br.ufpr.gres.core.classpath;
//
//import br.ufpr.gres.ClassInfo;
//import br.ufpr.gres.testcase.classloader.ForeingClassLoader;
//import org.apache.commons.io.FileUtils;
//
//import java.io.File;
//import java.io.IOException;
//
///** class loaded from file */
//public class FileClassDetails extends ClassDetails {
//
//    private final File file;
//
//    public FileClassDetails(ClassInfo classInfo, File root, Class clazz) throws ClassNotFoundException {
//        super(classInfo, clazz);
//        this.file = file;
////                    boolean isJavaFile = file.getName().contains(".java");
//    }
//
//    public byte[] getBytes() throws IOException {
//        return FileUtils.readFileToByteArray(file);
//    }
//
//    public File getFile() {
//        return this.file;
//    }
//}
