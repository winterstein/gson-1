package com.winterwell.utilslite;


import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

/**
 * Reflection-related utility functions
 * 
 * @testedby {@link ReflectionUtilsTest}
 */
public class ReflectionUtils {
	

	public static boolean isTransient(Field field) {
		int mods = field.getModifiers();
		return Modifier.isTransient(mods);
	}

	/**
	 * Ugly hack to handle different primitives. TODO are there better ways? Use
	 * v.getClass().getComponentType()? Array.newInstance()?
	 * 
	 * @param v
	 * @return shallow copy
	 */
	static Object copyArray(Object v) {
		int len = Array.getLength(v);
		if (v instanceof boolean[]) {
			return Arrays.copyOf((boolean[]) v, len);

		} else if (v instanceof byte[]) {
			return Arrays.copyOf((byte[]) v, len);

		} else if (v instanceof char[]) {
			return Arrays.copyOf((char[]) v, len);

		}
		if (v instanceof double[]) {
			return Arrays.copyOf((double[]) v, len);

		}
		if (v instanceof float[]) {
			return Arrays.copyOf((float[]) v, len);

		}
		if (v instanceof int[]) {
			return Arrays.copyOf((int[]) v, len);

		}
		if (v instanceof long[]) {
			return Arrays.copyOf((long[]) v, len);

		}
		if (v instanceof short[]) {
			return Arrays.copyOf((short[]) v, len);

		}
		return copyArray2(v, len);
	}

	static <T> T[] copyArray2(Object v, int len) {
		return Arrays.copyOf((T[]) v, len);
	}

	/**
	 * @param clazz
	 * @return Instance fields - public and private - which can be accessed from
	 *         this class. Excludes: static fields and fields which cannot be
	 *         accessed due to hardline JVM security.<br>
	 *         Includes: non-static final fields<br>
	 *         Field objects will have setAccessible(true) called on them as
	 *         needed to try & make private fields accessible.
	 */
	public static List<Field> getAllFields(Class clazz) {
		ArrayList<Field> list = new ArrayList<Field>();
		getAllFields2(clazz, list);
		return list;
	}

