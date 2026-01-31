package com.j256.simplejmx.client;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;

/**
 * Utility methods used whenever we are processing JMX client information.
 * 
 * @author graywatson
 */
public class ClientUtils {

	/**
	 * Convert a value string to an object based on the type string.
	 */
	public static Object valueToParam(String value, String typeString) throws IllegalArgumentException {
		if (value == null) {
			return null;
		}
		if (typeString.equals("boolean") || typeString.equals("java.lang.Boolean")) {
			return Boolean.parseBoolean(value);
		} else if (typeString.equals("char") || typeString.equals("java.lang.Character")) {
			if (value.length() == 0) {
				// not sure what to do here
				return '\0';
			} else {
				return value.toCharArray()[0];
			}
		} else if (typeString.equals("byte") || typeString.equals("java.lang.Byte")) {
			return Byte.parseByte(value);
		} else if (typeString.equals("short") || typeString.equals("java.lang.Short")) {
			return Short.parseShort(value);
		} else if (typeString.equals("int") || typeString.equals("java.lang.Integer")) {
			return Integer.parseInt(value);
		} else if (typeString.equals("long") || typeString.equals("java.lang.Long")) {
			return Long.parseLong(value);
		} else if (typeString.equals("java.lang.String")) {
			return value;
		} else if (typeString.equals("float") || typeString.equals("java.lang.Float")) {
			return Float.parseFloat(value);
		} else if (typeString.equals("double") || typeString.equals("java.lang.Double")) {
			return Double.parseDouble(value);
		} else {
			Constructor<?> constr = getConstructor(typeString);
			try {
				return constr.newInstance(new Object[] { value });
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

		StringBuilder sb = new StringBuilder();
		valueToString(sb, value);
		return sb.toString();
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
				className = "unknown";
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
				className = "unknown";
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

	private static void valueToString(StringBuilder sb, Object value) {
		if (value == null) {
			sb.append("null");
			return;
		} else if (!value.getClass().isArray()) {
			sb.append(value);
			return;
		}

		sb.append('[');
		int length = Array.getLength(value);
		for (int i = 0; i < length; i++) {
			if (i > 0) {
				sb.append(", ");
			}
			valueToString(sb, Array.get(value, i));
		}
		sb.append(']');
	}
}
