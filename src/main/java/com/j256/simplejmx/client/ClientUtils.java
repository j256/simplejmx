package com.j256.simplejmx.client;

import java.lang.reflect.Constructor;
import java.util.Arrays;

/**
 * Utility methods used whenever we are processing JMX client information.
 * 
 * @author graywatson
 */
public class ClientUtils {

	/**
	 * Convert a string to an object based on the type string.
	 */
	public static Object stringToParam(String string, String typeString) throws IllegalArgumentException {
		if (typeString.equals("boolean") || typeString.equals("java.lang.Boolean")) {
			return Boolean.parseBoolean(string);
		} else if (typeString.equals("char") || typeString.equals("java.lang.Character")) {
			if (string.length() == 0) {
				// not sure what to do here ffee
				return '\0';
			} else {
				return string.toCharArray()[0];
			}
		} else if (typeString.equals("byte") || typeString.equals("java.lang.Byte")) {
			return Byte.parseByte(string);
		} else if (typeString.equals("short") || typeString.equals("java.lang.Short")) {
			return Short.parseShort(string);
		} else if (typeString.equals("int") || typeString.equals("java.lang.Integer")) {
			return Integer.parseInt(string);
		} else if (typeString.equals("long") || typeString.equals("java.lang.Long")) {
			return Long.parseLong(string);
		} else if (typeString.equals("java.lang.String")) {
			return string;
		} else if (typeString.equals("float") || typeString.equals("java.lang.Float")) {
			return Float.parseFloat(string);
		} else if (typeString.equals("double") || typeString.equals("java.lang.Double")) {
			return Double.parseDouble(string);
		} else {
			Constructor<?> constr = getConstructor(typeString);
			try {
				return constr.newInstance(new Object[] { string });
			} catch (Exception e) {
				throw new IllegalArgumentException(
						"Could not get new instance using string constructor for type " + typeString);
			}
		}
	}

	/**
	 * Return the string version of value.
	 */
	public static String valueToString(Object value) {
		if (value == null) {
			return "null";
		} else if (!value.getClass().isArray()) {
			return value.toString();
		}

		if (value instanceof boolean[]) {
			return Arrays.toString((boolean[]) value);
		} else if (value instanceof byte[]) {
			return Arrays.toString((byte[]) value);
		} else if (value instanceof char[]) {
			return Arrays.toString((char[]) value);
		} else if (value instanceof short[]) {
			return Arrays.toString((short[]) value);
		} else if (value instanceof int[]) {
			return Arrays.toString((int[]) value);
		} else if (value instanceof long[]) {
			return Arrays.toString((long[]) value);
		} else if (value instanceof float[]) {
			return Arrays.toString((float[]) value);
		} else if (value instanceof double[]) {
			return Arrays.toString((double[]) value);
		} else {
			return Arrays.toString((Object[]) value);
		}
	}

	/**
	 * Display type string from class name string.
	 */
	public static String displayType(String className, Object value) {
		if (className == null) {
			return null;
		}
		boolean array = false;
		if (className.equals("[J")) {
			array = true;
			if (value == null) {
				className = "";
			} else if (value instanceof boolean[]) {
				className = "boolean";
			} else if (value instanceof byte[]) {
				className = "byte";
			} else if (value instanceof char[]) {
				className = "char";
			} else if (value instanceof short[]) {
				className = "short";
			} else if (value instanceof int[]) {
				className = "int";
			} else if (value instanceof long[]) {
				className = "long";
			} else if (value instanceof float[]) {
				className = "float";
			} else if (value instanceof double[]) {
				className = "double";
			} else {
				className = className.substring(2, className.length() - 1);
			}
		} else if (className.startsWith("[L")) {
			className = className.substring(2, className.length() - 1);
		}
		if (className.startsWith("java.lang.")) {
			className = className.substring(10);
		} else if (className.startsWith("javax.management.openmbean.")) {
			className = className.substring(27);
		}
		if (array) {
			return "array of " + className;
		} else {
			return className;
		}
	}

	private static <C> Constructor<C> getConstructor(String typeString) throws IllegalArgumentException {
		Class<Object> clazz;
		try {
			@SuppressWarnings("unchecked")
			Class<Object> clazzCast = (Class<Object>) Class.forName(typeString);
			clazz = clazzCast;
		} catch (ClassNotFoundException e) {
			throw new IllegalArgumentException("Unknown class for type " + typeString);
		}
		try {
			@SuppressWarnings("unchecked")
			Constructor<C> constructor = (Constructor<C>) clazz.getConstructor(new Class[] { String.class });
			return constructor;
		} catch (Exception e) {
			throw new IllegalArgumentException("Could not find constructor with single String argument for " + clazz);
		}
	}
}