	private static void getAllFields2(Class clazz, ArrayList<Field> list) {
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			// exclude static
			int m = field.getModifiers();
			if (Modifier.isStatic(m)) {
				continue;
			}
			if (!field.isAccessible()) {
				try {
					field.setAccessible(true);
				} catch (SecurityException e) {
					// skip over this field
					continue;
				}
			}
			list.add(field);
		}
		// recurse
		Class superClass = clazz.getSuperclass();
		if (superClass == null)
			return;
		getAllFields2(superClass, list);
	}

	/**
	 * @param object
	 * @param annotation
	 * @param incPrivate
	 *            If true, will return private and protected fields (provided
	 *            they can be set accessible).
	 * @return (All fields / accessible public fields) in object which are
	 *         annotated with annotation
	 */
	public static List<Field> getAnnotatedFields(Object object,
			Class<? extends Annotation> annotation, boolean incPrivate) {
		List<Field> allFields = incPrivate ? getAllFields(object.getClass())
				: Arrays.asList(object.getClass().getFields());
		List<Field> fields = new ArrayList<Field>();
		for (Field f : allFields) {
			if (f.isAnnotationPresent(annotation)) {
				fields.add(f);
			}
		}
		return fields;
	}

	/**
	 * Recurse to get a private field which may be declared in a super-class.
	 * Note: {@link Class#getField(String)} will only retrieve public fields.
	 * 
	 * @param klass
	 * @param fieldName
	 * @return Field or null
	 */
	public static Field getField(Class klass, String fieldName) {
//		Utils.check4null(klass, fieldName);
		try {
			Field f = klass.getDeclaredField(fieldName);
			return f;
		} catch (NoSuchFieldException e) {
			klass = klass.getSuperclass();
			if (klass == null)
				return null;
		}
		return getField(klass, fieldName);
	}


	/**
	 * 
	 * @param clazz
	 * @param methodName
	 * @return The first method with matching name (ignores the parameters), or null if it isn't there. 
	 */
	static Method getMethod(Class<?> clazz, String methodName) {
//		clazz.getMethod(name, parameterTypes)
		// ignore the parameter types - but iterate over all methods :(
		for (Method m : clazz.getMethods()) {
			if (m.getName().equals(methodName))
				return m;
		}
		return null;
	}

	public static <X> X getPrivateField(Object obj, String fieldName) {
		Field f = getField(obj.getClass(), fieldName);
		f.setAccessible(true);
		try {
			return (X) f.get(obj);
		} catch (Exception e) {
			throw Utils.runtime(e);
		}
	}


	public static boolean hasField(Class klass, String field) {
		return getField(klass, field) != null;
	}

	/**
	 * @param klass
	 * @param methodName
	 * @return true if klass has a public method of that name
	 */
	public static boolean hasMethod(Class klass, String methodName) {
		for (Method m : klass.getMethods()) {
			if (m.getName().equals(methodName))
				return true;
		}
		return false;
	}

	/**
	 * The equivalent of instanceof, but for Class objects. 'cos I always forget
	 * how to do this.
	 * 
	 * @param possSubType Can be null (returns false)
	 * @param superType
	 * @return true if possSubType <i>is</i> a subType of superType
	 */
	public static boolean isa(Class possSubType, Class superType) {
		if (possSubType==null) return false;
		return superType.isAssignableFrom(possSubType);
	}


	/**
	 * TODO TEST!! This has changed
	 * 
	 * @return total available memory, in bytes. Does not run GC, so this is a fast
	 *         call.
	 * @see Runtime#freeMemory() -- which ignores the heap's capacity to grow, so is less
	 * useful! Runtime#freeMemory() can be thought of as "fast memory".
	 */
	public static long getAvailableMemory() {
		Runtime rt = Runtime.getRuntime();
		long maxMem = rt.maxMemory();
		long freeMem = rt.freeMemory();
		long totalMem = rt.totalMemory();
		long used = totalMem - freeMem;
		long available = maxMem - used;
		return available;
	}

	/**
	 * @return a String showing the current stack
	 */
	public static String stacktrace() {
		try {
			throw new Exception();
		} catch (Exception e) {
			StackTraceElement[] trace = e.getStackTrace();
			StringBuilder sb = new StringBuilder();
			for (int i = 1; i < trace.length; i++) {
				StackTraceElement stackTraceElement = trace[i];
				sb.append(stackTraceElement.toString());
				sb.append('\n');
			}
			return sb.toString();
		}
	}

	/**
	 * Who called this method?
	 * 
	 * NB: This always skips the actual calling method, stacktrace #-0
	 * 
	 * @param ignore
	 *            list of fully-qualified-class or method names to ignore (will
	 *            then search higher up the stack)
	 * @return Can be a dummy entry if the filters exclude everything. Never
	 *         null.
	 * @see #getSomeStack(int, String...)
	 */
	public static StackTraceElement getCaller(String... ignore) {
		return getCaller(1, ignore);
	}


	/**
	 * Who called this method?
	 * 
	 * @param ignore
	 *            list of fully-qualified-class or method names to ignore (will
	 *            then search higher up the stack)
	 * @param up 0 = get the method directly above the one calling this.           
	 * @return Can be a dummy entry if the filters exclude everything. Never
	 *         null.
	 * @see #getSomeStack(int, String...)
	 */
	public static StackTraceElement getCaller(int up, String... ignore) {
		List<String> ignoreNames = Arrays.asList(ignore);
		try {
			throw new Exception();
		} catch (Exception e) {
			StackTraceElement[] trace = e.getStackTrace();
			for (int i = 2+up; i < trace.length; i++) {
				String clazz = trace[i].getClassName();
				String method = trace[i].getMethodName();
				if (ignoreNames.contains(clazz) || ignoreNames.contains(method)) {
					continue;
				}
				return trace[i]; // new Pair<String>(clazz, method);
			}
			return new StackTraceElement("filtered", "?", null, -1);
		}
	}

	/**
	 * Who called this method? Returns the lowest parts of the stack.
	 * 
	 * @param depth
	 *            How many elements to aim for. Can be set very high for all-of-them.
	 * @param ignore
	 *            list of fully-qualified-class or method names to ignore (will
	 *            then search higher up the stack)
	 * @return Can be empty if the filters exclude everything. Never null.
	 * @see #getCaller(String...)
	 */
	public static List<StackTraceElement> getSomeStack(int depth,
			String... ignore) {
		assert depth > 0 : depth;
		List<String> ignoreNames = Arrays.asList(ignore);
		try {
			throw new Exception();
		} catch (Exception e) {
			StackTraceElement[] trace = e.getStackTrace();
			List<StackTraceElement> stack = new ArrayList(depth);
			for (int i = 2; i < trace.length; i++) {
				String clazz = trace[i].getClassName();
				String method = trace[i].getMethodName();
				if (ignoreNames.contains(clazz) || ignoreNames.contains(method)) {
					continue;
				}
				stack.add(trace[i]);
				if (stack.size() == depth)
					break;
			}
			return stack;
		}
	}

	/**
	 * Like class.getSimpleName() -- but if given an anonymous class, it will
	 * return the super-classes' name (rather than null)
	 * 
	 * @param class1
	 * @return name Never null or empty
	 */
	public static String getSimpleName(Class class1) {
		String name = class1.getSimpleName();
		if (!name.isEmpty()) {
			return name;
		}
		return getSimpleName(class1.getSuperclass());
	}

	public static boolean isaNumber(Class<?> type) {
		return isa(type, Number.class) || type == int.class
				|| type == double.class || type == long.class
				|| type == float.class;
	}

	/**
	 * @return e.g. 1.6
	 */
	public static double getJavaVersion() {
		String version = System.getProperty("java.version");
		if (version == null) {
			// WTF?!
			return 1.5;
		}
		int pos = 0, count = 0;
		for (; pos < version.length() && count < 2; pos++) {
			if (version.charAt(pos) == '.')
				count++;
		}
		pos--;
		return Double.parseDouble(version.substring(0, pos));
	}



}