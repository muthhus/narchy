package com.insightfullogic.slab.implementation;

import com.insightfullogic.slab.ConcreteCursor;
import com.insightfullogic.slab.InvalidInterfaceException;
import org.eclipse.collections.api.map.ImmutableMap;
import org.eclipse.collections.impl.factory.Maps;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.reflect.Modifier.isAbstract;
import static java.util.Arrays.asList;

public class TypeInspector {
    
    private final Class<?> klass;
    
    final List<Method> getters;
    final ImmutableMap<String, Method> setters;
    
    public TypeInspector(Class<?> klass) {
        this.klass = klass;
        if(!klass.isInterface() && !ConcreteCursor.class.isAssignableFrom(klass))
        	throw new InvalidInterfaceException("concrete classes must sublass ConcreteCursor");
        
        getters = findGetters();
        setters = findSetters();
        checkRemainingMethods(klass);
    }

	private void checkRemainingMethods(Class<?> klass) {
		List<Method> methods = new ArrayList<>(asList(klass.getDeclaredMethods()));
		methods.removeAll(getters);
		setters.forEachValue(methods::remove);
        for (Method method : methods)
			if (isAbstract(method.getModifiers()))
				throw new InvalidInterfaceException(klass.getName() + " has abstract methods that are neither getters nor setters");
	}

	private List<Method> findGetters() {
        List<Method> methods = new ArrayList<>();
        for (Method method : klass.getDeclaredMethods()) {
            String name = method.getName();
			if (!name.startsWith("get"))
                continue;

			ensureAbstract(method);
			doesntUseIndex(name);
            returnsPrimitive(method);
            hasNoParameters(method);
            methods.add(method);
        }
        return methods;
    }
	
    private static void ensureAbstract(Method method) {
		if (!isAbstract(method.getModifiers()))
			throw new InvalidInterfaceException(method + " must be abstract, since its a getter or setter");
	}

	private static void doesntUseIndex(String name) {
		if ("getIndex".equals(name))
			throw new InvalidInterfaceException("You can't declare an index field, since that name is used by Slab");
	}

	private static void hasNoParameters(Method method) {
        if (method.getParameterTypes().length != 0)
            throw new InvalidInterfaceException(method.getName() + " is a getter with one or more parameters");
    }

    private static void returnsPrimitive(Method method) {
        if (!method.getReturnType().isPrimitive())
        	throw new InvalidInterfaceException(method.getName() + " is a getter that doesn't return a primitive");
    }

    static Primitive getReturn(Method method) {
        return Primitive.of(method.getReturnType());
    }

	private ImmutableMap<String, Method> findSetters() {
		Map<String, Method> methods = new HashMap<>();
        for (Method method : klass.getDeclaredMethods()) {
            if (!method.getName().startsWith("set"))
                continue;

            ensureAbstract(method);
            returnsVoid(method);
            hasOnePrimitiveParameter(method);
            methods.put(method.getName(), method);
        }
        return Maps.immutable.ofMap(methods);
	}

    private static void hasOnePrimitiveParameter(Method method) {
		Class<?>[] parameters = method.getParameterTypes();
		if (parameters.length != 1)
			throw new InvalidInterfaceException(method.getName() + " is a setter with more than one parameter");
		
		if (!parameters[0].isPrimitive())
			throw new InvalidInterfaceException(method.getName() + " is a setter with a non-primitive parameter");
	}

	private static void returnsVoid(Method method) {
		if (method.getReturnType() != Void.TYPE)
			throw new InvalidInterfaceException(method.getName() + " is a setter that doesn't return void");
	}

	public int getSizeInBytes() {
        int total = 0;
        for (Method getter : getters) {
            total += getReturn(getter).sizeInBytes;
        }
        return total;
    }

    public int getFieldCount() {
        return getters.size();
    }

	public Method setterFor(Method getter) {
		String name = getter.getName().replaceFirst("get", "set");
		Method method = setters.get(name);
		if (method == null)
			throw new InvalidInterfaceException("Unable to find setter with name: " + name);
		return method;
	}

}
