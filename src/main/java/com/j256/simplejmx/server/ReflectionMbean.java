package com.j256.simplejmx.server;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.IntrospectionException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;
import javax.management.ReflectionException;

import com.j256.simplejmx.common.JmxAttributeField;
import com.j256.simplejmx.common.JmxAttributeMethod;
import com.j256.simplejmx.common.JmxOperation;
import com.j256.simplejmx.common.JmxResource;
import com.j256.simplejmx.common.JmxSelfNaming;

/**
 * This wraps an object that has been registered in the server using {@link JmxServer#register(Object)}. We wrap the
 * object so we can expose its attributes and operations using annotations and reflection. This handles the JMX server
 * calls to attributes and operations by calling through the delegation object.
 * 
 * @author graywatson
 */
public class ReflectionMbean implements DynamicMBean {

	private final static Method ATTRIBUTE_FIELD_GET_METHOD;
	private final static Method ATTRIBUTE_FIELD_SET_METHOD;

	private final Object delegate;
	private final Map<String, AttributeMethodInfo> attributeMethodMap = new HashMap<String, AttributeMethodInfo>();
	private final Map<NameParams, OperationMethodInfo> operationMethodMap =
			new HashMap<NameParams, OperationMethodInfo>();
	private final Map<String, AttributeFieldInfo> attributeFieldMap = new HashMap<String, AttributeFieldInfo>();
	private final MBeanInfo mbeanInfo;

	static {
		try {
			ATTRIBUTE_FIELD_GET_METHOD = AttributeFieldInfo.class.getMethod("getMethod");
		} catch (Exception e) {
			throw new RuntimeException("Could not find get method in " + AttributeFieldInfo.class);
		}
		try {
			ATTRIBUTE_FIELD_SET_METHOD = AttributeFieldInfo.class.getMethod("setMethod", Object.class);
		} catch (Exception e) {
			throw new RuntimeException("Could not find set method in " + AttributeFieldInfo.class);
		}
	}

	/**
	 * Create a mbean associated with a delegate object that implements self-naming.
	 */
	public ReflectionMbean(JmxSelfNaming delegate) {
		this.delegate = delegate;
		this.mbeanInfo = buildMbeanInfo();
	}

	/**
	 * Create a mbean associated with a delegate object that must have a {@link JmxResource} annotation.
	 */
	public ReflectionMbean(Object delegate) {
		this.delegate = delegate;
		this.mbeanInfo = buildMbeanInfo();
	}

	/**
	 * @see {@link DynamicMBean#getMBeanInfo()}.
	 */
	public MBeanInfo getMBeanInfo() {
		return mbeanInfo;
	}

