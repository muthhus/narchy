package com.github.pfmiles.dropincc.impl.hotcompile;

import javax.tools.*;
import javax.tools.JavaFileObject.Kind;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A forwarding file manager which is used to decouple the file system
 * dependency. That is, the compilation process write compiled class files into
 * memory instead of file system.
 * 
 * <p>
 * These following methods would be invoked during the compilation progress, so
 * they should behave well for sure:
 * </p>
 * 
 * <pre>
 * close
 * flush
 * getClassLoader
 * getJavaFileForOutput
 * handleOption
 * hasLocation
 * inferBinaryName
 * list
 * </pre>
 * 
 * @author pf-miles
 * 
 */
public class MemClsFileManager extends ForwardingJavaFileManager<StandardJavaFileManager> {

    private Map<String, JavaMemCls> destFiles;
    private final Map<String, JavaStringSource> srcFiles;

    protected MemClsFileManager(StandardJavaFileManager fileManager, Map<String, JavaMemCls> destFiles, Map<String, JavaStringSource> srcFiles) {
        super(fileManager);
        this.destFiles = destFiles;
        this.srcFiles = srcFiles;
    }

    // redirects class file output to memory
    @Override
    public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling) throws IOException {
        if (!(Kind.CLASS == kind && StandardLocation.CLASS_OUTPUT == location))
            return super.getJavaFileForOutput(location, className, kind, sibling);
        if (destFiles.containsKey(className)) {
            return destFiles.get(className);
        } else {
            JavaMemCls file = new JavaMemCls(className);
            this.destFiles.put(className, file);
            return file;
        }
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.destFiles = null;
    }

    @Override
    public Iterable<JavaFileObject> list(Location location, String packageName, Set<Kind> kinds, boolean recurse) throws IOException {
        List<JavaFileObject> ret = new ArrayList<>();
        if ((StandardLocation.CLASS_OUTPUT == location || StandardLocation.CLASS_PATH == location) && kinds.contains(Kind.CLASS)) {
            for (Map.Entry<String, JavaMemCls> e : destFiles.entrySet()) {
                String pkgName = resolvePkgName(e.getKey());
                if (recurse) {
                    if (pkgName.contains(packageName))
                        ret.add(e.getValue());
                } else {
                    if (pkgName.equals(packageName))
                        ret.add(e.getValue());
                }
            }
        } else if (StandardLocation.SOURCE_PATH == location && kinds.contains(Kind.SOURCE)) {
            for (Map.Entry<String, JavaStringSource> e : srcFiles.entrySet()) {
                String pkgName = resolvePkgName(e.getKey());
                if (recurse) {
                    if (pkgName.contains(packageName))
                        ret.add(e.getValue());
                } else {
                    if (pkgName.equals(packageName))
                        ret.add(e.getValue());
                }
            }
        }
        // 也包含super.list
        Iterable<JavaFileObject> superList = super.list(location, packageName, kinds, recurse);
        if (superList != null)
            for (JavaFileObject f : superList)
                ret.add(f);
        return ret;
    }

    private static String resolvePkgName(String fullQualifiedClsName) {
        return fullQualifiedClsName.substring(0, fullQualifiedClsName.lastIndexOf('.'));
    }

    @Override
    public String inferBinaryName(Location location, JavaFileObject file) {
        if (file instanceof JavaMemCls) {
            return ((JavaMemCls) file).getClsName();
        } else if (file instanceof JavaStringSource) {
            return ((JavaStringSource) file).getClsName();
        } else {
            return super.inferBinaryName(location, file);
        }
    }

}
