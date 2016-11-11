package com.github.pfmiles.dropincc.impl.hotcompile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

import static com.github.pfmiles.dropincc.impl.hotcompile.JavaStringSource.SLASHDOT;

/**
 * Represents compiled java class files in the memory.
 * 
 * @author pf-miles
 * 
 */
public class JavaMemCls extends SimpleJavaFileObject {


    private String clsName;
    private final ByteArrayOutputStream bos;

    protected JavaMemCls(String name) {
        super(URI.create("string:///" + SLASHDOT.matcher(name).replaceAll("/") + Kind.CLASS.extension), Kind.CLASS);
        this.bos = new ByteArrayOutputStream();
    }

    public byte[] getClsBytes() {
        return this.bos.toByteArray();
    }

    @Override
    public OutputStream openOutputStream() throws IOException {
        return bos;
    }

    public String getClsName() {
        return clsName;
    }
}
