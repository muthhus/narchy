package alice.util.proxyGenerator;
import javax.tools.*;
import java.io.*;
import java.net.*;

class GeneratedClassFile extends SimpleJavaFileObject {
  
	private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

  public GeneratedClassFile() {
    super(URI.create("generated.class"), Kind.CLASS);
  }

  @Override
  public OutputStream openOutputStream() {
    return outputStream;
  }

  public byte[] getClassAsBytes() {
    return outputStream.toByteArray();
  }
}
  

/*
  This class is used to hold the generated class file. It presents a ByteArrayOutputStream to the JavaFileManager 
  in the openOutputStream() method. The URI here is not used, so I just specify "generated.class". 
  Once the Java source is compiled, we extract the class with getClassAsBytes().
*/
 