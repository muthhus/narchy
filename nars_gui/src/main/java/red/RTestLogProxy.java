//    RED - A Java Editor Library
//    Copyright (C) 2003  Robert Lichtenberger
//
//    This library is free software; you can redistribute it and/or
//    modify it under the terms of the GNU Lesser General Public
//    License as published by the Free Software Foundation; either
//    version 2.1 of the License, or (at your option) any later version.
//
//    This library is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
//    Lesser General Public License for more details.
//
//    You should have received a copy of the GNU Lesser General Public
//    License along with this library; if not, write to the Free Software
//    Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 
package red;

import java.lang.reflect.*;
import java.util.*;

/** Log proxy for regression tests.
  * This auxiliary class is an invokation proxy that logs calls to interfaces.
  * @tier test
  */
public class RTestLogProxy implements InvocationHandler  {
	public static Object newInstance(Object obj, RTestLogProxy proxy) {
		return Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(), proxy);
	}

	/** Create a new RTestLogProxy 
	  * @param obj The object this proxy will be connected with
	  * @param logMethodsFrom The class whose methods this proxy will log
	  */
	public RTestLogProxy(Object obj) {
		fObj = obj;
		fClasses = new HashSet();
		fMethodIgnores = new HashSet();
		clear();
	}
	
	public void addLogClass(Class clToLogMethodsFor) {
		fClasses.add(clToLogMethodsFor);
	}
	
	public void addIgnoreMethod(String methodName) {
		fMethodIgnores.add(methodName);
	}
	
	private void appendArray(Object[] arr) {
		for(int i=0; i<arr.length; i++) {
			if(i>0) {
				fLog.append(", ");
			}
			Object o = arr[i];
			if (o != null) {
				if (o instanceof Object[]) {
					fLog.append('[');
					appendArray((Object []) o);
					fLog.append(']');
				}
				else {
					fLog.append(o.toString());
				}
			}
			else {
				fLog.append("null");
			}
		}
	}

	public Object invoke(Object proxy, Method m, Object[] args) throws Throwable {
		Object result;
		if (fClasses.contains(m.getDeclaringClass()) && !fMethodIgnores.contains(m.getName())) {
			fTime = System.currentTimeMillis();
			if (fLog.length() > 0) {
				fLog.append('\n');
			}
			fLog.append(m.getName()).append('(');
			if (args != null) {
				appendArray(args);
			}
			fLog.append(')');
		}
		else {
//			System.err.println("Got an ignorable method from " + m.getDeclaringClass().getName());
		}
		if (m.getName().equals("equals") && args.length > 0 && args[0] instanceof Proxy) {
			InvocationHandler h = Proxy.getInvocationHandler(args[0]);
			if (h instanceof RTestLogProxy) {
				args[0] = ((RTestLogProxy) h).fObj;
			}
		}
		return m.invoke(fObj, args);
	}
	
	public String toString() {
		return fLog.toString();
	}
	
	public void clear() {
		fLog = new StringBuffer();
	}
	
	public long getTime() {
		return fTime;
	}
	
	private StringBuffer fLog;
	private final Object fObj;
	private final HashSet fClasses;
	private final HashSet fMethodIgnores;
	private long fTime;
}
