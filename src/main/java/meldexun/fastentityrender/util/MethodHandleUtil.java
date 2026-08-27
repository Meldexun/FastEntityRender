package meldexun.fastentityrender.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MethodHandleUtil {

	private static final Lookup LOOKUP;
	static {
		try {
			Field IMPL_LOOKUP = Lookup.class.getDeclaredField("IMPL_LOOKUP");
			IMPL_LOOKUP.setAccessible(true);
			LOOKUP = (Lookup) IMPL_LOOKUP.get(null);
		} catch (ReflectiveOperationException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	public static MethodHandle method(String className, String methodName, Class<?>... parameterTypes) {
		try {
			Method method = Class.forName(className).getDeclaredMethod(methodName, parameterTypes);
			method.setAccessible(true);
			return LOOKUP.unreflect(method);
		} catch (ClassNotFoundException | NoSuchMethodException e) {
			return null;
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	public static MethodHandle getter(String className, String fieldName) {
		try {
			Field field = Class.forName(className).getDeclaredField(fieldName);
			field.setAccessible(true);
			return LOOKUP.unreflectGetter(field);
		} catch (ClassNotFoundException | NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	public static MethodHandle setter(String className, String fieldName) {
		try {
			Field field = Class.forName(className).getDeclaredField(fieldName);
			field.setAccessible(true);
			return LOOKUP.unreflectSetter(field);
		} catch (ClassNotFoundException | NoSuchFieldException e) {
			return null;
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException(e);
		}
	}

	@SuppressWarnings("unchecked")
	public static <T> T get(String className, String fieldName, Object obj, T defaultValue) {
		try {
			Field f = Class.forName(className).getDeclaredField(fieldName);
			f.setAccessible(true);
			return (T) f.get(obj);
		} catch (ReflectiveOperationException e) {
			return defaultValue;
		}
	}

}