	/**
	 * @see {@link DynamicMBean#getAttribute(String)}.
	 */
	public Object getAttribute(String attributeName) throws AttributeNotFoundException, ReflectionException {
		AttributeMethodInfo methodInfo = attributeMethodMap.get(attributeName);
		if (methodInfo == null) {
			AttributeFieldInfo fieldInfo = attributeFieldMap.get(attributeName);
			if (fieldInfo == null || !fieldInfo.isGetter) {
				throw new AttributeNotFoundException("Unknown attribute " + attributeName);
			}
			try {
				return fieldInfo.field.get(delegate);
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking getter attribute on field " + fieldInfo.field.getName()
						+ " on " + delegate.getClass() + " threw exception");
			}
		} else {
			if (methodInfo.getterMethod == null) {
				throw new AttributeNotFoundException("Unknown attribute " + attributeName);
			}
			try {
				return methodInfo.getterMethod.invoke(delegate);
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking getter attribute method "
						+ methodInfo.getterMethod.getName() + " on " + delegate.getClass() + " threw exception");
			}
		}
	}

	/**
	 * @see {@link DynamicMBean#getAttributes(String[])}.
	 */
	public AttributeList getAttributes(String[] attributeNames) {
		AttributeList returnList = new AttributeList();
		for (String name : attributeNames) {
			try {
				returnList.add(new Attribute(name, getAttribute(name)));
			} catch (Exception e) {
				returnList.add(new Attribute(name, "Getting attribute threw: " + e.getMessage()));
			}
		}
		return returnList;
	}

	/**
	 * @see {@link DynamicMBean#setAttribute(Attribute)}.
	 */
	public void setAttribute(Attribute attribute) throws AttributeNotFoundException, ReflectionException {
		AttributeMethodInfo methodInfo = attributeMethodMap.get(attribute.getName());
		if (methodInfo == null) {
			AttributeFieldInfo fieldInfo = attributeFieldMap.get(attribute.getName());
			if (fieldInfo == null || !fieldInfo.isSetter) {
				throw new AttributeNotFoundException("Unknown attribute " + attribute);
			}
			try {
				fieldInfo.field.set(delegate, attribute.getValue());
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking setter attribute on field " + fieldInfo.field.getName()
						+ " on " + delegate.getClass() + " threw exception");
			}
		} else {
			if (methodInfo.setterMethod == null) {
				throw new AttributeNotFoundException("Unknown attribute " + attribute);
			}
			try {
				methodInfo.setterMethod.invoke(delegate, attribute.getValue());
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking setter attribute method "
						+ methodInfo.setterMethod.getName() + " on " + delegate.getClass() + " threw exception");
			}
		}
	}

	/**
	 * @see {@link DynamicMBean#setAttributes(AttributeList)}.
	 */
	public AttributeList setAttributes(AttributeList attributes) {
		AttributeList returnList = new AttributeList();
		for (Attribute attribute : attributes.asList()) {
			String name = attribute.getName();
			try {
				setAttribute(attribute);
				returnList.add(new Attribute(name, getAttribute(name)));
			} catch (Exception e) {
				returnList.add(new Attribute(name, e.getMessage()));
			}
		}
		return returnList;
	}

	/**
	 * @see {@link DynamicMBean#invoke(String, Object[], String[])}.
	 */
	public Object invoke(String actionName, Object[] params, String[] signatureTypes) throws MBeanException,
			ReflectionException {
		OperationMethodInfo methodInfo = operationMethodMap.get(new NameParams(actionName, signatureTypes));
		if (methodInfo == null) {
			throw new MBeanException(new IllegalArgumentException("Unknown action '" + actionName
					+ "' with parameter types " + Arrays.toString(signatureTypes)));
		} else {
			try {
				return methodInfo.method.invoke(delegate, params);
			} catch (Exception e) {
				throw new ReflectionException(e, "Invoking operation method " + methodInfo.method.getName() + " on "
						+ delegate.getClass() + " threw exception");
			}
		}
	}

	/**
	 * Build our JMX information object by using reflection.
	 */
	private MBeanInfo buildMbeanInfo() {
		Class<?> clazz = delegate.getClass();
		JmxResource jmxResource = clazz.getAnnotation(JmxResource.class);
		String desc;
		if (jmxResource == null || jmxResource.description() == null || jmxResource.description().length() == 0) {
			desc = "Information about " + clazz;
		} else {
			desc = jmxResource.description();
		}
		Method[] methods = clazz.getMethods();
		discoverAttributeMethods(methods);
		discoverOperations(methods);
		discoverAttributeFields();

		List<MBeanAttributeInfo> attributes = new ArrayList<MBeanAttributeInfo>(attributeMethodMap.size());
		for (AttributeMethodInfo methodInfo : attributeMethodMap.values()) {
			try {
				attributes.add(new MBeanAttributeInfo(methodInfo.varName, methodInfo.description,
						methodInfo.getterMethod, methodInfo.setterMethod));
			} catch (IntrospectionException e) {
				// ignore this attribute if it throws I guess
			}
		}
		for (AttributeFieldInfo fieldInfo : attributeFieldMap.values()) {
			try {
				attributes.add(new MBeanAttributeInfo(fieldInfo.field.getName(), fieldInfo.description,
						fieldInfo.getGetterMethod(), fieldInfo.getSetterMethod()));
			} catch (IntrospectionException e) {
				// ignore this attribute if it throws I guess
			}
		}
		List<MBeanOperationInfo> operations = new ArrayList<MBeanOperationInfo>(operationMethodMap.size());
		// we have to go back because we need to match up the getters and setters
		for (OperationMethodInfo methodInfo : operationMethodMap.values()) {
			operations.add(new MBeanOperationInfo(methodInfo.methodName, methodInfo.description,
					methodInfo.parameterInfos, methodInfo.method.getReturnType().getName(), methodInfo.action));
		}

		return new MBeanInfo(clazz.getName(), desc, attributes.toArray(new MBeanAttributeInfo[attributes.size()]),
				null, operations.toArray(new MBeanOperationInfo[operations.size()]), null);
	}

	/**
	 * Using reflection, find attribute methods from our object that will be exposed via JMX.
	 */
	private void discoverAttributeMethods(Method[] methods) {
		for (Method method : methods) {
			JmxAttributeMethod jmxAttribute = method.getAnnotation(JmxAttributeMethod.class);
			if (jmxAttribute == null) {
				// skip it if no annotation
				continue;
			}
			String name = method.getName();
			if (name.length() < 4) {
				throw new IllegalArgumentException("Method '" + method + "' has a name that is too short");
			}
			String varName = buildMethodSuffix(name);
			AttributeMethodInfo methodInfo = attributeMethodMap.get(varName);
			if (name.startsWith("get")) {
				if (method.getParameterTypes().length != 0) {
					throw new IllegalArgumentException("Method '" + method + "' starts with 'get' but has arguments");
				}
				if (method.getReturnType() == void.class) {
					throw new IllegalArgumentException("Method '" + method
							+ "' starts with 'get' but does not return anything");
				}
				if (methodInfo == null) {
					attributeMethodMap.put(varName, new AttributeMethodInfo(varName, jmxAttribute.description(),
							method, null));
				} else {
					methodInfo.getterMethod = method;
				}
			} else if (name.startsWith("set")) {
				if (method.getParameterTypes().length != 1) {
					throw new IllegalArgumentException("Method '" + method
							+ "' starts with 'set' but does not have 1 argument");
				}
				if (method.getReturnType() != void.class) {
					throw new IllegalArgumentException("Method '" + method
							+ "' starts with 'set' but does not return void");
				}
				if (methodInfo == null) {
					attributeMethodMap.put(varName, new AttributeMethodInfo(varName, jmxAttribute.description(), null,
							method));
				} else {
					methodInfo.setterMethod = method;
				}
			} else {
				throw new IllegalArgumentException("Method '" + method
						+ "' is marked as an attribute but does not start with 'get' or 'set'");
			}
		}
	}

	/**
	 * Using reflection, find attribute methods from our object that will be exposed via JMX.
	 */
	private void discoverAttributeFields() {
		Class<?> clazz = delegate.getClass();
		Field[] fields = clazz.getDeclaredFields();
		for (Field field : fields) {
			JmxAttributeField attributeField = field.getAnnotation(JmxAttributeField.class);
			if (attributeField == null) {
				continue;
			}

			if (!field.isAccessible()) {
				field.setAccessible(true);
			}
			attributeFieldMap.put(field.getName(), new AttributeFieldInfo(field, attributeField.description(),
					attributeField.isReadible(), attributeField.isWritable()));
		}
	}

	/**
	 * Using reflection, find operation methods from our object that will be exposed via JMX.
	 */
	private void discoverOperations(Method[] methods) {
		for (Method method : methods) {
			JmxOperation jmxOperation = method.getAnnotation(JmxOperation.class);
			if (jmxOperation == null) {
				continue;
			}
			String name = method.getName();
			if (name.startsWith("get") || name.startsWith("set")) {
				throw new IllegalArgumentException("Operation method " + method
						+ " cannot start with get or set.  Is this an attribute?");
			}
			Class<?>[] types = method.getParameterTypes();
			String[] stringTypes = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				stringTypes[i] = types[i].getName();
			}
			NameParams nameParams = new NameParams(name, stringTypes);
			MBeanParameterInfo[] parameterInfos = buildOperationParameterInfo(method, jmxOperation);
			operationMethodMap.put(nameParams, new OperationMethodInfo(name, jmxOperation.description(), method,
					parameterInfos, jmxOperation.action()));
		}
	}

	/**
	 * Build our parameter information for an operation.
	 */
	private MBeanParameterInfo[] buildOperationParameterInfo(Method method, JmxOperation jmxOperation) {
		Class<?>[] types = method.getParameterTypes();
		MBeanParameterInfo[] parameterInfos = new MBeanParameterInfo[types.length];
		String[] parameterNames = jmxOperation.parameterNames();
		String[] parameterDescriptions = jmxOperation.parameterDescriptions();
		for (int i = 0; i < types.length; i++) {
			String parameterName;
			if (i >= parameterNames.length) {
				parameterName = "p" + (i + 1);
			} else {
				parameterName = parameterNames[i];
			}
			String typeName = types[i].getName();
			String description;
			if (i >= parameterDescriptions.length) {
				description = "parameter #" + (i + 1) + " of type: " + typeName;
			} else {
				description = parameterDescriptions[i];
			}
			parameterInfos[i] = new MBeanParameterInfo(parameterName, typeName, description);
		}
		return parameterInfos;
	}

	private String buildMethodSuffix(String name) {
		return Character.toLowerCase(name.charAt(3)) + name.substring(4);
	}

	/**
	 * Key class for our hashmap to find matching methods based on name and parameter list.
	 */
	private static class NameParams {
		String name;
		String[] paramTypes;
		public NameParams(String name, String[] paramTypes) {
			this.name = name;
			this.paramTypes = paramTypes;
		}

		@Override
		public int hashCode() {
			int hashCode = 31 * (31 + name.hashCode());
			if (paramTypes != null) {
				hashCode += Arrays.hashCode(paramTypes);
			}
			return hashCode;
		}

		@Override
		public boolean equals(Object obj) {
			if (obj == null || getClass() != obj.getClass()) {
				return false;
			}
			NameParams other = (NameParams) obj;
			if (!this.name.equals(other.name)) {
				return false;
			}
			return Arrays.equals(this.paramTypes, other.paramTypes);
		}
	}

	private static class AttributeMethodInfo {
		final String varName;
		final String description;
		Method getterMethod;
		Method setterMethod;
		public AttributeMethodInfo(String varName, String description, Method getterMethod, Method setterMethod) {
			this.varName = varName;
			if (description == null || description.length() == 0) {
				this.description = varName + " attribute";
			} else {
				this.description = description;
			}
			this.getterMethod = getterMethod;
			this.setterMethod = setterMethod;
		}
	}

	private static class OperationMethodInfo {
		final String methodName;
		final String description;
		final Method method;
		final MBeanParameterInfo[] parameterInfos;
		final int action;
		public OperationMethodInfo(String methodName, String description, Method method,
				MBeanParameterInfo[] parameterInfos, int action) {
			this.methodName = methodName;
			if (description == null || description.length() == 0) {
				this.description = methodName + " attribute";
			} else {
				this.description = description;
			}
			this.method = method;
			this.parameterInfos = parameterInfos;
			this.action = action;
		}
	}

	private static class AttributeFieldInfo {

		final Field field;
		final String description;
		final boolean isGetter;
		final boolean isSetter;

		public AttributeFieldInfo(Field field, String description, boolean isGetter, boolean isSetter) {
			this.field = field;
			this.description = description;
			this.isGetter = isGetter;
			this.isSetter = isSetter;
		}

		public Method getGetterMethod() {
			if (isGetter) {
				return ATTRIBUTE_FIELD_GET_METHOD;
			} else {
				return null;
			}
		}

		public Method getSetterMethod() {
			if (isSetter) {
				return ATTRIBUTE_FIELD_SET_METHOD;
			} else {
				return null;
			}
		}

		/**
		 * These are used (in gross fashion) to be able to generate a fake getter Method.
		 */
		@SuppressWarnings("unused")
		public Object getMethod() {
			// noop
			return null;
		}

		/**
		 * These are used (in gross fashion) to be able to generate a fake setter Method.
		 */
		@SuppressWarnings("unused")
		public void setMethod(Object obj) {
			// noop
		}
	}
}
