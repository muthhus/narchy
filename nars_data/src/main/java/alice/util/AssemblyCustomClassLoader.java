//package alice.util;
//
//import java.net.URL;
//
//public class AssemblyCustomClassLoader extends java.net.URLClassLoader
//{
//  public AssemblyCustomClassLoader(cli.System.Reflection.Assembly asm, URL[] urls)
//  {
//    super(new java.net.URL[0], new ikvm.runtime.AssemblyClassLoader(asm));
//    // explicitly calling addURL() is safer than passing it to the super constructor,
//    // because this class loader instance may be used during the URL construction.
//    for (URL url : urls) {
//    	addURL(url);
//	}
//  }
//
//  public void addUrl(URL url)
//  {
//	  addURL(url);
//  }
//}